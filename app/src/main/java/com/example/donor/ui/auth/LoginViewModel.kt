package com.example.donor.ui.login
import kotlinx.coroutines.tasks.await
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.donor.data.session.SessionManager
import com.example.donor.service.BloodRequestService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.donor.service.FcmTokenManager

data class LoginUiState(
    val isLoading: Boolean    = false,
    val isSuccess: Boolean    = false,
    val errorMessage: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(application)
    private val requestService = BloodRequestService(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(phone: String, dateOfBirth: String) {
        val trimmedPhone = phone.trim()
        val trimmedDob   = dateOfBirth.trim()

        if (trimmedPhone.isBlank() || trimmedDob.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all fields.") }
            return
        }
        if (trimmedPhone.length < 10) {
            _uiState.update { it.copy(errorMessage = "Enter a valid phone number.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        db.child("donors")
            .orderByChild("mobile")
            .equalTo(trimmedPhone)
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "No account found with this phone number.") }
                        return
                    }

                    var matched   = false
                    var wrongDob  = false
                    var notActive = false

                    for (child in snapshot.children) {
                        val dob    = child.child("dateOfBirth").getValue(String::class.java) ?: ""

                        if (dob != trimmedDob) { wrongDob = true; continue }

                        val uid          = child.key ?: ""
                        val name         = child.child("fullName").getValue(String::class.java)    ?: ""
                        val bloodGroup   = child.child("bloodGroup").getValue(String::class.java)  ?: ""
                        val lastDonation = child.child("lastDonation").getValue(String::class.java) ?: ""
                        val nextEligible = child.child("nextEligible").getValue(String::class.java) ?: ""
                        val district     = child.child("district").getValue(String::class.java)    ?: ""
                        val available    = child.child("available").getValue(Boolean::class.java)  ?: true

                        viewModelScope.launch {
                            sessionManager.saveSession(
                                phone        = trimmedPhone,
                                name         = name,
                                uid          = uid,
                                bloodGroup   = bloodGroup,
                                lastDonation = lastDonation,
                                nextEligible = nextEligible,
                                district     = district,
                                isAvailable  = available
                            )
                            // Force a fresh token fetch and save
                            try {
                                com.google.firebase.messaging.FirebaseMessaging.getInstance().deleteToken().await()
                                FcmTokenManager.saveToken(uid)
                            } catch (e: Exception) {
                                // If delete fails, try saving anyway
                                FcmTokenManager.saveToken(uid)
                            }
                            // Start listening for blood requests matching this donor
                            requestService.startListeningAsDonor()

                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                        matched = true
                        break
                    }

                    if (!matched) {
                        val msg = when {
                            wrongDob  -> "Date of birth does not match our records."
                            else      -> "No active account found. Contact admin."
                        }
                        _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            })
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}