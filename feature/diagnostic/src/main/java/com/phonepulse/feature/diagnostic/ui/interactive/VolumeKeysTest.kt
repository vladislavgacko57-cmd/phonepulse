package com.phonepulse.feature.diagnostic.ui.interactive

import android.content.Context
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonepulse.core.common.HapticManager

@Composable
fun VolumeKeysTest(
    context: Context = LocalContext.current,
    onResult: (volumeUpOk: Boolean, volumeDownOk: Boolean) -> Unit
) {
    var volumeUpPressed by remember { mutableStateOf(false) }
    var volumeDownPressed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(volumeUpPressed, volumeDownPressed) {
        if (volumeUpPressed && volumeDownPressed) {
            onResult(true, true)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        volumeUpPressed = true
                        HapticManager.click(context)
                        true
                    }

                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        volumeDownPressed = true
                        HapticManager.click(context)
                        true
                    }

                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Нажмите кнопки громкости", color = Color.White, fontSize = 20.sp)
        Spacer(Modifier.height(40.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = if (volumeUpPressed) Color(0xFF00C853) else Color(0xFF8899AA),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (volumeUpPressed) "✓" else "Громкость +",
                    color = if (volumeUpPressed) Color(0xFF00C853) else Color(0xFF8899AA)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.VolumeDown,
                    contentDescription = null,
                    tint = if (volumeDownPressed) Color(0xFF00C853) else Color(0xFF8899AA),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = if (volumeDownPressed) "✓" else "Громкость -",
                    color = if (volumeDownPressed) Color(0xFF00C853) else Color(0xFF8899AA)
                )
            }
        }
    }
}
