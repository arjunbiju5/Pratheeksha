package com.example.donor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.donor.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.donor.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "blood_requests"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated")
        CoroutineScope(Dispatchers.IO).launch {
            val uid = SessionManager(applicationContext).donorUid.first()
            if (uid.isNotBlank()) {
                FcmTokenManager.saveToken(uid)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "Blood Request"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "Someone needs blood nearby"
        val reqId = message.data["requestId"]
        val type  = message.data["type"] ?: ""

        showNotification(title, body, reqId, type)
    }

    private fun showNotification(
        title:     String,
        body:      String,
        requestId: String?,
        type:      String
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            when (type) {
                "donor_acknowledged" -> {
                    // Requester taps → go to their specific request detail
                    if (requestId != null) {
                        putExtra("requestId",  requestId)
                        putExtra("openScreen", "blood_request_detail")
                    }
                }
                // Donor/admin taps new request notification → pending requests list
                "new_request" -> {
                    // Donor taps → open specific request detail
                    // (pending requests will be in back stack via SplashScreen)
                    if (requestId != null) {
                        putExtra("requestId",  requestId)
                        putExtra("openScreen", "blood_request_detail")
                        putExtra("fromNotification", true)  // signals to add pending_requests to back stack
                    }
                }
                "new_request_admin" -> {
                    putExtra("openScreen", "pending_requests")
                }
            }
        }

        // CHANGE: use requestId hashcode as unique requestCode so each notification gets its own PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestId?.hashCode() ?: System.currentTimeMillis().toInt(), // ← was hardcoded 0
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.example.donor.R.drawable.ic_bloodlink_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Blood Requests", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}