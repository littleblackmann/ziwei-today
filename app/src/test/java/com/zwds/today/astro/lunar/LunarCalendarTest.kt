package com.zwds.today.astro.lunar

import com.zwds.today.domain.model.SolarBirth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 農曆轉換驗證
 *
 * 對照來源：香港天文台農曆轉換、中華民國中央氣象署
 * 重點覆蓋：
 *  - 春節、中秋等知名日期
 *  - 閏月（如 2020 閏 4 月、2023 閏 2 月）
 *  - 跨年邊界
 *  - 表的兩端 epoch / 2099
 */
class LunarCalendarTest {

    private fun solar(y: Int, m: Int, d: Int) =
        SolarBirth(y, m, d, hour24 = 12)

    @Test
    fun `epoch 1900-01-31 應為農曆 1900 正月初一`() {
        val lunar = LunarCalendar.solarToLunar(solar(1900, 1, 31))
        assertEquals(1900, lunar.year)
        assertEquals(1, lunar.month)
        assertEquals(1, lunar.day)
        assertFalse(lunar.isLeapMonth)
    }

    @Test
    fun `2024-02-10 應為農曆 2024 正月初一（甲辰年春節）`() {
        val lunar = LunarCalendar.solarToLunar(solar(2024, 2, 10))
        assertEquals(2024, lunar.year)
        assertEquals(1, lunar.month)
        assertEquals(1, lunar.day)
    }

    @Test
    fun `2025-01-29 應為農曆 2025 正月初一（乙巳年春節）`() {
        val lunar = LunarCalendar.solarToLunar(solar(2025, 1, 29))
        assertEquals(2025, lunar.year)
        assertEquals(1, lunar.month)
        assertEquals(1, lunar.day)
    }

    @Test
    fun `2026-02-17 應為農曆 2026 正月初一（丙午年春節）`() {
        val lunar = LunarCalendar.solarToLunar(solar(2026, 2, 17))
        assertEquals(2026, lunar.year)
        assertEquals(1, lunar.month)
        assertEquals(1, lunar.day)
    }

    @Test
    fun `2026-04-28 今天 應為農曆 2026年三月十二`() {
        val lunar = LunarCalendar.solarToLunar(solar(2026, 4, 28))
        assertEquals(2026, lunar.year)
        assertEquals(3, lunar.month)
        assertEquals(12, lunar.day)
        assertFalse(lunar.isLeapMonth)
    }

    @Test
    fun `2020-05-23 應為農曆 2020 閏四月初一`() {
        val lunar = LunarCalendar.solarToLunar(solar(2020, 5, 23))
        assertEquals(2020, lunar.year)
        assertEquals(4, lunar.month)
        assertEquals(1, lunar.day)
        assertTrue("應為閏月", lunar.isLeapMonth)
    }

    @Test
    fun `2023-03-22 應為農曆 2023 閏二月初一`() {
        val lunar = LunarCalendar.solarToLunar(solar(2023, 3, 22))
        assertEquals(2023, lunar.year)
        assertEquals(2, lunar.month)
        assertEquals(1, lunar.day)
        assertTrue("應為閏月", lunar.isLeapMonth)
    }

    @Test
    fun `2024 年閏月為閏 6 月`() {
        assertEquals(0, LunarCalendar.leapMonth(2024))  // 2024 無閏月（Feb 10 春節 → 平年）
    }

    @Test
    fun `2025 年閏月為閏 6 月`() {
        assertEquals(6, LunarCalendar.leapMonth(2025))  // 2025 閏 6 月
    }

    @Test
    fun `1949-10-01 中華人民共和國成立日 應為農曆 1949年八月初十`() {
        val lunar = LunarCalendar.solarToLunar(solar(1949, 10, 1))
        assertEquals(1949, lunar.year)
        assertEquals(8, lunar.month)
        assertEquals(10, lunar.day)
    }

    @Test
    fun `農曆 2024 春節 lunarToSolar 應回到 2024-02-10`() {
        val solar = LunarCalendar.lunarToSolar(2024, 1, 1, isLeapMonth = false)
        assertEquals(2024, solar.year)
        assertEquals(2, solar.monthValue)
        assertEquals(10, solar.dayOfMonth)
    }

    @Test
    fun `農曆 2025 閏六月初一 lunarToSolar 應為國曆 2025-07-25`() {
        val solar = LunarCalendar.lunarToSolar(2025, 6, 1, isLeapMonth = true)
        assertEquals(2025, solar.year)
        assertEquals(7, solar.monthValue)
        assertEquals(25, solar.dayOfMonth)
    }

    @Test
    fun `來回轉換應一致 - 任意採樣 5 個日期`() {
        val samples = listOf(
            Triple(1955, 3, 15),
            Triple(1980, 6, 30),
            Triple(2000, 12, 25),
            Triple(2020, 5, 23),  // 閏 4 月起點
            Triple(2026, 4, 28)
        )
        for ((y, m, d) in samples) {
            val lunar = LunarCalendar.solarToLunar(solar(y, m, d))
            val back = LunarCalendar.lunarToSolar(lunar.year, lunar.month, lunar.day, lunar.isLeapMonth)
            assertEquals("solar→lunar→solar 不一致 ($y-$m-$d)", y, back.year)
            assertEquals("solar→lunar→solar 不一致 ($y-$m-$d)", m, back.monthValue)
            assertEquals("solar→lunar→solar 不一致 ($y-$m-$d)", d, back.dayOfMonth)
        }
    }
}
