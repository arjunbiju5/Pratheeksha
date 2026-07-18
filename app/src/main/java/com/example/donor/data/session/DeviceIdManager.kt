package com.example.donor.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.deviceIdStore: DataStore<Preferences>
        by preferencesDataStore(name = "device_identity")

private val DEVICE_ID_KEY = stringPreferencesKey("device_id")

/**
 * Generates and persists a random UUID the first time it's needed.
 * This UUID is independent of donor login — it survives app restarts
 * on the same device install, so anonymous requesters (bystanders, family
 * members who never log in) can still see "their" requests later.
 *
 * Note: this resets on app reinstall/uninstall, since DataStore data is
 * cleared with app data. That's an acceptable tradeoff for this use case.
 */
object DeviceIdManager {

    suspend fun getOrCreateDeviceId(context: Context): String {
        val existing = context.deviceIdStore.data.first()[DEVICE_ID_KEY]
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        context.deviceIdStore.edit { it[DEVICE_ID_KEY] = newId }
        return newId
    }
}

private val Context.dataStore: DataStore<Preferences>
        by preferencesDataStore(name = "donor_session")

object SessionKeys {
    val IS_LOGGED_IN   = booleanPreferencesKey("is_logged_in")
    val DONOR_PHONE    = stringPreferencesKey("donor_phone")
    val DONOR_NAME     = stringPreferencesKey("donor_name")
    val DONOR_UID      = stringPreferencesKey("donor_uid")
    val BLOOD_GROUP    = stringPreferencesKey("blood_group")
    val LAST_DONATION  = stringPreferencesKey("last_donation")
    val NEXT_ELIGIBLE  = stringPreferencesKey("next_eligible")
    val DONOR_DISTRICT = stringPreferencesKey("donor_district")
    val IS_AVAILABLE   = booleanPreferencesKey("is_available")
    val IS_ADMIN       = booleanPreferencesKey("is_admin")
}

class SessionManager(private val context: Context) {

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[SessionKeys.IS_LOGGED_IN]   ?: false }
    val donorPhone: Flow<String> = context.dataStore.data.map { it[SessionKeys.DONOR_PHONE]    ?: "" }
    val donorName: Flow<String> = context.dataStore.data.map { it[SessionKeys.DONOR_NAME]     ?: "" }
    val donorUid: Flow<String> = context.dataStore.data.map { it[SessionKeys.DONOR_UID]      ?: "" }
    val bloodGroup: Flow<String> = context.dataStore.data.map { it[SessionKeys.BLOOD_GROUP]    ?: "" }
    val lastDonation: Flow<String> = context.dataStore.data.map { it[SessionKeys.LAST_DONATION]  ?: "" }
    val nextEligible: Flow<String> = context.dataStore.data.map { it[SessionKeys.NEXT_ELIGIBLE]  ?: "" }
    val donorDistrict: Flow<String> = context.dataStore.data.map { it[SessionKeys.DONOR_DISTRICT] ?: "" }
    val isAvailable: Flow<Boolean> = context.dataStore.data.map { it[SessionKeys.IS_AVAILABLE]   ?: false }
    val isAdmin: Flow<Boolean> = context.dataStore.data.map { it[SessionKeys.IS_ADMIN]       ?: false }

    // True if today is past the nextEligible date (or never donated)
    val isEligible: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val next = prefs[SessionKeys.NEXT_ELIGIBLE] ?: ""
        if (next.isBlank()) return@map true
        try {
            val sdf      = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val nextDate = sdf.parse(next) ?: return@map true
            Date().after(nextDate)
        } catch (e : Exception) { true }
    }

    suspend fun saveSession(
        phone:        String,
        name:         String,
        uid:          String,
        bloodGroup:   String,
        lastDonation: String  = "",
        nextEligible: String  = "",
        district:     String  = "",
        isAvailable:  Boolean = true
    ) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.IS_LOGGED_IN]   = true
            prefs[SessionKeys.DONOR_PHONE]    = phone
            prefs[SessionKeys.DONOR_NAME]     = name
            prefs[SessionKeys.DONOR_UID]      = uid
            prefs[SessionKeys.BLOOD_GROUP]    = bloodGroup
            prefs[SessionKeys.LAST_DONATION]  = lastDonation
            prefs[SessionKeys.NEXT_ELIGIBLE]  = nextEligible
            prefs[SessionKeys.DONOR_DISTRICT] = district
            prefs[SessionKeys.IS_AVAILABLE]   = isAvailable
            prefs[SessionKeys.IS_ADMIN]       = false
        }
    }

    suspend fun saveAdminSession(email: String) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.IS_LOGGED_IN] = true
            prefs[SessionKeys.DONOR_NAME]   = email
            prefs[SessionKeys.IS_ADMIN]     = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}