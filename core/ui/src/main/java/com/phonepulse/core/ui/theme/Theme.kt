package com.phonepulse.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimary,
    secondary = Secondary,
    onSecondary = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    error = Error,
    onError = TextPrimary
)

@Composable
fun PhonePulseTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else DarkColorScheme
    MaterialTheme(
        colorScheme = colors,
        typography = PhonePulseTypography,
        content = content
    )
}
