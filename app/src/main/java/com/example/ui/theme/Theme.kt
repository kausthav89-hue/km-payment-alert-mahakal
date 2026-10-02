package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF22C55E), // Emerald Green
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFF86EFAC),
    secondary = Color(0xFF38BDF8), // Cyan Sky
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFFA855F7), // Purple accent
    background = Color(0xFF0A0F1D),
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF16A34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = Color(0xFF9333EA),
    background = Color(0xFF0A0F1D), // We prefer the sleek dark soundbox look
    surface = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun PaymentAlertTheme(
    darkTheme: Boolean = true, // Default to sleek midnight fintech dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    PaymentAlertTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
