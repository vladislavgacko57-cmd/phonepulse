package com.phonepulse.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phonepulse.core.database.dao.DiagnosticDao
import com.phonepulse.core.database.entity.DiagnosticSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticDaoTest {

    private lateinit var db: PhonePulseDatabase
    private lateinit var dao: DiagnosticDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PhonePulseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.diagnosticDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun makeSession(
        certId: String = "PP-2025-TEST0001",
        score: Int = 85,
        grade: String = "A"
    ) = DiagnosticSessionEntity(
        certId = certId,
        timestamp = System.currentTimeMillis(),
        deviceModel = "Samsung Galaxy S23",
        overallScore = score,
        grade = grade,
        certificateJson = """{"certId":"$certId","overallScore":$score}"""
    )

    @Test
    fun insertAndRetrieveSession() = runBlocking {
        val session = makeSession()
        dao.insertSession(session)

        val retrieved = dao.getSession("PP-2025-TEST0001")
        assertNotNull(retrieved)
        assertEquals("PP-2025-TEST0001", retrieved!!.certId)
        assertEquals(85, retrieved.overallScore)
        assertEquals("A", retrieved.grade)
    }

    @Test
    fun getAllSessionsReturnsInsertedItems() = runBlocking {
        dao.insertSession(makeSession("PP-2025-A001"))
        dao.insertSession(makeSession("PP-2025-A002"))
        dao.insertSession(makeSession("PP-2025-A003"))

        val all = dao.getAllSessions().first()
        assertEquals(3, all.size)
    }

    @Test
    fun getAllSessionsOrderedByTimestampDesc() = runBlocking {
        dao.insertSession(makeSession("PP-OLD").copy(timestamp = 1000L))
        dao.insertSession(makeSession("PP-NEW").copy(timestamp = 3000L))
        dao.insertSession(makeSession("PP-MID").copy(timestamp = 2000L))

        val all = dao.getAllSessions().first()
        assertEquals("PP-NEW", all[0].certId)
        assertEquals("PP-MID", all[1].certId)
        assertEquals("PP-OLD", all[2].certId)
    }

    @Test
    fun getSessionReturnsNullForNonExistent() = runBlocking {
        val result = dao.getSession("NON_EXISTENT")
        assertNull(result)
    }

    @Test
    fun insertReplacesDuplicate() = runBlocking {
        dao.insertSession(makeSession("PP-DUP", score = 70, grade = "B"))
        dao.insertSession(makeSession("PP-DUP", score = 95, grade = "S"))

        val retrieved = dao.getSession("PP-DUP")
        assertEquals(95, retrieved!!.overallScore)
        assertEquals("S", retrieved.grade)
    }

    @Test
    fun deleteRemovesSession() = runBlocking {
        val session = makeSession("PP-DEL")
        dao.insertSession(session)

        assertNotNull(dao.getSession("PP-DEL"))

        dao.deleteSession(session)

        assertNull(dao.getSession("PP-DEL"))
    }

    @Test
    fun emptyDatabaseReturnsEmptyList() = runBlocking {
        val all = dao.getAllSessions().first()
        assertTrue(all.isEmpty())
    }
}
