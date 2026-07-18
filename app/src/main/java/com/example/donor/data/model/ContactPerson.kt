package com.example.donor.data.model

data class ContactPerson(
    val uid:   String = "",
    val name:  String = "",
    val role:  String = "",   // currently used as phone number per your existing data
    val phone: String = ""
)