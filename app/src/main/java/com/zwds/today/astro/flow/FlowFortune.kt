package com.zwds.today.astro.flow

import com.zwds.today.astro.ganzhi.Ganzhi
import com.zwds.today.astro.lunar.LunarCalendar
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.SolarBirth
import java.time.LocalDate

/**
 * 流年、流月、流日宮位推算
 *
 * V1.0 採通用紫微流運法（多數現代派系一致）：
 *
 * 流年命宮：
 *   即流年的年支位置。例如 2026 年（丙午）→ 午宮為流年命宮。
 *
 * 流月命宮：
 *   從流年命宮起算正月，順時針每月 +1。
 *   即 流月命宮地支 = 流年年支 + (流月月份 - 1)
 *   注意：本派採「流年正月即流年命宮」，而非「流年正月起寅」。
 *
 * 流日命宮：
 *   從流月命宮起算初一，順時針每日 +1。
 *   即 流日命宮地支 = 流月命宮 + (農曆日 - 1)
 *
 * 為何不採「寅起正月再順月逆時辰」：
 *   那是「本命命宮」的演算法。流年/流月/流日是太歲投射，
 *   起點為流年年支本身，而非寅。本系統 V1 採此通用法。
 *
 * 早晚子時：
 *   23 點（晚子時）的歸屬交由 ChartBuilder.adjustForZiHourSchool() 在輸入端處理，
 *   FlowFortune 只看「目標日期」（已是調整後的日期）。
 */
object FlowFortune {

    data class FlowResult(
        val flowYearBranch: EarthlyBranch,
        val flowMonthBranch: EarthlyBranch,
        val flowDayBranch: EarthlyBranch,
        /** 目標日期所對應的農曆，方便 UI / 字典查詢時參考 */
        val targetLunarYear: Int,
        val targetLunarMonth: Int,
        val targetLunarDay: Int,
        val targetLunarIsLeap: Boolean
    )

    /**
     * 計算「指定國曆日期」當天的流年/流月/流日命宮地支
     *
     * @param targetDate 目標日期（國曆）
     * @return 三個地支位置 + 對應的農曆日期
     */
    fun compute(targetDate: LocalDate): FlowResult {
        // 1. 國曆 → 農曆（用 hour24 = 12 即中午，避免子時邊界問題）
        val targetLunar = LunarCalendar.solarToLunar(
            SolarBirth(
                year = targetDate.year,
                month = targetDate.monthValue,
                day = targetDate.dayOfMonth,
                hour24 = 12
            )
        )

        // 2. 流年命宮 = 流年的年支
        val (_, yearBranch) = Ganzhi.yearStemBranch(targetLunar.year)

        // 3. 流月命宮 = 流年命宮 + (月 - 1)
        //    注意：閏月仍視為對應的「正月份」（V1 簡化處理；嚴謹版需考慮閏月歸屬）
        val flowMonth = EarthlyBranch.fromIndex(yearBranch.index + (targetLunar.month - 1))

        // 4. 流日命宮 = 流月命宮 + (日 - 1)
        val flowDay = EarthlyBranch.fromIndex(flowMonth.index + (targetLunar.day - 1))

        return FlowResult(
            flowYearBranch = yearBranch,
            flowMonthBranch = flowMonth,
            flowDayBranch = flowDay,
            targetLunarYear = targetLunar.year,
            targetLunarMonth = targetLunar.month,
            targetLunarDay = targetLunar.day,
            targetLunarIsLeap = targetLunar.isLeapMonth
        )
    }
}
