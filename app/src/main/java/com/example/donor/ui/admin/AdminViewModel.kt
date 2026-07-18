//admin view model
package com.example.donor.ui.admin

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.example.donor.data.session.SessionManager
import com.example.donor.service.FcmTokenManager
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat
import androidx.lifecycle.AndroidViewModel
import com.example.donor.data.model.Donor
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class AdminUiState(
    val isLoading: Boolean       = false,
    val isLoggedIn: Boolean      = false,
    val allDonors: List<Donor>   = emptyList(),
    val errorMessage: String?    = null,
    val infoMessage: String?     = null,
    val adminEmail: String       = "",
    val selectedDonor: Donor?    = null
)

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(application)

    private var allDonorsListener: ValueEventListener? = null

    private val _adminUiState = MutableStateFlow(AdminUiState())
    val adminUiState: StateFlow<AdminUiState> = _adminUiState

    fun adminLogin(email: String, password: String) {
        _adminUiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user == null) {
                    _adminUiState.update { it.copy(
                        isLoading = false,
                        errorMessage = "Login failed: User is null"
                    )}
                    return@addOnSuccessListener
                }
                val uid = user.uid

                // Check if user is admin
                db.child("admins").child(uid).get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            viewModelScope.launch {
                                sessionManager.saveAdminSession(email)
                                _adminUiState.update { it.copy(
                                    isLoading  = false,
                                    isLoggedIn = true,
                                    adminEmail = email
                                )}
                                loadAllDonors()
                                val token = FcmTokenManager.getToken()
                                if (token.isNotBlank()) {
                                    db.child("admins").child(uid).child("fcmToken").setValue(token)
                                }
                            }

                        } else {
                            auth.signOut()
                            _adminUiState.update { it.copy(
                                isLoading    = false,
                                errorMessage = "Not authorized as admin"
                            )}
                        }
                    }
                    .addOnFailureListener { e ->
                        auth.signOut()
                        _adminUiState.update { it.copy(
                            isLoading    = false,
                            errorMessage = e.localizedMessage ?: "Authorization check failed"
                        )}
                    }
            }
            .addOnFailureListener { e ->
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = e.localizedMessage ?: "Login failed"
                )}
            }
    }

    /**
     * Sends a Firebase password-reset email to the given address.
     * Lets the admin (the client) recover/reset their own password
     * without ever needing to ask the developer for it.
     */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _adminUiState.update { it.copy(errorMessage = "Enter your email first") }
            return
        }

        _adminUiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                _adminUiState.update { it.copy(
                    isLoading   = false,
                    infoMessage = "Password reset link sent to $email"
                )}
            }
            .addOnFailureListener { e ->
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = e.localizedMessage ?: "Failed to send reset email"
                )}
            }
    }

    /**
     * Lets a logged-in admin change their own password from inside the app.
     * Firebase requires re-authentication with the current password before
     * a sensitive action like updatePassword() is allowed.
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user == null || user.email == null) {
            _adminUiState.update { it.copy(errorMessage = "Not logged in") }
            return
        }

        if (newPassword.length < 6) {
            _adminUiState.update { it.copy(errorMessage = "New password must be at least 6 characters") }
            return
        }

        _adminUiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        _adminUiState.update { it.copy(
                            isLoading   = false,
                            infoMessage = "Password updated successfully"
                        )}
                    }
                    .addOnFailureListener { e ->
                        _adminUiState.update { it.copy(
                            isLoading    = false,
                            errorMessage = e.localizedMessage ?: "Failed to update password"
                        )}
                    }
            }
            .addOnFailureListener {
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = "Current password is incorrect"
                )}
            }
    }

    fun loadAllDonors() {
        // Remove existing listener to prevent stacking
        allDonorsListener?.let { db.child("donors").removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val donors = mutableListOf<Donor>()
                for (child in snapshot.children) {
                    val donor = child.getValue(Donor::class.java)
                    if (donor != null) donors.add(donor)
                }
                _adminUiState.update { it.copy(allDonors = donors) }
            }

            override fun onCancelled(error: DatabaseError) {
                _adminUiState.update { it.copy(
                    errorMessage = error.message
                )}
            }
        }

        allDonorsListener = listener
        db.child("donors").addValueEventListener(listener)
    }

    fun selectDonor(donor: Donor) {
        _adminUiState.update { it.copy(selectedDonor = donor) }
    }

    fun clearSelectedDonor() {
        _adminUiState.update { it.copy(selectedDonor = null) }
    }

    /**
     * Creates a brand-new donor record (Admin "Add Donor" flow).
     * Generates a fresh Firebase push key so the new donor gets a real
     * uid consistent with donors created via the phone-auth registration
     * flow, then writes the full Donor object to /donors/{newUid}.
     */
    fun addDonor(donor: Donor) {
        _adminUiState.update { it.copy(isLoading = true) }

        val uid = java.util.UUID.randomUUID().toString()

        // Use the Donor data class directly to ensure all fields (including 'available') are written
        val donorToSave = donor.copy(
            uid = uid,
            regDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date()),
            lastDonation = if (donor.lastDonation.isBlank()) "" else donor.lastDonation.trim()
        )

        // Save to Firebase
        db.child("donors").child(uid).setValue(donorToSave)
            .addOnSuccessListener {
                _adminUiState.update { it.copy(
                    isLoading = false,
                    selectedDonor = null
                )}
                // loadAllDonors() is redundant here as we have a live listener,
                // but kept if the UI needs immediate confirmation.
            }
            .addOnFailureListener { e ->
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = e.localizedMessage ?: "Failed to add donor"
                )}
            }
    }

    /**
     * Updates an existing donor's information in Firebase.
     */
    fun updateDonor(donor: Donor) {
        if (donor.uid.isBlank()) {
            _adminUiState.update { it.copy(errorMessage = "Donor ID is missing") }
            return
        }

        _adminUiState.update { it.copy(isLoading = true) }

        // Use the Donor object itself for the update to maintain consistency
        db.child("donors").child(donor.uid).setValue(donor)
            .addOnSuccessListener {
                _adminUiState.update { it.copy(
                    isLoading = false,
                    selectedDonor = null
                )}
            }
            .addOnFailureListener { e ->
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = e.localizedMessage ?: "Failed to update donor"
                )}
            }
    }

    fun deleteDonor(donorUid: String) {
        _adminUiState.update { it.copy(isLoading = true) }

        db.child("donors").child(donorUid).removeValue()
            .addOnSuccessListener {
                db.child("donationHistory").child(donorUid).removeValue()
                _adminUiState.update { it.copy(
                    isLoading     = false,
                    selectedDonor = null
                )}
            }
            .addOnFailureListener { e ->
                _adminUiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = e.localizedMessage ?: "Delete failed"
                )}
            }
    }

    fun adminLogout() {
        auth.signOut()
        _adminUiState.update {
            AdminUiState()
        }
    }

    fun setError(message: String) {
        _adminUiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _adminUiState.update { it.copy(errorMessage = null) }
    }

    fun clearInfo() {
        _adminUiState.update { it.copy(infoMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        allDonorsListener?.let { db.child("donors").removeEventListener(it) }
        allDonorsListener = null
    }
}