package com.zwds.today.domain.model

/**
 * 命盤完整結果
 *
 * ChartBuilder 的輸出，包含本命盤 + 流運投射。
 */
data class ChartResult(
    // ── 基本資料 ──
    val birthLunar: LunarBirth,
    val gender: Gender,

    // ── 干支系統 ──
    val yearStem: HeavenlyStem,
    val yearBranch: EarthlyBranch,
    val monthStem: HeavenlyStem,
    val monthBranch: EarthlyBranch,
    val dayStem: HeavenlyStem,
    val dayBranch: EarthlyBranch,
    val hourBranch: EarthlyBranch,

    // ── 本命盤 ──
    val lifePalaceBranch: EarthlyBranch,
    val bodyPalaceBranch: EarthlyBranch,
    val fiveElement: FiveElementClass,
    val palaces: List<Palace>,    // 12 個宮位（含星曜）

    // ── 流年/流月/流日 ──
    val flowYearPalaceBranch: EarthlyBranch,    // 今年流年命宮地支
    val flowMonthPalaceBranch: EarthlyBranch,   // 本月流月命宮地支
    val flowDayPalaceBranch: EarthlyBranch,     // 今日流日命宮地支
) {
    /** 取得指定地支位置的宮位 */
    fun palaceAt(branch: EarthlyBranch): Palace =
        palaces.first { it.branch == branch }

    /** 流日命宮的星曜清單（白話文字典查詢的主要來源）*/
    val flowDayStars: List<PlacedStar>
        get() = palaceAt(flowDayPalaceBranch).stars
}

/**
 * 五行局
 *
 * 紫微斗數依命宮天干地支配出的「五行局」，決定起紫微的位置。
 * 局數 = 局的天數，水二、木三、金四、土五、火六。
 */
enum class FiveElementClass(val cnName: String, val days: Int) {
    WATER_2(" 水二局", 2),
    WOOD_3("木三局", 3),
    METAL_4("金四局", 4),
    EARTH_5("土五局", 5),
    FIRE_6("火六局", 6)
}
