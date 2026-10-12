package com.zwds.today.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5944C6), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E0FF), onPrimaryContainer = Color(0xFF352778),
    secondary = Color(0xFF056775), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F3F5), onSecondaryContainer = Color(0xFF07515B),
    tertiary = Color(0xFF8550A3), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1DFFF), onTertiaryContainer = Color(0xFF54306A),
    background = PaperWhite, onBackground = Color(0xFF20233D),
    surface = Color(0xFFFBFAFF), onSurface = Color(0xFF20233D),
    surfaceDim = Color(0xFFDFE0F1), surfaceBright = Color(0xFFFBFAFF),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F0FC),
    surfaceContainer = Color(0xFFECECF9), surfaceContainerHigh = Color(0xFFE6E7F5),
    surfaceContainerHighest = Color(0xFFDDE0F1),
    surfaceVariant = Color(0xFFE9EAF6), onSurfaceVariant = Color(0xFF5D627B),
    outline = Color(0xFF797E98), outlineVariant = Color(0xFFD7D9ED),
    error = Color(0xFFA73555), onError = Color.White,
    errorContainer = Color(0xFFFFE4EA), onErrorContainer = Color(0xFF71233C)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFC1B5FF), onPrimary = Color(0xFF21194C),
    primaryContainer = Color(0xFF302752), onPrimaryContainer = Color(0xFFE5DEFF),
    secondary = NebulaCyan, onSecondary = Color(0xFF07353C),
    secondaryContainer = Color(0xFF153B48), onSecondaryContainer = Color(0xFFAAEFF3),
    tertiary = NebulaPink, onTertiary = Color(0xFF47225D),
    tertiaryContainer = Color(0xFF3F2A51), onTertiaryContainer = Color(0xFFF1DFFF),
    background = NightInk, onBackground = HeroPaper,
    surface = Color(0xFF141A30), onSurface = HeroPaper,
    surfaceDim = Color(0xFF090D1E), surfaceBright = Color(0xFF30364E),
    surfaceContainerLowest = Color(0xFF070A17), surfaceContainerLow = Color(0xFF10162A),
    surfaceContainer = Color(0xFF171D33), surfaceContainerHigh = Color(0xFF20263E),
    surfaceContainerHighest = Color(0xFF2A3049),
    surfaceVariant = Color(0xFF252B45), onSurfaceVariant = Color(0xFFAFB8D1),
    outline = Color(0xFF7E89A8), outlineVariant = Color(0xFF343F60),
    error = Color(0xFFFFAFCA), onError = Color(0xFF60182F),
    errorContainer = Color(0xFF4B2638), onErrorContainer = Color(0xFFFFD9E4)
)
val LocalCosmicDark = staticCompositionLocalOf { false }
@Composable
fun TodayZwdsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: FontScale = FontScale.Standard,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalFontScale provides fontScale, LocalCosmicDark provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = zwdsTypography(fontScale),
            shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(28.dp)),
            content = content
        )
    }
}
