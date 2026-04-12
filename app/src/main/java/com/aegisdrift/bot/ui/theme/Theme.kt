package com.aegisdrift.bot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cyan400    = Color(0xFF00E5FF)
val Cyan700    = Color(0xFF00B2CC)
val Green400   = Color(0xFF00E676)
val Red400     = Color(0xFFFF1744)
val Yellow400  = Color(0xFFFFEA00)
val BgDark     = Color(0xFF0A0F1E)
val SurfaceDark= Color(0xFF131929)
val CardDark   = Color(0xFF1A2235)

private val DarkColors = darkColorScheme(
    primary        = Cyan400,
    onPrimary      = Color(0xFF003544),
    secondary      = Green400,
    onSecondary    = Color(0xFF003314),
    error          = Red400,
    background     = BgDark,
    onBackground   = Color(0xFFE0E0E0),
    surface        = SurfaceDark,
    onSurface      = Color(0xFFE0E0E0),
    surfaceVariant = CardDark,
)

@Composable
fun AegisDriftTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content
    )
}
