package com.example.donor.ui.request

import kotlinx.coroutines.flow.first
import com.example.donor.data.session.DeviceIdManager
import com.example.donor.data.session.SessionManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.donor.service.FcmNotificationSender
import com.example.donor.service.FcmTokenManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class RequestBloodUiState(
    val isLoading:           Boolean = false,
    val isSendingNotifications: Boolean = false,
    val isSuccess:           Boolean = false,
    val errorMessage:        String? = null,
    val requestId:           String? = null
)

class RequestBloodViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(app)
    private val _uiState = MutableStateFlow(RequestBloodUiState())
    val uiState: StateFlow<RequestBloodUiState> = _uiState

    fun submitRequest(
        patientName:   String,
        bystanderName: String,
        contactPhone:  String,
        bloodGroup:    String,
        units:         Int,
        hospital:      String,
        district:      String,
        city:          String,
        medicalCase:   String,
        note:          String,
        urgency:       String,
        requiredUntil: String
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Get this device's FCM token so the requester can be notified later
                val requesterToken = FcmTokenManager.getToken()
                val deviceId        = DeviceIdManager.getOrCreateDeviceId(getApplication())
                val ref = db.child("bloodRequests").push()
                val id  = ref.key ?: UUID.randomUUID().toString()

                val fullNote = buildString {
                    append("Medical case: $medicalCase")
                    if (units > 1)                  append(" | Units: $units")
                    if (bystanderName.isNotBlank())  append(" | Bystander: $bystanderName")
                    if (note.isNotBlank())           append(" | $note")
                }

                val request = mapOf(
                    "id"                 to id,
                    "requesterName"      to patientName,
                    "mobile"             to contactPhone,
                    "bloodGroup"         to bloodGroup,
                    "hospital"           to hospital,
                    "district"           to district,
                    "city"               to city,
                    "urgency"            to urgency,
                    "note"               to fullNote,
                    "requestDate"        to SimpleDateFormat(
                        "dd/MM/yyyy HH:mm",
                        Locale.getDefault()
                    ).format(Date()),
                    "requiredUntil"      to requiredUntil,
                    "status"             to "pending",
                    "responses"          to mapOf<String, Any>(),
                    "requesterFcmToken"  to requesterToken,  // NEW — donor uses this to notify requester
                    "requestedByUid"     to deviceId,
                    "requestedByDonorUid" to sessionManager.donorUid.first()
                )

                // 2. Write request to Firebase
                ref.setValue(request).await()

                _uiState.update { it.copy(isLoading = false, isSendingNotifications = true) }

                // 3. Find matching donors and notify them
                notifyMatchingDonors(bloodGroup, district, id, patientName, hospital, urgency)


                // 4. Notify all admins of the new request
                notifyAdmins(bloodGroup, hospital, district, urgency, id)

                _uiState.update { it.copy(isSendingNotifications = false, isSuccess = true, requestId = id) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Failed to send request.")
                }
            }
        }
    }
    private suspend fun notifyAdmins(
        bloodGroup: String,
        hospital:   String,
        district:   String,
        urgency:    String,
        requestId:  String
    ) {
        try {
            val snapshot = db.child("admins").get().await()
            val tokens = snapshot.children.mapNotNull {
                it.child("fcmToken").getValue(String::class.java)
            }.filter { it.isNotBlank() }

            if (tokens.isEmpty()) return

            FcmNotificationSender.sendToMultiple(
                context = getApplication(),
                tokens  = tokens,
                title   = "New Blood Request — $bloodGroup",
                body    = "$hospital, $district" ,
                data    = mapOf("requestId" to requestId, "type" to "new_request_admin")
            )
        } catch (e: Exception) {
            android.util.Log.e("RequestBloodVM", "Failed to notify admins: ${e.message}")
        }
    }
    private suspend fun notifyMatchingDonors(
        bloodGroup: String,
        district:   String,
        requestId:  String,
        patientName: String,
        hospital:   String,
        urgency:    String
    ) {
        try {
            // Get the submitter's own donorUid so we can exclude them
            val myDonorUid = sessionManager.donorUid.first()

            val snapshot = db.child("donors")
                .orderByChild("district")
                .equalTo(district)
                .get()
                .await()

            val today = Date()

            val tokens = snapshot.children.mapNotNull { child ->
                val donorUid = child.key ?: return@mapNotNull null  // ADD THIS
                val donorBloodGroup =
                    child.child("bloodGroup").getValue(String::class.java) ?: return@mapNotNull null
                val donorDistrict =
                    child.child("district").getValue(String::class.java) ?: return@mapNotNull null
                val available = child.child("available").getValue(Boolean::class.java) ?: false
                val nextEligible = child.child("nextEligible").getValue(String::class.java) ?: ""
                val fcmToken =
                    child.child("fcmToken").getValue(String::class.java) ?: return@mapNotNull null

                if (donorBloodGroup != bloodGroup) return@mapNotNull null
                if (donorDistrict != district) return@mapNotNull null
                if (!available) return@mapNotNull null
                if (donorUid == myDonorUid && myDonorUid.isNotBlank()) return@mapNotNull null  // ADD THIS

                if (nextEligible.isNotBlank()) {
                    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val eligibleDate = runCatching { fmt.parse(nextEligible) }.getOrNull()
                    if (eligibleDate != null && today.before(eligibleDate)) return@mapNotNull null
                }

                fcmToken
            }

            if (tokens.isEmpty()) return

            val title = "🩸 Blood needed — $bloodGroup ($district)"
            val body = "Patient: $patientName | $hospital | $district"

            FcmNotificationSender.sendToMultiple(
                context = getApplication(),
                tokens = tokens,
                title = title,
                body = body,
                data = mapOf("requestId" to requestId, "type" to "new_request")
            )

        } catch (e: Exception) {
            android.util.Log.e("RequestBloodVM", "Failed to notify donors: ${e.message}")
        }
    }
}