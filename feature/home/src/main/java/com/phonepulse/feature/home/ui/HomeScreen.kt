package com.phonepulse.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartDiagnostic: () -> Unit,
    onScanQr: () -> Unit,
    onOpenHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PhonePulse",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            text = "Pulse check for your phone in 3 minutes.",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStartDiagnostic
        ) {
            Text("Start Diagnostics")
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = onScanQr
        ) {
            Text("Scan QR")
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = onOpenHistory
        ) {
            Text("History")
        }
    }
}
