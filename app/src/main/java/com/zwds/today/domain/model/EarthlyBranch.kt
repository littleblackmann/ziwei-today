package com.zwds.today.domain.model

/**
 * 十二地支
 *
 * 索引從 0 (子) 到 11 (亥)。
 * 紫微斗數中：
 * - 地支同時是「時辰」與「宮位」的座標
 * - 命盤是十二宮的環形結構，索引運算需用模 12 處理邊界
 *
 * 時辰對應（24 小時制）：
 *   子 23:00-01:00 (注意：派系不同，早子/晚子歸屬有別)
 *   丑 01:00-03:00
 *   寅 03:00-05:00
 *   卯 05:00-07:00
 *   辰 07:00-09:00
 *   巳 09:00-11:00
 *   午 11:00-13:00
 *   未 13:00-15:00
 *   申 15:00-17:00
 *   酉 17:00-19:00
 *   戌 19:00-21:00
 *   亥 21:00-23:00
 */
enum class EarthlyBranch(val index: Int, val cnName: String) {
    ZI(0, "子"),
    CHOU(1, "丑"),
    YIN(2, "寅"),
    MAO(3, "卯"),
    CHEN(4, "辰"),
    SI(5, "巳"),
    WU(6, "午"),
    WEI(7, "未"),
    SHEN(8, "申"),
    YOU(9, "酉"),
    XU(10, "戌"),
    HAI(11, "亥");

    /** 環形位移：正數順時針，負數逆時針 */
    fun shift(steps: Int): EarthlyBranch = fromIndex(this.index + steps)

    /** 陽支：子寅辰午申戌；陰支：丑卯巳未酉亥 */
    val isYang: Boolean get() = index % 2 == 0

    /** 三合（用於四化、流月計算）：申子辰、亥卯未、寅午戌、巳酉丑 */
    val sanHe: Triple<EarthlyBranch, EarthlyBranch, EarthlyBranch>
        get() = when (index % 4) {
            0 -> Triple(SHEN, ZI, CHEN)
            1 -> Triple(SI, YOU, CHOU)
            2 -> Triple(YIN, WU, XU)
            3 -> Triple(HAI, MAO, WEI)
            else -> error("unreachable")
        }

    /**
     * 對宮（六沖位置）：子午、丑未、寅申、卯酉、辰戌、巳亥
     * 紫微斗數中常用「對宮的力量」概念
     */
    val opposite: EarthlyBranch get() = fromIndex(index + 6)

    companion object {
        fun fromIndex(index: Int): EarthlyBranch {
            val n = ((index % 12) + 12) % 12
            return entries[n]
        }

        /**
         * 從 24 小時制小時值（含小數）換算時辰
         * 子時 23:00–01:00，依 [lateZiBelongsToNextDay] 派系決定歸屬
         *
         * @param hour24 0-23.99 的小時數
         * @return 對應的地支時辰
         */
        fun fromHour24(hour24: Int): EarthlyBranch {
            require(hour24 in 0..23) { "hour24 must be in 0..23, got $hour24" }
            // 23、0 → 子；1、2 → 丑；以此類推
            val idx = ((hour24 + 1) / 2) % 12
            return fromIndex(idx)
        }
    }
}
