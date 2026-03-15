package com.phonepulse.feature.diagnostic.ui.interactive

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.coroutines.delay

@Composable
fun BurnInTest(
    onResult: (hasBurnIn: Boolean) -> Unit
) {
    var phase by remember { mutableIntStateOf(0) }

    when (phase) {
        0 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1923))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🔍", fontSize = 48.sp)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Тест на выгорание экрана",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.padding(8.dp))
                Text(
                    "Сейчас экран станет серым.\n\n" +
                        "Посмотрите, нет ли:\n" +
                        "• Тёмных/светлых полос вверху (статус-бар)\n" +
                        "• Тёмных/светлых полос внизу (навигация)\n" +
                        "• Призрачных контуров клавиатуры или иконок\n\n" +
                        "Наклоните телефон под разными углами.",
                    fontSize = 14.sp,
                    color = Color(0xFF8899AA),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { phase = 1 },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C9A7))
                ) {
                    Text("Начать тест")
                }
            }
        }

        1, 2, 3 -> {
            val grayLevel = when (phase) {
                1 -> 128
                2 -> 64
                3 -> 192
                else -> 128
            }
            val label = when (phase) {
                1 -> "Серый 50% — ищите следы выгорания"
                2 -> "Серый 25% — тёмный тест"
                3 -> "Серый 75% — светлый тест"
                else -> ""
            }

            var autoAdvance by remember(phase) { mutableStateOf(false) }

            LaunchedEffect(phase) {
                delay(5000)
                autoAdvance = true
                delay(500)
                phase++
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(grayLevel, grayLevel, grayLevel))
            ) {
                if (autoAdvance) {
                    Text(
                        "Переход...",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(20.dp)
                    )
                } else {
                    Text(
                        label,
                        color = if (grayLevel > 128) Color.DarkGray else Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 60.dp)
                    )
                }
            }
        }

        4 -> {
            val infiniteTransition = rememberInfiniteTransition(label = "retention")
            val offsetX by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "x"
            )

            LaunchedEffect(Unit) {
                delay(6000)
                phase = 5
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(128, 128, 128))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .offset(x = (offsetX * 200).dp)
                        .background(Color(110, 110, 110))
                )

                Text(
                    "Движущийся паттерн — ищите \"призраки\"",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }
        }

        5 -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1923))
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Заметили ли вы:",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.padding(6.dp))
                Text(
                    "• Следы статус-бара или навигации?\n" +
                        "• Призрачные контуры иконок/клавиатуры?\n" +
                        "• Неравномерность яркости?",
                    fontSize = 15.sp,
                    color = Color(0xFF8899AA),
                    lineHeight = 24.sp
                )

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = { onResult(false) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Нет, экран чистый ✓", fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onResult(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                ) {
                    Text("Есть лёгкие следы ⚠", fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { onResult(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Text("Заметное выгорание ✗", fontSize = 16.sp)
                }
            }
        }
    }
}
