package io.github.hdlee73.financenewsradar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF173B57),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7E9F6),
    onPrimaryContainer = Color(0xFF082C45),
    secondary = Color(0xFFA46208),
    secondaryContainer = Color(0xFFFFDDB5),
    background = Color(0xFFF4F7F5),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7ECE9),
    outline = Color(0xFF737875)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8CDE8),
    primaryContainer = Color(0xFF214A67),
    secondary = Color(0xFFF4BD75),
    background = Color(0xFF101413),
    surface = Color(0xFF171C1A),
    surfaceVariant = Color(0xFF303633)
)

@Composable
fun FinanceNewsRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
