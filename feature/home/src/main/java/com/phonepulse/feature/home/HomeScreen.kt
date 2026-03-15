package com.phonepulse.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onStartDiagnostic: () -> Unit,
    onScanQR: () -> Unit,
    onHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text(text = "📱", fontSize = 64.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "PhonePulse",
            modifier = Modifier.testTag("home_app_name"),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Пульс телефона за 3 минуты",
            modifier = Modifier.testTag("home_slogan"),
            fontSize = 16.sp,
            color = Color(0xFF8899AA)
        )

        Spacer(Modifier.height(60.dp))

        Button(
            onClick = onStartDiagnostic,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .testTag("home_start_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(12.dp))
            Text("Начать диагностику", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onScanQR,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("home_scan_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF845EC2))
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text("Сканировать сертификат", fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))

        TextButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_history_button")
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color(0xFF8899AA))
            Spacer(Modifier.width(8.dp))
            Text("История диагностик", color = Color(0xFF8899AA))
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "v1.0.0 • PhonePulse",
            fontSize = 12.sp,
            color = Color(0xFF556677),
            textAlign = TextAlign.Center
        )
    }
}
