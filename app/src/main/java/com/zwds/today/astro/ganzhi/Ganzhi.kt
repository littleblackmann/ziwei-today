package com.zwds.today.astro.ganzhi

import com.zwds.today.astro.lunar.LunarCalendar
import com.zwds.today.astro.lunar.LunarTable
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.HeavenlyStem
import com.zwds.today.domain.model.LunarBirth
import java.time.LocalDate

/**
 * 干支推算
 *
 * 紫微斗數中所有與「天干地支」相關的計算。
 * 包含：
 *   - 年干支（用於起命局、生年四化）
 *   - 月干支（用於流月四化）
 *   - 日干支（用於流日宮位）
 *   - 時干（用於某些輔星安星）
 */
object Ganzhi {

    /**
     * 農曆年的年干支
     *
     * 農曆 1984 年（甲子年）為干支六十甲子的「甲子」起點。
     * 公式：(year - 1984) mod 60 = 干支序號
     */
    fun yearStemBranch(lunarYear: Int): Pair<HeavenlyStem, EarthlyBranch> {
        val offset = (lunarYear - 1984) % 60
        val n = (offset + 60) % 60
        val stem = HeavenlyStem.fromIndex(n % 10)
        val branch = EarthlyBranch.fromIndex(n % 12)
        return stem to branch
    }

    /**
     * 月干支（紫微斗數派系：以農曆月為主，不考慮節氣）
     *
     * 月支固定：正月寅、二月卯、三月辰 ...（即農曆月份 + 1 的地支）
     * 月干由「五虎遁」公式決定：
     *   甲己之年丙作首
     *   乙庚之年戊為頭
     *   丙辛之年從庚起
     *   丁壬壬位順流行
     *   戊癸甲寅惟正月
     *
     * 即：年干 → 正月（寅月）的月干，再順推。
     */
    fun monthStemBranch(yearStem: HeavenlyStem, lunarMonth: Int): Pair<HeavenlyStem, EarthlyBranch> {
        require(lunarMonth in 1..12) { "lunarMonth must be 1..12" }
        // 月支：正月=寅(2)，二月=卯(3) ... 十二月=丑(1)
        val branch = EarthlyBranch.fromIndex(lunarMonth + 1)
        // 五虎遁起例（年干 → 寅月之月干起點）
        // 甲己年 → 丙寅；乙庚年 → 戊寅；丙辛年 → 庚寅；丁壬年 → 壬寅；戊癸年 → 甲寅
        val yinMonthStemIndex = when (yearStem.index) {
            0, 5 -> 2   // 甲、己 → 丙
            1, 6 -> 4   // 乙、庚 → 戊
            2, 7 -> 6   // 丙、辛 → 庚
            3, 8 -> 8   // 丁、壬 → 壬
            4, 9 -> 0   // 戊、癸 → 甲
            else -> error("unreachable")
        }
        // 從寅月起算，每個月天干 +1
        val stem = HeavenlyStem.fromIndex(yinMonthStemIndex + (lunarMonth - 1))
        return stem to branch
    }

    /**
     * 日干支
     *
     * 以某一已知干支日為基準，按與該基準的相差天數推算。
     * 基準：1900-01-31（農曆 1900 正月初一）為「甲戌」日
     *   驗證：1900-01-31 是星期三，干支甲戌（公認）
     *
     * 任意公曆日的干支 = (距 1900-01-31 的天數 + 甲戌索引) mod 60
     *   甲戌索引：天干甲=0，地支戌=10 → 甲戌在六十甲子中為第 10 位
     */
    fun dayStemBranch(solarDate: LocalDate): Pair<HeavenlyStem, EarthlyBranch> {
        val epoch = LocalDate.of(LunarTable.EPOCH_YEAR, LunarTable.EPOCH_MONTH, LunarTable.EPOCH_DAY)
        val diff = (solarDate.toEpochDay() - epoch.toEpochDay()).toInt()

        // 1900-01-31 為甲戌日 → 六十甲子中第 10 位（索引 10）
        val baseSexagenary = 10
        val n = ((diff + baseSexagenary) % 60 + 60) % 60
        val stem = HeavenlyStem.fromIndex(n % 10)
        val branch = EarthlyBranch.fromIndex(n % 12)
        return stem to branch
    }

    /**
     * 時干（時辰的天干）
     *
     * 時支由出生小時直接決定（EarthlyBranch.fromHour24）。
     * 時干由「五鼠遁」公式決定（日干起例）：
     *   甲己還加甲，乙庚丙作初
     *   丙辛從戊起，丁壬庚子居
     *   戊癸何方發，壬子是真途
     *
     * 即：日干 → 子時的時干起點，再順推到目標時辰。
     */
    fun hourStem(dayStem: HeavenlyStem, hourBranch: EarthlyBranch): HeavenlyStem {
        val ziHourStemIndex = when (dayStem.index) {
            0, 5 -> 0   // 甲、己 → 甲
            1, 6 -> 2   // 乙、庚 → 丙
            2, 7 -> 4   // 丙、辛 → 戊
            3, 8 -> 6   // 丁、壬 → 庚
            4, 9 -> 8   // 戊、癸 → 壬
            else -> error("unreachable")
        }
        return HeavenlyStem.fromIndex(ziHourStemIndex + hourBranch.index)
    }

    /**
     * 完整四柱（生辰八字的紫微版本）
     *
     * @param lunar 農曆出生資料
     * @return GanzhiSet 包含年/月/日/時 的天干地支
     */
    fun fullGanzhi(lunar: LunarBirth): GanzhiSet {
        val (yStem, yBranch) = yearStemBranch(lunar.year)
        val (mStem, mBranch) = monthStemBranch(yStem, lunar.month)

        val solarDate = LunarCalendar.lunarToSolar(
            lunar.year,
            lunar.month,
            lunar.day,
            lunar.isLeapMonth
        )
        val (dStem, dBranch) = dayStemBranch(solarDate)
        val hBranch = EarthlyBranch.fromHour24(lunar.hour24)
        val hStem = hourStem(dStem, hBranch)

        return GanzhiSet(
            yearStem = yStem, yearBranch = yBranch,
            monthStem = mStem, monthBranch = mBranch,
            dayStem = dStem, dayBranch = dBranch,
            hourStem = hStem, hourBranch = hBranch
        )
    }
}

data class GanzhiSet(
    val yearStem: HeavenlyStem,
    val yearBranch: EarthlyBranch,
    val monthStem: HeavenlyStem,
    val monthBranch: EarthlyBranch,
    val dayStem: HeavenlyStem,
    val dayBranch: EarthlyBranch,
    val hourStem: HeavenlyStem,
    val hourBranch: EarthlyBranch
)
