package com.zwds.today.domain.model

/**
 * 十二宮位
 *
 * 紫微斗數中的十二宮從「命宮」開始逆時針排列：
 *   命 → 兄弟 → 夫妻 → 子女 → 財帛 → 疾厄 → 遷移 → 交友（奴僕）→ 官祿（事業）→ 田宅 → 福德 → 父母
 *
 * 每個宮位代表一個人生領域。
 * 流日宮位則是「今天的命宮」，為本專案的核心查詢目標。
 */
enum class PalaceType(val cnName: String, val flowName: String) {
    LIFE("命宮", "流日命宮"),
    SIBLINGS("兄弟", "流日兄弟"),
    SPOUSE("夫妻", "流日夫妻"),
    CHILDREN("子女", "流日子女"),
    WEALTH("財帛", "流日財帛"),
    HEALTH("疾厄", "流日疾厄"),
    TRAVEL("遷移", "流日遷移"),
    FRIENDS("交友", "流日交友"),
    CAREER("官祿", "流日官祿"),
    PROPERTY("田宅", "流日田宅"),
    FORTUNE("福德", "流日福德"),
    PARENTS("父母", "流日父母");

    companion object {
        /** 由命宮起算，逆時針 N 步抵達的宮位類型（紫微的傳統排列）*/
        fun fromOffsetFromLife(offset: Int): PalaceType {
            val n = ((offset % 12) + 12) % 12
            return entries[n]
        }
    }
}

/**
 * 一個宮位的完整資料：地支位置 + 宮類型 + 駐守星曜
 */
data class Palace(
    val branch: EarthlyBranch,
    val type: PalaceType,
    val stars: List<PlacedStar>
) {
    /** 主星清單（不含輔煞與四化）*/
    val mainStars: List<PlacedStar>
        get() = stars.filter { it.star.category == StarCategory.MAIN }

    /** 是否有四化飛入此宮 */
    val hasTransformation: Boolean
        get() = stars.any { it.transformation != null }

    /** 取出此宮所有四化標記 */
    val transformations: List<Transformation>
        get() = stars.mapNotNull { it.transformation }
}
