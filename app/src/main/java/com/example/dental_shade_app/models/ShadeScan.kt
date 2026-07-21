package com.example.dental_shade_app.models

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ShadeScan(
    val id: String? = null,
    val patientName: String? = null,
    val shadeResult: String? = null,
    val timestamp: Long? = null,
    val confidence: Double? = null
)
