package com.example.donor.data.model

import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@IgnoreExtraProperties
data class Donor(
    val uid: String              = "",
    val fullName: String         = "",
    val phoneNumber: String      = "",        // NEW: for phone auth
    val dateOfBirth: String      = "",        // NEW: DD/MM/YYYY
    val bloodGroup: String       = "",
    val weight: String           = "",        // NEW: <50, 50-60, 60-70, 70+
    val mobile: String           = "",
    val district: String         = "",
    val city: String             = "",
    val regDate: String          = "",
    val lastDonation: String     = "",
    val nextEligible: String     = "",
    val available: Boolean       = true,
    val fcmToken: String         = ""

) {
    /**
     * Unified logic for availability:
     * A donor is available if they are manually marked as available AND
     * (they've never donated OR their next-eligible date has passed/is today).
     */
    fun isCurrentlyAvailable(): Boolean {
        // 1. Check manual toggle
        if (!available) return false

        // 2. Check donation history/recovery
        if (lastDonation.isBlank() || lastDonation == "Never" || nextEligible.isBlank()) {
            return true
        }

        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val eligibleDate = sdf.parse(nextEligible) ?: return true

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // Available if today >= eligibleDate
            !eligibleDate.after(today)
        } catch (_: Exception) {
            // Default to available if date parsing fails for some reason
            true
        }
    }
}