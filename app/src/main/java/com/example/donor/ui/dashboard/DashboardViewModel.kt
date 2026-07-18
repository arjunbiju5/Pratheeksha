package com.example.donor.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.donor.data.model.Donor
import com.example.donor.data.session.SessionManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class DashboardUiState(
    val isLoading:    Boolean = true,
    val donor:        Donor?  = null,
    val errorMessage: String? = null,
    val isDeleted:    Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(application)

    private var profileListener: ValueEventListener? = null
    private var currentUid: String = ""

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        loadDonorProfile()
    }

    fun loadDonorProfile() {
        viewModelScope.launch {
            // ← Use SessionManager uid, NOT FirebaseAuth
            val uid = sessionManager.donorUid.first()

            if (uid.isBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Not logged in.") }
                return@launch
            }

            currentUid = uid
            _uiState.update { it.copy(isLoading = true) }

            profileListener?.let {
                db.child("donors").child(uid).removeEventListener(it)
            }

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val donor = snapshot.getValue(Donor::class.java)
                    _uiState.update { it.copy(isLoading = false, donor = donor) }
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            }

            profileListener = listener
            db.child("donors").child(uid).addValueEventListener(listener)
        }
    }

    fun saveDonationDate(lastDonationDate: String) {
        if (currentUid.isBlank()) return

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        try { cal.time = sdf.parse(lastDonationDate) ?: Date() }
        catch (e: Exception) { cal.time = Date() }
        cal.add(Calendar.DAY_OF_YEAR, 90)
        val nextEligible = sdf.format(cal.time)

        db.child("donors").child(currentUid).updateChildren(
            mapOf("lastDonation" to lastDonationDate, "nextEligible" to nextEligible)
        )
        db.child("donationHistory").child(currentUid).push().setValue(
            mapOf("date" to lastDonationDate, "location" to "Recorded via app")
        )
    }

    fun clearDonationDate() {
        if (currentUid.isBlank()) return
        db.child("donors").child(currentUid).updateChildren(
            mapOf("lastDonation" to "Never", "nextEligible" to "")
        )
    }

    fun toggleAvailability(newValue: Boolean) {
        if (currentUid.isBlank()) return
        db.child("donors").child(currentUid).child("available").setValue(newValue)
    }

    fun deleteProfile() {
        if (currentUid.isBlank()) return
        db.child("donors").child(currentUid).removeValue()
            .addOnSuccessListener {
                db.child("donationHistory").child(currentUid).removeValue()
                viewModelScope.launch {
                    sessionManager.clearSession()
                    _uiState.update { it.copy(isDeleted = true) }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
    }

    fun daysRemaining(nextEligible: String): Int {
        return try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val next = sdf.parse(nextEligible) ?: return 0
            val diff = next.time - Date().time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) { 0 }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    override fun onCleared() {
        super.onCleared()
        if (currentUid.isNotBlank()) {
            profileListener?.let {
                db.child("donors").child(currentUid).removeEventListener(it)
            }
        }
    }
}