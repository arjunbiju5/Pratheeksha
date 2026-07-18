package com.example.donor.ui.registration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.donor.data.model.Donor
import com.example.donor.data.session.SessionManager
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class RegistrationUiState(
    val isLoading:    Boolean = false,
    val isSuccess:    Boolean = false,
    val errorMessage: String? = null
)

class RegistrationViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(application)

    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState

    fun saveDonor(
        fullName:     String,
        bloodGroup:   String,
        district:     String,
        city:         String,
        dateOfBirth:  String,
        weight:       String,
        lastDonation: String,
        mobile:       String
    ) {
        if (fullName.isBlank() || bloodGroup.isBlank() || district.isBlank() || city.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill all required fields.") }
            return
        }
        if (mobile.isBlank() || mobile.length < 10) {
            _uiState.update { it.copy(errorMessage = "Enter a valid 10-digit phone number.") }
            return
        }
        if (weight == "<50 kg") {
            _uiState.update { it.copy(errorMessage = "Donors must weigh at least 50 kg to be eligible.") }
            return
        }

        // Must be at least 18 years old
        if (dateOfBirth.isNotBlank()) {
            try {
                val sdf      = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val dob      = sdf.parse(dateOfBirth)
                if (dob != null) {
                    val today = Calendar.getInstance()
                    val birth = Calendar.getInstance().apply { time = dob }
                    var age   = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
                    if (age < 18) {
                        _uiState.update { it.copy(errorMessage = "You must be at least 18 years old to register as a donor.") }
                        return
                    }
                }
            } catch (e: Exception) { /* ignore parse errors, let Firebase handle it */ }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // Check if mobile already registered
        db.child("donors")
            .orderByChild("mobile")
            .equalTo(mobile)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    _uiState.update {
                        it.copy(isLoading = false,
                            errorMessage = "This phone number is already registered.")
                    }
                    return@addOnSuccessListener
                }

                val newRef = db.child("donors").push()
                val uid    = newRef.key ?: ""

                val cleanLastDonation = if (lastDonation.isBlank() || lastDonation == "Never") "" else lastDonation

                val donor = Donor(
                    uid          = uid,
                    fullName     = fullName,
                    bloodGroup   = bloodGroup,
                    mobile       = mobile,
                    city         = city,
                    district     = district,
                    regDate      = todayString(),
                    lastDonation = cleanLastDonation,
                    nextEligible = "",
                    available    = true,
                    dateOfBirth  = dateOfBirth,
                    weight       = weight
                )

                newRef.setValue(donor)
                    .addOnSuccessListener {
                        // ── Save session so DashboardViewModel can find the donor ──
                        viewModelScope.launch {
                            sessionManager.saveSession(
                                phone        = mobile,
                                name         = fullName,
                                uid          = uid,
                                bloodGroup   = bloodGroup,
                                lastDonation = cleanLastDonation,
                                nextEligible = "",
                                district     = district,
                                isAvailable  = true
                            )
                            _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                        }
                    }
                    .addOnFailureListener { e ->
                        _uiState.update {
                            it.copy(isLoading = false,
                                errorMessage = e.localizedMessage ?: "Failed to save. Try again.")
                        }
                    }
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to check existing records.")
                }
            }
    }

    private fun todayString(): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
}