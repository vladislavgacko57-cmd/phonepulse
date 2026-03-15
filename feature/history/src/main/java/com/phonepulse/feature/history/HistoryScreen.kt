package com.phonepulse.feature.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonepulse.core.database.entity.DiagnosticSessionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onSessionClick: (certId: String) -> Unit,
    onCompareSelected: (certId1: String, certId2: String) -> Unit
) {
    val sessions by viewModel.sessions.collectAsState(initial = emptyList())
    var selectedForCompare by remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(24.dp)
    ) {
        Text(
            text = "📋 История диагностик",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))

        if (selectedForCompare.size == 2) {
            Button(
                onClick = {
                    onCompareSelected(selectedForCompare[0], selectedForCompare[1])
                    selectedForCompare = emptyList()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF845EC2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text("📊 Сравнить выбранные")
            }
        }

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Пока нет диагностик", color = Color(0xFF8899AA), fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions) { session ->
                    val isSelected = selectedForCompare.contains(session.certId)
                    HistoryCard(
                        session = session,
                        isSelected = isSelected,
                        onClick = {
                            if (selectedForCompare.isEmpty()) {
                                onSessionClick(session.certId)
                            } else {
                                selectedForCompare = toggleSelection(selectedForCompare, session.certId)
                            }
                        },
                        onLongClick = {
                            selectedForCompare = toggleSelection(selectedForCompare, session.certId)
                        }
                    )
                }
            }
        }
    }
}

private fun toggleSelection(current: List<String>, certId: String): List<String> {
    if (current.contains(certId)) {
        return current - certId
    }
    return if (current.size < 2) {
        current + certId
    } else {
        listOf(current[1], certId)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    session: DiagnosticSessionEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A3750) else Color(0xFF1A2733)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = session.deviceModel,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = dateFormat.format(Date(session.timestamp)),
                    fontSize = 12.sp,
                    color = Color(0xFF8899AA)
                )
                Text(
                    text = session.certId,
                    fontSize = 11.sp,
                    color = Color(0xFF556677)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${session.overallScore}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        session.overallScore >= 85 -> Color(0xFF00C853)
                        session.overallScore >= 70 -> Color(0xFFFFB300)
                        else -> Color(0xFFFF1744)
                    }
                )
                Text(
                    text = session.grade,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8899AA)
                )
            }
        }
    }
}
