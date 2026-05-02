package com.zwds.today.astro.chart

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.LunarBirth
import com.zwds.today.domain.model.PalaceType

/**
 * 命宮、身宮定位
 */
object PalaceLocator {

    /**
     * 安命宮
     *
     * 古訣：「寅起正月，順數至生月，逆數生時為命宮」
     *
     * 實作：
     *   起點：寅宮（索引 2）
     *   順數至生月：+ (lunarMonth - 1)（因正月本身落在寅，不需位移）
     *   逆數生時：- hourBranchIndex（子時 0 不退、丑時 1 退一格 ...）
     *
     * @param lunarMonth 出生時的農曆月（1..12）
     * @param hourBranch 出生時辰的地支
     * @return 命宮所在地支
     */
    fun lifePalace(lunarMonth: Int, hourBranch: EarthlyBranch): EarthlyBranch {
        require(lunarMonth in 1..12)
        val startIdx = EarthlyBranch.YIN.index
        val finalIdx = startIdx + (lunarMonth - 1) - hourBranch.index
        return EarthlyBranch.fromIndex(finalIdx)
    }

    /**
     * 安身宮
     *
     * 古訣：「寅起正月，順數至生月，順數生時為身宮」
     *
     * 與命宮的差別在於「生時」是順數而非逆數。
     */
    fun bodyPalace(lunarMonth: Int, hourBranch: EarthlyBranch): EarthlyBranch {
        require(lunarMonth in 1..12)
        val startIdx = EarthlyBranch.YIN.index
        val finalIdx = startIdx + (lunarMonth - 1) + hourBranch.index
        return EarthlyBranch.fromIndex(finalIdx)
    }

    /**
     * 從命宮逆時針排出十二宮
     *
     * 排列順序（逆時針）：
     *   命 → 兄弟 → 夫妻 → 子女 → 財帛 → 疾厄 → 遷移 → 交友 → 官祿 → 田宅 → 福德 → 父母
     *
     * @return Map<地支, 宮類型>
     */
    fun layoutPalaces(lifePalaceBranch: EarthlyBranch): Map<EarthlyBranch, PalaceType> {
        val map = HashMap<EarthlyBranch, PalaceType>()
        for (offset in 0 until 12) {
            // 逆時針排列：每偏移 1 步，地支索引 -1
            val branch = EarthlyBranch.fromIndex(lifePalaceBranch.index - offset)
            val type = PalaceType.fromOffsetFromLife(offset)
            map[branch] = type
        }
        return map
    }
}
