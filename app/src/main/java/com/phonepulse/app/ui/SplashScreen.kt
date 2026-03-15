package com.phonepulse.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.phonepulse.app.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var showText by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_pulse)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1.2f
    )

    // Show title even if animation fails to load.
    LaunchedEffect(Unit) {
        delay(350)
        showText = true
    }

    // Normal path: move next after animation completion.
    LaunchedEffect(progress) {
        if (progress >= 0.98f && !finished) {
            finished = true
            delay(250)
            onFinished()
        }
    }

    // Fallback path: never stay stuck on splash.
    LaunchedEffect(Unit) {
        delay(2500)
        if (!finished) {
            finished = true
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(24.dp))

            AnimatedVisibility(
                visible = showText,
                enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PhonePulse",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Пульс телефона за 3 минуты",
                        fontSize = 14.sp,
                        color = Color(0xFF00C9A7)
                    )
                }
            }
        }
    }
}
