package com.purplegate.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PurpleGateColors = darkColorScheme(
    primary = GrapePurpleBright,
    onPrimary = Color.White,
    secondary = TorchOrange,
    onSecondary = Color.Black,
    tertiary = SlimeGreen,
    background = MudDark,
    onBackground = Color.White,
    surface = MudBrown,
    onSurface = Color.White,
)

@Composable
fun PurpleGateTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PurpleGateColors,
        content = content
    )
}
