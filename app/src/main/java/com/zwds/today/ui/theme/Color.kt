package com.zwds.today.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// 今日紫微 — 配色系統
//
// 設計理念：
// - 暖米白底（接近宣紙色），對比柔和不刺眼，適合長者長時間閱讀
// - 主色用「丹砂紅」，符合傳統命理意象，又能在白底上提供高對比
// - 吉凶用紅藍綠三色區分，符合一般人對「紅吉藍凶綠平」的直覺
// - 暗色模式採深藍底（接近古籍藍底）+ 米白字
// ============================================================

// 淺色（預設）
val PaperWhite = Color(0xFFFFF8E7)        // 主背景：宣紙米白
val PaperWhiteVariant = Color(0xFFF3EBD3) // 卡片陰影邊
val InkBlack = Color(0xFF2A1E15)          // 主要文字：墨黑帶暖
val InkBlackSoft = Color(0xFF5A4A3D)      // 次要文字
val InkBlackHint = Color(0xFF8A7A6D)      // 提示文字

val CinnabarRed = Color(0xFF8C2A2A)       // 主色：丹砂紅
val CinnabarRedDark = Color(0xFF6B1F1F)
val CinnabarRedLight = Color(0xFFB85050)

// 吉凶色彩
val FortuneVeryGood = Color(0xFFC02828)   // 大吉：丹紅
val FortuneGood = Color(0xFFD46A1A)       // 中吉：金橘
val FortuneNeutral = Color(0xFF6B6051)    // 平：墨灰
val FortuneCaution = Color(0xFF1F5E8C)    // 小凶：靛藍
val FortuneBad = Color(0xFF1F3A8C)        // 凶：深藍

// 暗色
val NightInk = Color(0xFF1A1410)
val NightInkVariant = Color(0xFF2A211B)
val NightPaper = Color(0xFFF0E6D0)
val NightPaperSoft = Color(0xFFB8A998)
