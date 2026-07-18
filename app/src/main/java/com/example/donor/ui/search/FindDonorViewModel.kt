package com.example.donor.ui.search

import androidx.lifecycle.ViewModel
import com.example.donor.data.model.Donor
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class FindDonorUiState(
    val isLoading: Boolean    = false,
    val donors: List<Donor>   = emptyList(),
    val errorMessage: String? = null,
    val searched: Boolean     = false,
    // Store active filters in state for robustness
    val bloodGroup: String    = "All",
    val district: String      = "All",
    val city: String          = "All",
    val showUnavailable: Boolean = false
)

class FindDonorViewModel : ViewModel() {

    private val db = FirebaseDatabase.getInstance().reference

    private val _uiState = MutableStateFlow(FindDonorUiState())
    val uiState: StateFlow<FindDonorUiState> = _uiState

    // Reference to the currently attached listener so it can be removed
    private var donorsListener: ValueEventListener? = null

    /**
     * Searches donors based on filters and keeps the results LIVE.
     */
    fun searchDonors(
        bloodGroup: String,
        district: String,
        city: String,
        showUnavailable: Boolean
    ) {
        // Validation check
        if (bloodGroup.isBlank() || district.isBlank() || city.isBlank()) {
            _uiState.update { it.copy(
                errorMessage = "Please select blood group, district, and city."
            )}
            return
        }

        _uiState.update { it.copy(
            isLoading    = true,
            errorMessage = null,
            searched     = true,
            bloodGroup   = bloodGroup,
            district     = district,
            city         = city,
            showUnavailable = showUnavailable
        )}

        attachLiveListener()
    }

    private fun attachLiveListener() {
        donorsListener?.let { db.child("donors").removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val results = mutableListOf<Donor>()
                val state = _uiState.value

                for (child in snapshot.children) {
                    val donor = child.getValue(Donor::class.java) ?: continue

                    // Filter by Blood Group
                    if (state.bloodGroup != "All" && !donor.bloodGroup.matches(state.bloodGroup)) continue

                    // Filter by District
                    if (state.district != "All" && !donor.district.matches(state.district)) continue

                    // Filter by City
                    if (state.city != "All" && !donor.city.matches(state.city)) continue

                    // Filter by availability (Now using unified logic from Donor model)
                    if (!state.showUnavailable && !donor.isCurrentlyAvailable()) continue

                    results.add(donor)
                }

                _uiState.update { it.copy(
                    isLoading = false,
                    donors    = results
                )}
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(
                    isLoading    = false,
                    errorMessage = error.message
                )}
            }
        }

        donorsListener = listener
        db.child("donors").addValueEventListener(listener)
    }

    private fun String.matches(other: String): Boolean =
        this.trim().equals(other.trim(), ignoreCase = true)

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        donorsListener?.let { db.child("donors").removeEventListener(it) }
        donorsListener = null
    }
}
