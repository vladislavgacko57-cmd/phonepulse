package com.phonepulse.feature.history

import androidx.lifecycle.ViewModel
import com.phonepulse.core.database.dao.DiagnosticDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: DiagnosticDao
) : ViewModel() {
    val sessions = dao.getAllSessions()
}
