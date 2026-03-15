package com.phonepulse.feature.scanner

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.phonepulse.core.model.Certificate
import com.phonepulse.core.model.TestStatus
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

@Composable
fun ScannerScreen(
    onBack: () -> Unit = {}
) {
    var hasCameraPermission by remember { mutableStateOf(false) }
    var scannedCertificate by remember { mutableStateOf<Certificate?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        if (!hasCameraPermission && scannedCertificate == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("\uD83D\uDCF7", fontSize = 48.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Нужен доступ к камере\nдля сканирования QR-кода",
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
                ) {
                    Text("Дать разрешение")
                }
            }
        }

        if (hasCameraPermission && scannedCertificate == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onQRCodeDetected = { rawValue ->
                        try {
                            val cert = decodeCertificateFromQR(rawValue)
                            if (cert != null) {
                                scannedCertificate = cert
                                errorMessage = null
                            } else {
                                errorMessage = "Это не сертификат PhonePulse"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка чтения QR: ${e.message}"
                        }
                    }
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .border(3.dp, Color(0xFF00C9A7), RoundedCornerShape(24.dp))
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Наведите на QR-код\nсертификата PhonePulse",
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                errorMessage?.let {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFF1744).copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }

                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }
            }
        }

        scannedCertificate?.let { cert ->
            ScannedCertificateView(
                certificate = cert,
                onBack = {
                    scannedCertificate = null
                    errorMessage = null
                }
            )
        }
    }
}

@Composable
private fun ScannedCertificateView(
    certificate: Certificate,
    onBack: () -> Unit
) {
    val gradeColor = when (certificate.grade) {
        "S" -> Color(0xFF00C9A7)
        "A" -> Color(0xFF00C853)
        "B" -> Color(0xFFFFB300)
        "C" -> Color(0xFFFF9100)
        else -> Color(0xFFFF1744)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text("Проверка сертификата", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(32.dp))
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Сертификат подтвержден", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                    Text(certificate.certId, color = Color(0xFF8899AA), fontSize = 12.sp)
                    Text("Дата: ${certificate.timestamp.take(10)}", color = Color(0xFF8899AA), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "${certificate.device.manufacturer} ${certificate.device.model}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            "Android ${certificate.device.androidVersion} • ${certificate.device.ramGb.toInt()}GB RAM • ${certificate.device.storageGb.toInt()}GB",
            fontSize = 13.sp,
            color = Color(0xFF8899AA)
        )

        Spacer(Modifier.height(20.dp))

        Text("${certificate.overallScore}", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = gradeColor)
        Text("Грейд ${certificate.grade}", fontSize = 20.sp, color = gradeColor, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(20.dp))

        if (certificate.recommendedPriceMin != null && certificate.recommendedPriceMax != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("\uD83D\uDCB0 Справедливая цена", fontSize = 14.sp, color = Color(0xFF8899AA))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "%,d – %,d ₽".format(certificate.recommendedPriceMin, certificate.recommendedPriceMax),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C9A7)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Детали проверки", fontSize = 16.sp, color = Color(0xFF8899AA), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(12.dp))

        certificate.results.forEach { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (result.status) {
                            TestStatus.PASSED -> Icons.Default.CheckCircle
                            TestStatus.WARNING -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = when (result.status) {
                            TestStatus.PASSED -> Color(0xFF00C853)
                            TestStatus.WARNING -> Color(0xFFFFB300)
                            else -> Color(0xFFFF1744)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            friendlyModuleName(result.moduleName),
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                        result.details.entries.take(2).forEach { (k, v) ->
                            Text("$k: $v", fontSize = 11.sp, color = Color(0xFF8899AA))
                        }
                    }
                    Text(
                        "${result.score}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            result.score >= 80 -> Color(0xFF00C853)
                            result.score >= 50 -> Color(0xFFFFB300)
                            else -> Color(0xFFFF1744)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

private fun friendlyModuleName(name: String): String = when (name) {
    "battery" -> "\uD83D\uDD0B Батарея"
    "display" -> "\uD83D\uDCF1 Экран"
    "audio" -> "\uD83D\uDD0A Аудио"
    "camera" -> "\uD83D\uDCF7 Камеры"
    "sensors" -> "\uD83E\uDDED Датчики"
    "connectivity" -> "\uD83D\uDCE1 Связь"
    "storage" -> "\uD83D\uDCBE Память"
    "controls" -> "\uD83C\uDF9B Управление"
    else -> name
}

fun decodeCertificateFromQR(qrContent: String): Certificate? {
    val payload = when {
        qrContent.startsWith("PP1:") -> qrContent.removePrefix("PP1:")
        qrContent.contains("phonepulse", ignoreCase = true) && qrContent.contains("#") ->
            qrContent.substringAfter("#")
        else -> return null
    }

    return try {
        val compressed = Base64.getDecoder().decode(payload)
        val json = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader().readText()
        Json.decodeFromString<Certificate>(json)
    } catch (_: Exception) {
        null
    }
}
