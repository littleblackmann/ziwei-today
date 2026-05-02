package com.zwds.today.domain.model

import java.time.LocalDate

/**
 * 使用者可詢問的運勢主題。
 *
 * 每個主題對應紫微斗數十二宮中的一個宮位，
 * 計算流日運勢時會看「流日 + 該宮位」的星曜駐守。
 */
enum class FortuneTopic(
    val cnName: String,
    val palaceType: PalaceType,
    val emoji: String,
    val description: String
) {
    OVERVIEW("整體運勢", PalaceType.LIFE, "🌅", "今天綜合的運勢概況"),
    HEALTH("健康", PalaceType.HEALTH, "🌿", "身體狀況、保養"),
    WEALTH("財運", PalaceType.WEALTH, "💰", "錢財、收支"),
    CAREER("事業工作", PalaceType.CAREER, "💼", "工作、職場、進修"),
    LOVE("感情", PalaceType.SPOUSE, "💕", "伴侶、感情"),
    FRIENDS("人際", PalaceType.FRIENDS, "🤝", "朋友、貴人"),
    TRAVEL("出行", PalaceType.TRAVEL, "🚪", "外出、變動"),
    MOOD("心情", PalaceType.FORTUNE, "✨", "心境、福氣");

    companion object {
        fun fromName(name: String?): FortuneTopic =
            entries.firstOrNull { it.name == name } ?: OVERVIEW
    }
}

/**
 * 針對某個主題的今日運勢結果。
 */
data class TopicFortune(
    val familyMemberId: Long,
    val familyMemberName: String,
    val date: LocalDate,
    val topic: FortuneTopic,
    val flowPalaceBranch: EarthlyBranch,
    val flowPalaceStars: List<PlacedStar>,

    val title: String,
    val level: FortuneLevel,
    val focusSummary: String,
    val advice: String,
    val tags: List<String> = emptyList()
)

/**
 * 運勢等級
 */
enum class FortuneLevel(val cnName: String, val emoji: String) {
    VERY_GOOD("大吉", "🌞"),
    GOOD("中吉", "🌿"),
    NEUTRAL("平", "✨"),
    CAUTION("小凶", "🌧"),
    BAD("凶", "⛈️")
}
