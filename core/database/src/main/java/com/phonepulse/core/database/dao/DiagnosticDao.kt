package com.phonepulse.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phonepulse.core.database.entity.DiagnosticSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostic_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<DiagnosticSessionEntity>>

    @Query("SELECT * FROM diagnostic_sessions WHERE certId = :id")
    suspend fun getSession(id: String): DiagnosticSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: DiagnosticSessionEntity)

    @Delete
    suspend fun deleteSession(session: DiagnosticSessionEntity)
}
