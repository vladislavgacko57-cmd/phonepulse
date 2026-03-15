package com.phonepulse.feature.diagnostic.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionGateScreen(
    onAllGranted: () -> Unit
) {
    val permissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }.toTypedArray()
    }

    var allGranted by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        allGranted = results.values.all { it }
        if (allGranted) onAllGranted()
        requestSent = true
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("\uD83D\uDD10", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Разрешения",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Для полной диагностики нужен доступ к камере, микрофону, местоположению и Bluetooth",
            fontSize = 14.sp,
            color = Color(0xFF8899AA),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        if (requestSent && !allGranted) {
            Text(
                "Некоторые разрешения не выданы.\nДиагностика возможна, но некоторые тесты будут пропущены.",
                fontSize = 13.sp,
                color = Color(0xFFFFB300),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { launcher.launch(permissions) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00C9A7))
                ) {
                    Text("Повторить")
                }
                Button(
                    onClick = onAllGranted,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
                ) {
                    Text("Продолжить без них")
                }
            }
        } else if (!requestSent) {
            CircularProgressIndicator(color = Color(0xFF00C9A7))
        }
    }
}
