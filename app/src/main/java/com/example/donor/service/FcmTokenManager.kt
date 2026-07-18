package com.example.donor.service

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Manages the device's FCM token.
 *
 * Call saveToken(uid) once after a donor successfully logs in.
 * The token is written to /donors/{uid}/fcmToken in Firebase so that
 * FcmNotificationSender can find it when sending notifications.
 *
 * FirebaseMessaging handles token refresh automatically — but if you want
 * to keep the token fresh, call saveToken() again from onNewToken() in
 * your FirebaseMessagingService subclass (see MyFirebaseMessagingService.kt).
 */
object FcmTokenManager {

    private const val TAG = "FcmTokenManager"
    private val db = FirebaseDatabase.getInstance().reference

    /**
     * Gets the current FCM token for this device and writes it to
     * /donors/{uid}/fcmToken in Firebase Realtime Database.
     *
     * This is a suspend function — call it from a coroutine scope,
     * e.g. viewModelScope.launch { FcmTokenManager.saveToken(uid) }
     */
    suspend fun saveToken(uid: String) {
        if (uid.isBlank()) {
            Log.w(TAG, "saveToken called with blank uid — skipping")
            return
        }

        try {
            val token = FirebaseMessaging.getInstance().token.await()

            if (token.isBlank()) {
                Log.w(TAG, "FCM token is blank — skipping save")
                return
            }

            db.child("donors").child(uid).child("fcmToken")
                .setValue(token)
                .await()

            Log.d(TAG, "FCM token saved for donor $uid")

        } catch (e: Exception) {
            // Non-fatal — donor can still use the app, they just won't
            // receive push notifications until the token is saved.
            Log.e(TAG, "Failed to save FCM token: ${e.message}")
        }
    }

    /**
     * Gets the current FCM token without saving it anywhere.
     * Used by RequestBloodViewModel to include the token in a blood request
     * so the requester can be notified when a donor acknowledges.
     *
     * Returns blank string on failure.
     */
    suspend fun getToken(): String {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get FCM token: ${e.message}")
            ""
        }
    }
}