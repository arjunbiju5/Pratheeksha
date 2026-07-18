package com.example.donor.data.model

data class BloodRequest(
    val id:            String = "",
    val requesterName: String = "",
    val mobile:        String = "",
    val bloodGroup:    String = "",
    val hospital:      String = "",
    val district:      String = "",
    val city:          String = "",
    val urgency:       String = "normal",
    val note:          String = "",
    val requestDate:   String = "",
    val status:        String = "pending",
    val responses:     Map<String, Any> = emptyMap()
)