package com.phonepulse.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_sessions")
data class DiagnosticSessionEntity(
    @PrimaryKey
    val certId: String,
    val timestamp: Long,
    val deviceModel: String,
    val overallScore: Int,
    val grade: String,
    val certificateJson: String
)
