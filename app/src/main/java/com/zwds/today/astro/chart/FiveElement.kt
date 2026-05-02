package com.zwds.today.astro.chart

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.FiveElementClass
import com.zwds.today.domain.model.HeavenlyStem

/**
 * 五行局
 *
 * 紫微斗數依「命宮的天干地支」配出五行局，決定起紫微星的位置與大限起運歲數。
 *
 * 推算流程：
 *   1. 命宮地支已知（PalaceLocator.lifePalace）
 *   2. 命宮天干由「五虎遁年起月」公式決定（年干 → 寅月干，再順推到命宮地支對應月份）
 *      實際上紫微的命宮天干 = 月柱對應到命宮地支位置的天干
 *   3. 命宮天干地支 → 納音五行 → 五行局
 *
 * 納音對照表（六十甲子納音）：
 *   每兩個甲子（10 年）為一組納音五行
 *   甲子乙丑 海中金；丙寅丁卯 爐中火；戊辰己巳 大林木 ...（共 30 組）
 *
 * 五行局：
 *   水 → 水二局
 *   木 → 木三局
 *   金 → 金四局
 *   土 → 土五局
 *   火 → 火六局
 */
object FiveElement {

    /**
     * 命宮天干推算
     *
     * 紫微斗數中，命宮的天干由「五虎遁」決定：
     *   年干 → 寅月（正月）的月干起點，再順推到命宮地支位置。
     *
     * 與 Ganzhi.monthStemBranch 不同的是：
     *   monthStemBranch 計算的是「出生月份」對應的月干月支
     *   這裡計算的是「命宮地支位置」對應的天干（不一定等於出生月）
     *
     * @param yearStem 出生年的天干
     * @param lifePalaceBranch 命宮地支
     */
    fun lifePalaceStem(yearStem: HeavenlyStem, lifePalaceBranch: EarthlyBranch): HeavenlyStem {
        // 五虎遁起例：年干 → 寅月之月干起點
        val yinMonthStemIndex = when (yearStem.index) {
            0, 5 -> 2   // 甲、己 → 丙
            1, 6 -> 4   // 乙、庚 → 戊
            2, 7 -> 6   // 丙、辛 → 庚
            3, 8 -> 8   // 丁、壬 → 壬
            4, 9 -> 0   // 戊、癸 → 甲
            else -> error("unreachable")
        }
        // 從寅(2) 起算，每往後一個地支天干 +1
        val offsetFromYin = (lifePalaceBranch.index - EarthlyBranch.YIN.index + 12) % 12
        return HeavenlyStem.fromIndex(yinMonthStemIndex + offsetFromYin)
    }

    /**
     * 由命宮天干地支取得五行局
     *
     * 採用六十甲子納音五行對照（業界標準）：
     *   六十甲子按序編號 0..59（甲子=0, 乙丑=1, ..., 癸亥=59）
     *   每 5 組（10 年）為一個納音週期，共 6 種五行排列：
     *     甲子乙丑 → 海中金（金）
     *     丙寅丁卯 → 爐中火（火）
     *     戊辰己巳 → 大林木（木）
     *     庚午辛未 → 路旁土（土）
     *     壬申癸酉 → 劍鋒金（金）
     *     甲戌乙亥 → 山頭火（火）
     *     丙子丁丑 → 澗下水（水）
     *     戊寅己卯 → 城頭土（土）
     *     庚辰辛巳 → 白蠟金（金）
     *     壬午癸未 → 楊柳木（木）
     *     甲申乙酉 → 泉中水（水）
     *     丙戌丁亥 → 屋上土（土）
     *     戊子己丑 → 霹靂火（火）
     *     庚寅辛卯 → 松柏木（木）
     *     壬辰癸巳 → 長流水（水）
     *     甲午乙未 → 沙中金（金）
     *     丙申丁酉 → 山下火（火）
     *     戊戌己亥 → 平地木（木）
     *     庚子辛丑 → 壁上土（土）
     *     壬寅癸卯 → 金箔金（金）
     *     甲辰乙巳 → 覆燈火（火）
     *     丙午丁未 → 天河水（水）
     *     戊申己酉 → 大驛土（土）
     *     庚戌辛亥 → 釵釧金（金）
     *     壬子癸丑 → 桑柘木（木）
     *     甲寅乙卯 → 大溪水（水）
     *     丙辰丁巳 → 沙中土（土）
     *     戊午己未 → 天上火（火）
     *     庚申辛酉 → 石榴木（木）
     *     壬戌癸亥 → 大海水（水）
     */
    fun classFromGanzhi(stem: HeavenlyStem, branch: EarthlyBranch): FiveElementClass {
        // 六十甲子序號計算
        // n 滿足：n % 10 == stem.index, n % 12 == branch.index
        // 用中國剩餘定理或直接遍歷 0..59
        val sexagenary = (0 until 60).first {
            it % 10 == stem.index && it % 12 == branch.index
        }
        // 每兩個（10 年週期）為一個納音 → sexagenary / 2 落在 0..29
        val pairIdx = sexagenary / 2
        // 30 組納音的五行序列（依上方對照表）
        val elements = intArrayOf(
            // 0-4 (甲子-壬申)
            0 /*金*/, 1 /*火*/, 2 /*木*/, 3 /*土*/, 0 /*金*/,
            // 5-9 (甲戌-壬午)
            1 /*火*/, 4 /*水*/, 3 /*土*/, 0 /*金*/, 2 /*木*/,
            // 10-14 (甲申-壬辰)
            4 /*水*/, 3 /*土*/, 1 /*火*/, 2 /*木*/, 4 /*水*/,
            // 15-19 (甲午-壬寅)
            0 /*金*/, 1 /*火*/, 2 /*木*/, 3 /*土*/, 0 /*金*/,
            // 20-24 (甲辰-壬子)
            1 /*火*/, 4 /*水*/, 3 /*土*/, 0 /*金*/, 2 /*木*/,
            // 25-29 (甲寅-壬戌)
            4 /*水*/, 3 /*土*/, 1 /*火*/, 2 /*木*/, 4 /*水*/
        )
        return when (elements[pairIdx]) {
            0 -> FiveElementClass.METAL_4
            1 -> FiveElementClass.FIRE_6
            2 -> FiveElementClass.WOOD_3
            3 -> FiveElementClass.EARTH_5
            4 -> FiveElementClass.WATER_2
            else -> error("unreachable")
        }
    }
}
