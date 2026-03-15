package com.phonepulse.feature.diagnostic.ui.interactive

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonepulse.core.common.HapticManager
import kotlinx.coroutines.delay

@Composable
fun TouchTest(
    context: Context = LocalContext.current,
    onResult: (coveragePct: Int) -> Unit
) {
    val gridCols = 6
    val gridRows = 10
    val totalCells = gridCols * gridRows

    val touchedCells = remember { mutableStateMapOf<Int, Boolean>() }
    var timeLeft by remember { mutableIntStateOf(10) }
    var finished by remember { mutableStateOf(false) }

    val coverage by remember {
        derivedStateOf { (touchedCells.size * 100) / totalCells }
    }

    LaunchedEffect(Unit) {
        while (timeLeft > 0 && !finished) {
            delay(1000)
            timeLeft--
        }
        if (!finished) {
            finished = true
            onResult(coverage)
        }
    }

    LaunchedEffect(coverage) {
        if (coverage >= 90 && !finished) {
            finished = true
            onResult(coverage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val col = (change.position.x / (size.width.toFloat() / gridCols)).toInt()
                            .coerceIn(0, gridCols - 1)
                        val row = (change.position.y / (size.height.toFloat() / gridRows)).toInt()
                            .coerceIn(0, gridRows - 1)
                        val key = row * gridCols + col
                        if (!touchedCells.containsKey(key)) {
                            touchedCells[key] = true
                            HapticManager.touchCell(context)
                        }
                    }
                }
        ) {
            val cellWidth = size.width / gridCols
            val cellHeight = size.height / gridRows

            for (col in 0 until gridCols) {
                for (row in 0 until gridRows) {
                    val key = row * gridCols + col
                    val isTouched = touchedCells.containsKey(key)
                    drawRect(
                        color = if (isTouched) Color(0xFF00C9A7).copy(alpha = 0.5f) else Color(0xFF1A2733),
                        topLeft = Offset(col * cellWidth, row * cellHeight),
                        size = Size(cellWidth - 2f, cellHeight - 2f)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Водите пальцем по всему экрану", color = Color.White, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Покрытие: $coverage%  •  Осталось: ${timeLeft}с",
                color = Color(0xFF00C9A7),
                fontSize = 14.sp
            )
        }
    }
}
