package com.zwds.today.domain.model

/**
 * 星曜分類
 *
 * 紫微斗數的星曜系統共數十顆，本專案 V1.0 實作以下：
 * - 14 主星（紫微系 6 + 天府系 8）
 * - 6 輔星（左輔右弼、文昌文曲、天魁天鉞）
 * - 2 祿馬（祿存、天馬）
 * - 6 煞星（擎羊陀羅、火星鈴星、地空地劫）
 * - 4 化曜（化祿、化權、化科、化忌）
 *
 * 共 32 顆，足以判定吉凶與寫白話文建議。
 * 其餘長生十二神、博士十二神、流年將前等留待 V2.0+。
 */
enum class StarCategory {
    MAIN,           // 主星（十四正曜）
    ASSIST,         // 輔星（六吉星 + 祿存天馬）
    MALEFIC,        // 煞星（六煞）
    TRANSFORMATION  // 四化
}

/**
 * 星曜
 *
 * cnName 為傳統名稱，本系統以此為唯一識別字串去字典查表。
 */
enum class Star(
    val cnName: String,
    val category: StarCategory
) {
    // ── 紫微系六星（北斗主星）──
    ZI_WEI("紫微", StarCategory.MAIN),
    TIAN_JI("天機", StarCategory.MAIN),
    TAI_YANG("太陽", StarCategory.MAIN),
    WU_QU("武曲", StarCategory.MAIN),
    TIAN_TONG("天同", StarCategory.MAIN),
    LIAN_ZHEN("廉貞", StarCategory.MAIN),

    // ── 天府系八星（南斗主星）──
    TIAN_FU("天府", StarCategory.MAIN),
    TAI_YIN("太陰", StarCategory.MAIN),
    TAN_LANG("貪狼", StarCategory.MAIN),
    JU_MEN("巨門", StarCategory.MAIN),
    TIAN_XIANG("天相", StarCategory.MAIN),
    TIAN_LIANG("天梁", StarCategory.MAIN),
    QI_SHA("七殺", StarCategory.MAIN),
    PO_JUN("破軍", StarCategory.MAIN),

    // ── 六吉輔星 ──
    ZUO_FU("左輔", StarCategory.ASSIST),
    YOU_BI("右弼", StarCategory.ASSIST),
    WEN_CHANG("文昌", StarCategory.ASSIST),
    WEN_QU("文曲", StarCategory.ASSIST),
    TIAN_KUI("天魁", StarCategory.ASSIST),
    TIAN_YUE("天鉞", StarCategory.ASSIST),

    // ── 祿馬（屬輔助）──
    LU_CUN("祿存", StarCategory.ASSIST),
    TIAN_MA("天馬", StarCategory.ASSIST),

    // ── 六煞星 ──
    QING_YANG("擎羊", StarCategory.MALEFIC),
    TUO_LUO("陀羅", StarCategory.MALEFIC),
    HUO_XING("火星", StarCategory.MALEFIC),
    LING_XING("鈴星", StarCategory.MALEFIC),
    DI_KONG("地空", StarCategory.MALEFIC),
    DI_JIE("地劫", StarCategory.MALEFIC),

    // ── 四化（附著於主星，但獨立記錄方便字典查詢）──
    HUA_LU("化祿", StarCategory.TRANSFORMATION),
    HUA_QUAN("化權", StarCategory.TRANSFORMATION),
    HUA_KE("化科", StarCategory.TRANSFORMATION),
    HUA_JI("化忌", StarCategory.TRANSFORMATION);

    companion object {
        /** 所有十四主星（依紫微系→天府系順序）*/
        val mainStars: List<Star>
            get() = entries.filter { it.category == StarCategory.MAIN }

        fun byName(cnName: String): Star? = entries.firstOrNull { it.cnName == cnName }
    }
}

/**
 * 四化類型（化祿、化權、化科、化忌）
 *
 * 這跟 Star 的化祿四星不同：
 * - Star.HUA_LU 是「化祿這個概念星」用於字典查詢
 * - Transformation 是「附著於哪顆主星」的標記
 */
enum class Transformation(val cnName: String, val starName: Star) {
    LU("化祿", Star.HUA_LU),
    QUAN("化權", Star.HUA_QUAN),
    KE("化科", Star.HUA_KE),
    JI("化忌", Star.HUA_JI)
}

/**
 * 帶有四化標記的星曜實例
 *
 * 同一顆「太陽」在不同人的命盤上可能附帶不同的四化（如太陽化祿 / 太陽化忌）。
 * 命盤中安星時用此資料類別表示。
 */
data class PlacedStar(
    val star: Star,
    val transformation: Transformation? = null
) {
    /** 用於字典查詢的鍵 */
    val dictKey: String
        get() = if (transformation != null) {
            "${star.cnName}${transformation.cnName}"
        } else {
            star.cnName
        }
}
