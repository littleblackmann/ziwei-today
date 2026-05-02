package com.zwds.today.domain.model

import java.time.LocalDate

/**
 * 國曆出生資料（演算法的輸入）
 */
data class SolarBirth(
    val year: Int,
    val month: Int,    // 1..12
    val day: Int,      // 1..31
    val hour24: Int    // 0..23（24 小時制）
) {
    init {
        require(month in 1..12) { "month must be 1..12, got $month" }
        require(day in 1..31) { "day must be 1..31, got $day" }
        require(hour24 in 0..23) { "hour24 must be 0..23, got $hour24" }
    }

    fun toLocalDate(): LocalDate = LocalDate.of(year, month, day)
}

/**
 * 農曆出生資料（演算法的中介格式）
 *
 * 紫微斗數所有計算都基於農曆（陰曆）。
 * 國曆輸入會先透過 [com.zwds.today.astro.lunar.LunarCalendar] 轉成這個格式。
 */
data class LunarBirth(
    val year: Int,             // 農曆年
    val month: Int,            // 農曆月（1..12）
    val day: Int,              // 農曆日（1..30）
    val isLeapMonth: Boolean,  // 是否為閏月
    val hour24: Int            // 24 小時制小時（決定時辰）
) {
    init {
        require(month in 1..12) { "lunar month must be 1..12, got $month" }
        require(day in 1..30) { "lunar day must be 1..30, got $day" }
        require(hour24 in 0..23) { "hour24 must be 0..23, got $hour24" }
    }
}

/**
 * 派系設定：早子時 / 晚子時的歸屬
 *
 * 23:00–24:00 出生屬「晚子時」，這個時段的命盤歸屬有兩派：
 * - LATE_BELONGS_TO_SAME_DAY：晚子時當作今日子時
 * - LATE_BELONGS_TO_NEXT_DAY：晚子時歸明日（多數現代派採用）
 *
 * 設定頁讓使用者選擇，預設 LATE_BELONGS_TO_NEXT_DAY。
 */
enum class ZiHourSchool {
    LATE_BELONGS_TO_SAME_DAY,
    LATE_BELONGS_TO_NEXT_DAY
}
