package com.zwds.today.domain.model

/**
 * 性別
 *
 * 紫微斗數中性別影響：
 * - 大限順逆排（陽男陰女順、陰男陽女逆）
 * - 部分輔星的安星（如左輔右弼月份計算）
 */
enum class Gender(val cnName: String) {
    MALE("男"),
    FEMALE("女")
}
