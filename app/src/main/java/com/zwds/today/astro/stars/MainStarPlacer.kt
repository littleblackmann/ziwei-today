package com.zwds.today.astro.stars

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.Star

/**
 * 安十四正曜（主星）
 *
 * 紫微落宮後：
 *
 * 紫微系（北斗六星，相對紫微的位置）：
 *   紫微 → 自己
 *   天機 → 紫微逆時針 1（紫微 -1）
 *   太陽 → 紫微逆時針 3（紫微 -3）
 *   武曲 → 紫微逆時針 4（紫微 -4）
 *   天同 → 紫微逆時針 5（紫微 -5）
 *   廉貞 → 紫微逆時針 8（紫微 -8）
 *   （即跳過 -2、-6、-7、-9、-10、-11）
 *
 * 天府系（南斗八星，相對天府的位置）：
 *   天府 → 與紫微對「夾辰戌軸」鏡像 → 公式：天府索引 = (4 - 紫微索引) mod 12
 *           即紫微在寅(2) 時天府在寅(2)、紫微在卯(3) 時天府在丑(1) ... （以辰戌為對稱軸）
 *           實際更標準的算法：天府 = 紫微關於寅申連線的對稱位置
 *           即 天府.index = (4 - 紫微.index + 12) % 12
 *   太陰 → 天府順時針 1（天府 +1）
 *   貪狼 → 天府順時針 2（天府 +2）
 *   巨門 → 天府順時針 3（天府 +3）
 *   天相 → 天府順時針 4（天府 +4）
 *   天梁 → 天府順時針 5（天府 +5）
 *   七殺 → 天府順時針 6（天府 +6）
 *   破軍 → 天府順時針 10（天府 +10）
 *           （跳過 +7, +8, +9）
 *
 * 紫微定位天府的對稱軸驗證：
 *   紫微在 子(0) → 天府在 辰(4)
 *   紫微在 丑(1) → 天府在 卯(3)
 *   紫微在 寅(2) → 天府在 寅(2)
 *   紫微在 卯(3) → 天府在 丑(1)
 *   紫微在 辰(4) → 天府在 子(0)
 *   紫微在 巳(5) → 天府在 亥(11)
 *   紫微在 午(6) → 天府在 戌(10)
 *   紫微在 未(7) → 天府在 酉(9)
 *   紫微在 申(8) → 天府在 申(8)
 *   紫微在 酉(9) → 天府在 未(7)
 *   紫微在 戌(10) → 天府在 午(6)
 *   紫微在 亥(11) → 天府在 巳(5)
 *
 * 對稱軸即 寅申(2,8) 連線：兩星在連線上時相同，否則互為鏡像。
 * 公式：天府.index = (4 - 紫微.index + 12) % 12
 */
object MainStarPlacer {

    /**
     * 計算十四主星各自的地支位置
     *
     * @param ziWeiBranch 紫微星地支（由 ZiWeiPlacer 算出）
     * @return Map<地支, List<PlacedStar>>，每個地支可能有 0 或多顆主星
     */
    fun placeMainStars(ziWeiBranch: EarthlyBranch): Map<EarthlyBranch, List<PlacedStar>> {
        val ziwei = ziWeiBranch.index
        val tianfu = ((4 - ziwei) % 12 + 12) % 12

        // 紫微系六星：相對紫微的位移（負數 = 逆時針）
        val ziWeiSeries = listOf(
            Star.ZI_WEI to 0,
            Star.TIAN_JI to -1,
            Star.TAI_YANG to -3,
            Star.WU_QU to -4,
            Star.TIAN_TONG to -5,
            Star.LIAN_ZHEN to -8
        )

        // 天府系八星：相對天府的位移（正數 = 順時針）
        val tianFuSeries = listOf(
            Star.TIAN_FU to 0,
            Star.TAI_YIN to 1,
            Star.TAN_LANG to 2,
            Star.JU_MEN to 3,
            Star.TIAN_XIANG to 4,
            Star.TIAN_LIANG to 5,
            Star.QI_SHA to 6,
            Star.PO_JUN to 10
        )

        val map = HashMap<EarthlyBranch, MutableList<PlacedStar>>()

        for ((star, offset) in ziWeiSeries) {
            val idx = ((ziwei + offset) % 12 + 12) % 12
            val branch = EarthlyBranch.fromIndex(idx)
            map.getOrPut(branch) { mutableListOf() }.add(PlacedStar(star))
        }

        for ((star, offset) in tianFuSeries) {
            val idx = ((tianfu + offset) % 12 + 12) % 12
            val branch = EarthlyBranch.fromIndex(idx)
            map.getOrPut(branch) { mutableListOf() }.add(PlacedStar(star))
        }

        return map.mapValues { it.value.toList() }
    }
}
