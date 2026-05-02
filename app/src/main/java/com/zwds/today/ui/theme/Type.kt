package com.zwds.today.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============================================================
// 字體系統 — 三級可變字體
//
// 長者首選字體大小可選：標準 / 大 / 特大
// 比例：1.0x / 1.25x / 1.5x
// 全部使用 sp，跟隨系統字體縮放
// ============================================================

enum class FontScale(val multiplier: Float, val displayName: String) {
    Standard(1.0f, "標準"),
    Large(1.25f, "大"),
    ExtraLarge(1.5f, "特大");

    companion object {
        fun fromName(name: String?): FontScale =
            entries.firstOrNull { it.name == name } ?: Standard
    }
}

val LocalFontScale = staticCompositionLocalOf { FontScale.Standard }

@Composable
fun zwdsTypography(scale: FontScale = FontScale.Standard): Typography {
    val m = scale.multiplier

    fun style(size: Float, weight: FontWeight, lineHeightRatio: Float = 1.5f) = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = weight,
        fontSize = (size * m).sp,
        lineHeight = (size * m * lineHeightRatio).sp
    )

    return Typography(
        // 大標題：吉凶結果
        displayLarge = style(40f, FontWeight.Black, 1.3f),
        displayMedium = style(32f, FontWeight.Bold, 1.3f),
        displaySmall = style(28f, FontWeight.Bold, 1.3f),

        // 卡片標題
        headlineLarge = style(28f, FontWeight.Bold, 1.4f),
        headlineMedium = style(24f, FontWeight.Bold, 1.4f),
        headlineSmall = style(22f, FontWeight.Medium, 1.4f),

        // 內文（白話文建議的主要載體）
        titleLarge = style(22f, FontWeight.Medium, 1.5f),
        titleMedium = style(20f, FontWeight.Medium, 1.5f),
        titleSmall = style(18f, FontWeight.Medium, 1.5f),

        // 按鈕、標籤
        labelLarge = style(18f, FontWeight.Medium, 1.4f),
        labelMedium = style(16f, FontWeight.Medium, 1.4f),
        labelSmall = style(14f, FontWeight.Normal, 1.4f),

        // 內文段落（建議文，最重要）
        bodyLarge = style(20f, FontWeight.Normal, 1.6f),
        bodyMedium = style(18f, FontWeight.Normal, 1.6f),
        bodySmall = style(16f, FontWeight.Normal, 1.6f),
    )
}
