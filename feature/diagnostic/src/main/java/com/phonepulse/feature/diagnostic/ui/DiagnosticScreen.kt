package com.phonepulse.feature.diagnostic.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.phonepulse.core.model.TestResult
import com.phonepulse.core.model.TestStatus
import com.phonepulse.feature.diagnostic.DiagnosticPhase
import com.phonepulse.feature.diagnostic.DiagnosticState
import com.phonepulse.feature.diagnostic.DiagnosticViewModel
import com.phonepulse.feature.diagnostic.ui.interactive.BurnInTest
import com.phonepulse.feature.diagnostic.ui.interactive.DeadPixelTest
import com.phonepulse.feature.diagnostic.ui.interactive.TouchTest
import com.phonepulse.feature.diagnostic.ui.interactive.VolumeKeysTest

@Composable
fun DiagnosticScreen(
    viewModel: DiagnosticViewModel = hiltViewModel(),
    onComplete: (certId: String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.startDiagnostic()
    }

    LaunchedEffect(state.phase) {
        if (state.phase == DiagnosticPhase.COMPLETED) {
            state.certificate?.let { onComplete(it.certId) }
        }
    }

    when (state.phase) {
        DiagnosticPhase.IDLE,
        DiagnosticPhase.RUNNING_AUTO,
        DiagnosticPhase.GENERATING_CERT -> {
            AutomaticTestsView(state = state)
        }

        DiagnosticPhase.INTERACTIVE_PIXELS -> {
            Box(modifier = Modifier.fillMaxSize()) {
                DeadPixelTest(
                    onResult = { hasBadPixels -> viewModel.onPixelTestResult(hasBadPixels) }
                )
                SkipButton(
                    onSkip = { viewModel.skipInteractiveTest() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        DiagnosticPhase.INTERACTIVE_BURNIN -> {
            Box(modifier = Modifier.fillMaxSize()) {
                BurnInTest(
                    onResult = { hasBurnIn -> viewModel.onBurnInResult(hasBurnIn) }
                )
                SkipButton(
                    onSkip = { viewModel.skipInteractiveTest() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        DiagnosticPhase.INTERACTIVE_TOUCH -> {
            Box(modifier = Modifier.fillMaxSize()) {
                TouchTest(
                    onResult = { coverage -> viewModel.onTouchTestResult(coverage) }
                )
                SkipButton(
                    onSkip = { viewModel.skipInteractiveTest() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        DiagnosticPhase.INTERACTIVE_VOLUME -> {
            Box(modifier = Modifier.fillMaxSize()) {
                VolumeKeysTest(
                    onResult = { upOk, downOk -> viewModel.onVolumeKeysResult(upOk, downOk) }
                )
                SkipButton(
                    onSkip = { viewModel.skipInteractiveTest() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                )
            }
        }

        DiagnosticPhase.COMPLETED -> Unit
    }
}

@Composable
private fun SkipButton(onSkip: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onSkip, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = "Пропустить",
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun AutomaticTestsView(state: DiagnosticState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text("Диагностика", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(12.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "alpha"
        )

        val phaseText = when (state.phase) {
            DiagnosticPhase.GENERATING_CERT -> "Генерация сертификата..."
            else -> "Тестируем: ${friendlyName(state.currentModuleName)}"
        }

        Text(phaseText, fontSize = 16.sp, color = Color(0xFF00C9A7).copy(alpha = alpha))

        Spacer(Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF00C9A7),
            trackColor = Color(0xFF1A2733)
        )

        Text(
            "${(state.progress * 100).toInt()}%",
            fontSize = 14.sp,
            color = Color(0xFF8899AA),
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.completedResults) { result ->
                ResultRow(result)
            }
        }
    }
}

@Composable
private fun ResultRow(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2733)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                friendlyName(result.moduleName),
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            result.score >= 80 -> Color(0xFF00C853).copy(alpha = 0.2f)
                            result.score >= 50 -> Color(0xFFFFB300).copy(alpha = 0.2f)
                            else -> Color(0xFFFF1744).copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${result.score}",
                    fontSize = 14.sp,
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
}

private fun friendlyName(moduleName: String): String = when (moduleName) {
    "battery" -> "🔋 Батарея"
    "display" -> "📱 Экран"
    "audio" -> "🔊 Аудио"
    "camera" -> "📷 Камеры"
    "sensors" -> "🧭 Датчики"
    "connectivity" -> "📡 Связь"
    "storage" -> "💾 Память"
    "controls" -> "🎛 Управление"
    "wifi_speed" -> "📶 Wi-Fi скорость"
    else -> moduleName
}
