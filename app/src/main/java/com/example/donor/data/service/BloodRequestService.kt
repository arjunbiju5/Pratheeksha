package com.example.donor.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.donor.MainActivity
import com.example.donor.data.model.BloodRequest
import com.example.donor.data.session.SessionManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * SECONDARY / BEST-EFFORT ONLY.
 *
 * This service fires a local notification when a matching blood request arrives
 * while the donor already has the app running in the background. It is NOT the
 * primary delivery mechanism — OneSignal handles that. Never add logic that
 * assumes this covers cases OneSignal might miss.
 *
 * Call startListeningAsDonor() after donor login.
 * Call startListeningAsAdmin() after admin login.
 * Call stopListening()         on logout.
 */
class BloodRequestService(private val context: Context) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(context)
    private var listener: ChildEventListener? = null

    companion object {
        const val CHANNEL_ID       = "blood_requests"
        const val CHANNEL_NAME     = "Blood Requests"
        const val EXTRA_REQUEST_ID = "requestId"
        const val EXTRA_OPEN       = "openScreen"
    }

    // ── Donor listener ────────────────────────────────────────────────────────

    fun startListeningAsDonor() {
        stopListening()
        createChannel()

        listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (!snapshot.exists()) return
                val request = try {
                    snapshot.getValue(BloodRequest::class.java)
                } catch (e: Exception) {
                    null
                } ?: return

                if (request.status != "pending") return

                CoroutineScope(Dispatchers.IO).launch {
                    val donorDistrict   = sessionManager.donorDistrict.first()
                    val donorBloodGroup = sessionManager.bloodGroup.first()
                    val isAvailable     = sessionManager.isAvailable.first()
                    val isEligible      = sessionManager.isEligible.first()

                    // Trim + lowercase — same fix applied to FindDonorViewModel.
                    // Raw == was silently dropping matches on whitespace/case mismatches.
                    val districtMatch   = request.district.trim().lowercase() ==
                            donorDistrict.trim().lowercase()
                    val bloodGroupMatch = request.bloodGroup.trim().lowercase() ==
                            donorBloodGroup.trim().lowercase()
                    val donorCanDonate  = isAvailable && isEligible

                    if (districtMatch && bloodGroupMatch && donorCanDonate) {
                        showNotification(request, isAdmin = false)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        db.child("bloodRequests").addChildEventListener(listener!!)
    }

    // ── Admin listener ────────────────────────────────────────────────────────

    fun startListeningAsAdmin() {
        stopListening()
        createChannel()

        listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val request = snapshot.getValue(BloodRequest::class.java) ?: return
                if (request.status == "pending") showNotification(request, isAdmin = true)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        db.child("bloodRequests").addChildEventListener(listener!!)
    }

    fun stopListening() {
        listener?.let { db.child("bloodRequests").removeEventListener(it) }
        listener = null
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun showNotification(request: BloodRequest, isAdmin: Boolean) {
        val isUrgent = request.urgency == "urgent"

        val title = when {
            isUrgent -> "🚨 URGENT: ${request.bloodGroup} Blood Needed"
            else     -> "🩸 Blood Request: ${request.bloodGroup}"
        }
        val body = "${request.requesterName} needs ${request.bloodGroup} at " +
                "${request.hospital}, ${request.city}. Contact: ${request.mobile}"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, request.id)
            putExtra(EXTRA_OPEN, "blood_request_detail")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            request.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.donor.R.drawable.ic_bloodlink_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(request.id.hashCode(), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nearby blood donation requests"
                enableVibration(true)
                enableLights(true)
            }
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }
}