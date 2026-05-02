package com.zwds.today.domain.model

/**
 * 十天干
 *
 * 索引從 0 (甲) 到 9 (癸)。
 * 紫微斗數中所有跟「天干」相關的計算都圍繞這個列舉：
 * - 年干決定生年四化飛星
 * - 月干決定流月四化
 * - 日干決定流日宮位
 *
 * 五行對應（用於納音、五行局推算）：
 * 甲乙木、丙丁火、戊己土、庚辛金、壬癸水
 *
 * 陰陽：奇數位（甲丙戊庚壬）為陽，偶數位（乙丁己辛癸）為陰
 */
enum class HeavenlyStem(val index: Int, val cnName: String) {
    JIA(0, "甲"),
    YI(1, "乙"),
    BING(2, "丙"),
    DING(3, "丁"),
    WU(4, "戊"),
    JI(5, "己"),
    GENG(6, "庚"),
    XIN(7, "辛"),
    REN(8, "壬"),
    GUI(9, "癸");

    /** 陽干：甲丙戊庚壬 */
    val isYang: Boolean get() = index % 2 == 0

    /** 五行屬性 */
    val element: WuXing
        get() = when (index / 2) {
            0 -> WuXing.WOOD   // 甲乙
            1 -> WuXing.FIRE   // 丙丁
            2 -> WuXing.EARTH  // 戊己
            3 -> WuXing.METAL  // 庚辛
            4 -> WuXing.WATER  // 壬癸
            else -> error("unreachable")
        }

    companion object {
        fun fromIndex(index: Int): HeavenlyStem {
            val n = ((index % 10) + 10) % 10
            return entries[n]
        }
    }
}

enum class WuXing(val cnName: String) {
    WOOD("木"),
    FIRE("火"),
    EARTH("土"),
    METAL("金"),
    WATER("水")
}
