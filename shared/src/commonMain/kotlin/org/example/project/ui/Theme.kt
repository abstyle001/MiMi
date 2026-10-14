package org.example.project.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 围绕橘猫主色 #E8935A 的暖色系 Material 3 配色 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF9A4C17),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF765849),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFEDBCB),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF646116),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEBE78E),
    onTertiaryContainer = Color(0xFF1E1C00),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFF8F4),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB68C),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF723700),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFE5BFAE),
    onSecondary = Color(0xFF432A1E),
    secondaryContainer = Color(0xFF5C4032),
    onSecondaryContainer = Color(0xFFFEDBCB),
    tertiary = Color(0xFFCFCB75),
    onTertiary = Color(0xFF333200),
    tertiaryContainer = Color(0xFF4B4900),
    onTertiaryContainer = Color(0xFFEBE78E),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
