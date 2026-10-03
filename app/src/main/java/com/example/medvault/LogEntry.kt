package com.example.medvault

data class LogEntry(
    val id: Long = 0,
    val timestamp: Long,
    val glucose: Double?,
    val uricAcid: Double?,
    val systolic: Int?,
    val diastolic: Int?,
    val remark: String,
    val tag: String? = null
)