package com.phonepulse.feature.diagnostic.ui.interactive

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonepulse.core.common.HapticManager

@Composable
fun DeadPixelTest(
    context: Context = LocalContext.current,
    onResult: (hasBadPixels: Boolean) -> Unit
) {
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    var currentIndex by remember { mutableIntStateOf(0) }
    var showQuestion by remember { mutableStateOf(false) }

    if (!showQuestion && currentIndex < colors.size) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors[currentIndex])
                .clickable {
                    HapticManager.click(context)
                    if (currentIndex < colors.size - 1) {
                        currentIndex++
                    } else {
                        showQuestion = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when (currentIndex) {
                3 -> Text("Тапните для продолжения", color = Color.Black, fontSize = 14.sp)
                4 -> Text("Тапните для продолжения", color = Color.White, fontSize = 14.sp)
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1923)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Вы заметили битые пиксели\nили пятна на экране?",
                color = Color.White,
                fontSize = 20.sp
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Button(
                    onClick = { onResult(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                ) {
                    Text("Нет, всё чисто")
                }
                Button(
                    onClick = { onResult(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744))
                ) {
                    Text("Да, есть дефекты")
                }
            }
        }
    }
}
