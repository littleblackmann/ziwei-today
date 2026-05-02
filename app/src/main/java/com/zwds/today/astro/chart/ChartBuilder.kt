package com.zwds.today.astro.chart

import com.zwds.today.astro.flow.FlowFortune
import com.zwds.today.astro.ganzhi.Ganzhi
import com.zwds.today.astro.lunar.LunarCalendar
import com.zwds.today.astro.stars.AssistStarPlacer
import com.zwds.today.astro.stars.FourTransformations
import com.zwds.today.astro.stars.MainStarPlacer
import com.zwds.today.astro.stars.MaleficStarPlacer
import com.zwds.today.astro.stars.ZiWeiPlacer
import com.zwds.today.domain.model.ChartResult
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.Gender
import com.zwds.today.domain.model.LunarBirth
import com.zwds.today.domain.model.Palace
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.SolarBirth
import com.zwds.today.domain.model.Star
import com.zwds.today.domain.model.Transformation
import com.zwds.today.domain.model.ZiHourSchool
import java.time.LocalDate

/**
 * 命盤建構器（演算法總入口）
 *
 * 職責：
 *   - 國曆 → 農曆轉換
 *   - 干支系統推算
 *   - 命宮 / 五行局 / 紫微 / 主星 / 輔星 / 煞星 / 四化 全部安星
 *   - 流年 / 流月 / 流日 投射
 *   - 整合成 ChartResult
 */
object ChartBuilder {

    /**
     * 建構完整命盤
     *
     * @param solarBirth 國曆出生
     * @param gender 性別
     * @param targetDate 目標日期（用於流運投射，預設今天）
     * @param ziHourSchool 早子/晚子派系
     */
    fun build(
        solarBirth: SolarBirth,
        gender: Gender,
        targetDate: LocalDate = LocalDate.now(),
        ziHourSchool: ZiHourSchool = ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY
    ): ChartResult {
        // 1. 早晚子時派系修正
        val adjustedSolar = adjustForZiHourSchool(solarBirth, ziHourSchool)

        // 2. 國曆 → 農曆
        val lunar = LunarCalendar.solarToLunar(adjustedSolar)

        // 3. 干支系統
        val ganzhi = Ganzhi.fullGanzhi(lunar)

        // 4. 命宮、身宮
        val lifePalaceBranch = PalaceLocator.lifePalace(lunar.month, ganzhi.hourBranch)
        val bodyPalaceBranch = PalaceLocator.bodyPalace(lunar.month, ganzhi.hourBranch)

        // 5. 五行局
        val lifePalaceStem = FiveElement.lifePalaceStem(ganzhi.yearStem, lifePalaceBranch)
        val fiveElement = FiveElement.classFromGanzhi(lifePalaceStem, lifePalaceBranch)

        // 6. 起紫微 + 安主星
        val ziwei = ZiWeiPlacer.placeZiWei(fiveElement, lunar.day)
        val mainStars = MainStarPlacer.placeMainStars(ziwei)

        // 7. 安輔星
        val assistStars = AssistStarPlacer.place(
            ganzhi.yearStem,
            ganzhi.yearBranch,
            lunar.month,
            ganzhi.hourBranch
        )

        // 8. 安煞星
        val maleficStars = MaleficStarPlacer.place(
            ganzhi.yearStem,
            ganzhi.yearBranch,
            ganzhi.hourBranch,
            gender
        )

        // 9. 四化（生年四化）：依年干，把化標記附到對應主星上
        val transformations = FourTransformations.byStem(ganzhi.yearStem)
        val transformationByStar: Map<Star, Transformation> =
            transformations.associate { (t, s) -> s to t }

        // 10. 整合所有星曜到對應地支，並把四化標記附上
        val starsByBranch = HashMap<EarthlyBranch, MutableList<PlacedStar>>()
        for (branch in EarthlyBranch.entries) starsByBranch[branch] = mutableListOf()

        fun mergeStars(map: Map<EarthlyBranch, List<PlacedStar>>) {
            for ((branch, list) in map) {
                for (placed in list) {
                    val transformation = transformationByStar[placed.star]
                    val merged = if (transformation != null && placed.transformation == null) {
                        placed.copy(transformation = transformation)
                    } else placed
                    starsByBranch.getValue(branch).add(merged)
                }
            }
        }

        mergeStars(mainStars)
        mergeStars(assistStars)
        mergeStars(maleficStars)

        // 11. 排出十二宮
        val palaceLayout = PalaceLocator.layoutPalaces(lifePalaceBranch)
        val palaces = EarthlyBranch.entries.map { branch ->
            Palace(
                branch = branch,
                type = palaceLayout.getValue(branch),
                stars = starsByBranch.getValue(branch).toList()
            )
        }

        // 12. 流運
        val flow = FlowFortune.compute(targetDate)

        return ChartResult(
            birthLunar = lunar,
            gender = gender,
            yearStem = ganzhi.yearStem,
            yearBranch = ganzhi.yearBranch,
            monthStem = ganzhi.monthStem,
            monthBranch = ganzhi.monthBranch,
            dayStem = ganzhi.dayStem,
            dayBranch = ganzhi.dayBranch,
            hourBranch = ganzhi.hourBranch,
            lifePalaceBranch = lifePalaceBranch,
            bodyPalaceBranch = bodyPalaceBranch,
            fiveElement = fiveElement,
            palaces = palaces,
            flowYearPalaceBranch = flow.flowYearBranch,
            flowMonthPalaceBranch = flow.flowMonthBranch,
            flowDayPalaceBranch = flow.flowDayBranch
        )
    }

    /**
     * 早晚子時派系：23:00–24:00 出生的歸屬調整
     *
     * LATE_BELONGS_TO_NEXT_DAY：晚子時歸明日（多數現代派採用，預設）
     * LATE_BELONGS_TO_SAME_DAY：晚子時當作今日子時
     */
    private fun adjustForZiHourSchool(
        solarBirth: SolarBirth,
        school: ZiHourSchool
    ): SolarBirth {
        return when {
            // 23 點屬晚子時
            solarBirth.hour24 == 23 && school == ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY -> {
                val nextDay = solarBirth.toLocalDate().plusDays(1)
                SolarBirth(
                    nextDay.year,
                    nextDay.monthValue,
                    nextDay.dayOfMonth,
                    hour24 = 0  // 改為次日子時起點
                )
            }
            else -> solarBirth
        }
    }
}
