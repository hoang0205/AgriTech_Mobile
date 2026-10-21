package com.example.agritech_mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryDarkGreen,
    secondary = LightLeafGreen,
    background = BackgroundLightGray,
    surface = CardBackgroundGray,
    onPrimary = Color.White,
    onBackground = TextDarkGray,
    onSurface = TextDarkGray,
    outline = TextLightGray
)

@Composable
fun AgritechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}