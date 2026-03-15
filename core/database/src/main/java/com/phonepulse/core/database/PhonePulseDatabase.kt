package com.phonepulse.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.phonepulse.core.database.dao.DiagnosticDao
import com.phonepulse.core.database.entity.DiagnosticSessionEntity

@Database(
    entities = [DiagnosticSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PhonePulseDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao
}
