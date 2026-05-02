package com.zwds.today.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightColors = lightColorScheme(
    primary = CinnabarRed,
    onPrimary = PaperWhite,
    primaryContainer = CinnabarRedLight,
    onPrimaryContainer = InkBlack,
    secondary = FortuneCaution,
    onSecondary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = PaperWhite,
    onSurface = InkBlack,
    surfaceVariant = PaperWhiteVariant,
    onSurfaceVariant = InkBlackSoft,
    outline = InkBlackHint,
    error = FortuneVeryGood,
    onError = PaperWhite,
)

private val DarkColors = darkColorScheme(
    primary = CinnabarRedLight,
    onPrimary = NightInk,
    primaryContainer = CinnabarRedDark,
    onPrimaryContainer = NightPaper,
    secondary = FortuneCaution,
    onSecondary = NightPaper,
    background = NightInk,
    onBackground = NightPaper,
    surface = NightInkVariant,
    onSurface = NightPaper,
    surfaceVariant = NightInkVariant,
    onSurfaceVariant = NightPaperSoft,
    outline = NightPaperSoft,
    error = FortuneVeryGood,
    onError = NightPaper,
)

@Composable
fun TodayZwdsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: FontScale = FontScale.Standard,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colors,
            typography = zwdsTypography(fontScale),
            content = content
        )
    }
}
