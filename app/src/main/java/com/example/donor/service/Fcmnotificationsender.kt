package com.example.donor.service

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends FCM push notifications directly via the FCM HTTP v1 API.
 *
 * Uses a short-lived OAuth token generated at runtime from service_account.json
 * stored in assets/. The token expires every 60 minutes and is auto-refreshed
 * by the Google Auth Library — no hardcoded API keys anywhere.
 *
 * Two entry points:
 *   sendToOne()      — notify a single device (e.g. requester when donor acknowledges)
 *   sendToMultiple() — notify multiple devices (e.g. matching donors on new request)
 */
object FcmNotificationSender {

    private const val TAG = "FcmNotificationSender"

    // Replace with your actual Firebase project ID (the one in google-services.json)
    private const val PROJECT_ID = "donor-cf100"

    private const val FCM_URL =
        "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    private const val FCM_SCOPE =
        "https://www.googleapis.com/auth/firebase.messaging"

    // ── OAuth token ───────────────────────────────────────────────────────────

    /**
     * Reads service_account.json from assets and returns a short-lived
     * OAuth access token scoped to Firebase Messaging.
     * The Google Auth Library caches and refreshes this automatically.
     */
    private fun getAccessToken(context: Context): String {
        val stream = context.assets.open("service_account.json")
        val credentials = GoogleCredentials
            .fromStream(stream)
            .createScoped(listOf(FCM_SCOPE))
        credentials.refreshIfExpired()
        return credentials.accessToken.tokenValue
    }

    // ── Send to one device ────────────────────────────────────────────────────

    /**
     * Sends a notification to a single FCM token.
     * Used to notify the requester when a donor acknowledges their request.
     *
     * @param context      Android context (needed to read assets)
     * @param token        Target device's FCM token
     * @param title        Notification title
     * @param body         Notification body
     * @param data         Optional key-value data payload (e.g. requestId, screen)
     */
    suspend fun sendToOne(
        context: Context,
        token:   String,
        title:   String,
        body:    String,
        data:    Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (token.isBlank()) {
            Log.w(TAG, "sendToOne called with blank token — skipping")
            return@withContext
        }

        val payload = buildPayload(
            target  = JSONObject().put("token", token),
            title   = title,
            body    = body,
            data    = data
        )

        sendRequest(context, payload)
    }

    // ── Send to multiple devices ──────────────────────────────────────────────

    /**
     * Sends a notification to a list of FCM tokens, one at a time.
     * FCM HTTP v1 does not support multicast — each token gets its own request.
     * For BloodLink's scale (tens of donors per district) this is fine.
     *
     * Used to notify matching donors when a new blood request is submitted.
     *
     * @param context  Android context
     * @param tokens   List of target FCM tokens
     * @param title    Notification title
     * @param body     Notification body
     * @param data     Optional key-value data payload
     */
    suspend fun sendToMultiple(
        context: Context,
        tokens:  List<String>,
        title:   String,
        body:    String,
        data:    Map<String, String> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        if (tokens.isEmpty()) {
            Log.w(TAG, "sendToMultiple called with empty token list — skipping")
            return@withContext
        }

        val accessToken = try {
            getAccessToken(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get access token: ${e.message}")
            return@withContext
        }

        // ── Parallel Execution ──
        val jobs = tokens.filter { it.isNotBlank() }.map { token ->
            launch {
                val payload = buildPayload(
                    target = JSONObject().put("token", token),
                    title  = title,
                    body   = body,
                    data   = data
                )
                sendRequest(context, payload, accessToken)
            }
        }
        jobs.joinAll()

        Log.d(TAG, "sendToMultiple: Dispatched ${jobs.size} notifications in parallel")
    }

    // ── Build JSON payload ────────────────────────────────────────────────────

    private fun buildPayload(
        target: JSONObject,
        title:  String,
        body:   String,
        data:   Map<String, String>
    ): JSONObject {
        val notification = JSONObject()
            .put("title", title)
            .put("body",  body)

        val androidNotification = JSONObject()
            .put("channel_id",              "blood_requests")
            .put("default_vibrate_timings", true)
            .put("default_sound",           true)

        val android = JSONObject()
            .put("priority",     "HIGH")
            .put("notification", androidNotification)

        val message = JSONObject()
            .put("notification", notification)
            .put("android",      android)

        // Merge target (token / topic) into message
        target.keys().forEach { key ->
            message.put(key, target.get(key))
        }

        // Add data payload if provided
        if (data.isNotEmpty()) {
            val dataJson = JSONObject()
            data.forEach { (k, v) -> dataJson.put(k, v) }
            message.put("data", dataJson)
        }

        return JSONObject().put("message", message)
    }

    // ── HTTP request ──────────────────────────────────────────────────────────

    /**
     * Makes the HTTP POST to the FCM v1 API.
     * Returns true on success (HTTP 200), false otherwise.
     *
     * Accepts an optional pre-fetched accessToken to avoid re-reading
     * assets when sending to multiple tokens in a loop.
     */
    private fun sendRequest(
        context:     Context,
        payload:     JSONObject,
        accessToken: String? = null
    ): Boolean {
        return try {
            val token = accessToken ?: getAccessToken(context)

            val url        = URL(FCM_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type",  "application/json")
                doOutput = true
                connectTimeout = 10_000
                readTimeout    = 10_000
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.d(TAG, "FCM message sent successfully")
                true
            } else {
                val error = connection.errorStream?.bufferedReader()?.readText() ?: "unknown"
                Log.e(TAG, "FCM send failed [$responseCode]: $error")
                false
            }

        } catch (e: Exception) {
            Log.e(TAG, "FCM request exception: ${e.message}")
            false
        }
    }
}