package com.zwds.today.astro.lunar

import com.zwds.today.domain.model.LunarBirth
import com.zwds.today.domain.model.SolarBirth
import java.time.LocalDate

/**
 * 國曆 ↔ 農曆 轉換
 *
 * 範圍：1900-01-31 (農曆 1900 正月初一) ~ 2100 年
 *
 * 演算法概述：
 *   1. 國曆 → 農曆：計算與 epoch (1900-01-31) 的相差天數，
 *      逐年扣除該年農曆總天數，得到「該年第幾天」，再逐月扣除得到月與日。
 *   2. 農曆 → 國曆：累加從 epoch 到目標農曆日期的總天數，再加到 epoch 上。
 */
object LunarCalendar {

    private const val EPOCH_YEAR = LunarTable.EPOCH_YEAR
    private const val EPOCH_MONTH = LunarTable.EPOCH_MONTH
    private const val EPOCH_DAY = LunarTable.EPOCH_DAY

    private val EPOCH_DATE: LocalDate = LocalDate.of(EPOCH_YEAR, EPOCH_MONTH, EPOCH_DAY)

    /** 該農曆年的閏月月份（1..12，0 表示無閏月）*/
    fun leapMonth(lunarYear: Int): Int {
        require(lunarYear in LunarTable.BASE_YEAR..LunarTable.END_YEAR) {
            "lunarYear out of supported range: $lunarYear"
        }
        return LunarTable.LUNAR_INFO[lunarYear - LunarTable.BASE_YEAR] and 0xF
    }

    /** 該農曆年的閏月有幾天（無閏月則回傳 0）*/
    fun leapMonthDays(lunarYear: Int): Int {
        if (leapMonth(lunarYear) == 0) return 0
        val info = LunarTable.LUNAR_INFO[lunarYear - LunarTable.BASE_YEAR]
        return if ((info shr 16) and 0x1 == 1) 30 else 29
    }

    /**
     * 該農曆月的天數（29 或 30）
     *
     * @param lunarYear 農曆年
     * @param lunarMonth 農曆月（1..12）
     * @param isLeap 是否為閏月
     */
    fun monthDays(lunarYear: Int, lunarMonth: Int, isLeap: Boolean = false): Int {
        require(lunarMonth in 1..12)
        if (isLeap) {
            require(leapMonth(lunarYear) == lunarMonth) {
                "year $lunarYear has no leap month $lunarMonth"
            }
            return leapMonthDays(lunarYear)
        }
        val info = LunarTable.LUNAR_INFO[lunarYear - LunarTable.BASE_YEAR]
        // bit 15 = 1 月 ... bit 4 = 12 月，1 = 大月 30 天
        val bit = 16 - lunarMonth
        return if ((info shr bit) and 0x1 == 1) 30 else 29
    }

    /** 該農曆年總天數（含閏月）*/
    fun yearDays(lunarYear: Int): Int {
        var sum = 0
        for (m in 1..12) sum += monthDays(lunarYear, m, false)
        sum += leapMonthDays(lunarYear)
        return sum
    }

    /** 國曆 → 農曆 */
    fun solarToLunar(solar: SolarBirth): LunarBirth {
        val solarDate = solar.toLocalDate()
        require(!solarDate.isBefore(EPOCH_DATE)) {
            "solar date before lunar epoch ($EPOCH_DATE): $solarDate"
        }

        var diff = (solarDate.toEpochDay() - EPOCH_DATE.toEpochDay()).toInt()

        // 1. 找出落在哪一農曆年
        var lunarYear = LunarTable.BASE_YEAR
        while (lunarYear <= LunarTable.END_YEAR) {
            val days = yearDays(lunarYear)
            if (diff < days) break
            diff -= days
            lunarYear++
        }
        require(lunarYear <= LunarTable.END_YEAR) {
            "solar date out of supported lunar range: $solarDate"
        }

        // 2. 在該年內找月份（小心閏月）
        val leap = leapMonth(lunarYear)
        var month = 1
        var isLeapMonth = false
        while (month <= 12) {
            val normalDays = monthDays(lunarYear, month, false)
            if (diff < normalDays) break
            diff -= normalDays

            // 該月之後若有閏月，先扣閏月天數
            if (leap == month) {
                val lDays = leapMonthDays(lunarYear)
                if (diff < lDays) {
                    isLeapMonth = true
                    break
                }
                diff -= lDays
            }
            month++
        }

        val day = diff + 1

        return LunarBirth(
            year = lunarYear,
            month = month,
            day = day,
            isLeapMonth = isLeapMonth,
            hour24 = solar.hour24
        )
    }

    /** 農曆 → 國曆（不含時辰，只回 LocalDate）*/
    fun lunarToSolar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean = false
    ): LocalDate {
        require(lunarYear in LunarTable.BASE_YEAR..LunarTable.END_YEAR)
        require(lunarMonth in 1..12)
        require(lunarDay in 1..monthDays(lunarYear, lunarMonth, isLeapMonth)) {
            "invalid lunar day $lunarDay for year=$lunarYear month=$lunarMonth leap=$isLeapMonth"
        }

        var dayOffset = 0

        // 1. 累加完整年份
        for (y in LunarTable.BASE_YEAR until lunarYear) {
            dayOffset += yearDays(y)
        }

        // 2. 累加目標年內到 lunarMonth 之前的所有月份
        val leap = leapMonth(lunarYear)
        for (m in 1 until lunarMonth) {
            dayOffset += monthDays(lunarYear, m, false)
            if (leap == m) {
                // m 月之後接著閏 m 月
                dayOffset += leapMonthDays(lunarYear)
            }
        }

        // 3. 處理「目標就是閏月」的情況
        if (isLeapMonth) {
            require(leap == lunarMonth) {
                "year $lunarYear has no leap month $lunarMonth"
            }
            // 先加上正規 lunarMonth 月的天數（閏月在正月之後）
            dayOffset += monthDays(lunarYear, lunarMonth, false)
        }

        dayOffset += lunarDay - 1
        return EPOCH_DATE.plusDays(dayOffset.toLong())
    }
}
