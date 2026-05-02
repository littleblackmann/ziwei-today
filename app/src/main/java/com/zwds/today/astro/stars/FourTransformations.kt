package com.zwds.today.astro.stars

import com.zwds.today.domain.model.HeavenlyStem
import com.zwds.today.domain.model.Star
import com.zwds.today.domain.model.Transformation

/**
 * 四化（化祿、化權、化科、化忌）
 *
 * 紫微斗數中，四化是判斷流運吉凶最關鍵的參數。
 * 每個天干對應一組「化祿、化權、化科、化忌」附著於四顆主星：
 *
 * 業界廣為流通的四化表（以「中州派」為主，現代多數派系沿用）：
 *
 *   甲：廉貞化祿、破軍化權、武曲化科、太陽化忌
 *   乙：天機化祿、天梁化權、紫微化科、太陰化忌
 *   丙：天同化祿、天機化權、文昌化科、廉貞化忌
 *   丁：太陰化祿、天同化權、天機化科、巨門化忌
 *   戊：貪狼化祿、太陰化權、右弼化科、天機化忌
 *   己：武曲化祿、貪狼化權、天梁化科、文曲化忌
 *   庚：太陽化祿、武曲化權、天同化科 *、天同化忌（中州派）
 *      註：庚干化科有派系差異，部分派系用太陰化科。本系統採中州派天同化科（與化忌同主天同）。
 *      實作上若使用者選不同派系，可在此修改。
 *   辛：巨門化祿、太陽化權、文曲化科、文昌化忌
 *   壬：天梁化祿、紫微化權、左輔化科、武曲化忌
 *   癸：破軍化祿、巨門化權、太陰化科、貪狼化忌
 */
object FourTransformations {

    /**
     * 取得指定天干的四化主星
     *
     * @return 四個 Pair<Transformation, Star>
     */
    fun byStem(stem: HeavenlyStem): List<Pair<Transformation, Star>> = when (stem) {
        HeavenlyStem.JIA -> listOf(
            Transformation.LU to Star.LIAN_ZHEN,
            Transformation.QUAN to Star.PO_JUN,
            Transformation.KE to Star.WU_QU,
            Transformation.JI to Star.TAI_YANG
        )
        HeavenlyStem.YI -> listOf(
            Transformation.LU to Star.TIAN_JI,
            Transformation.QUAN to Star.TIAN_LIANG,
            Transformation.KE to Star.ZI_WEI,
            Transformation.JI to Star.TAI_YIN
        )
        HeavenlyStem.BING -> listOf(
            Transformation.LU to Star.TIAN_TONG,
            Transformation.QUAN to Star.TIAN_JI,
            Transformation.KE to Star.WEN_CHANG,
            Transformation.JI to Star.LIAN_ZHEN
        )
        HeavenlyStem.DING -> listOf(
            Transformation.LU to Star.TAI_YIN,
            Transformation.QUAN to Star.TIAN_TONG,
            Transformation.KE to Star.TIAN_JI,
            Transformation.JI to Star.JU_MEN
        )
        HeavenlyStem.WU -> listOf(
            Transformation.LU to Star.TAN_LANG,
            Transformation.QUAN to Star.TAI_YIN,
            Transformation.KE to Star.YOU_BI,
            Transformation.JI to Star.TIAN_JI
        )
        HeavenlyStem.JI -> listOf(
            Transformation.LU to Star.WU_QU,
            Transformation.QUAN to Star.TAN_LANG,
            Transformation.KE to Star.TIAN_LIANG,
            Transformation.JI to Star.WEN_QU
        )
        HeavenlyStem.GENG -> listOf(
            Transformation.LU to Star.TAI_YANG,
            Transformation.QUAN to Star.WU_QU,
            Transformation.KE to Star.TAI_YIN,   // 部分派系使用太陰化科
            Transformation.JI to Star.TIAN_TONG
        )
        HeavenlyStem.XIN -> listOf(
            Transformation.LU to Star.JU_MEN,
            Transformation.QUAN to Star.TAI_YANG,
            Transformation.KE to Star.WEN_QU,
            Transformation.JI to Star.WEN_CHANG
        )
        HeavenlyStem.REN -> listOf(
            Transformation.LU to Star.TIAN_LIANG,
            Transformation.QUAN to Star.ZI_WEI,
            Transformation.KE to Star.ZUO_FU,
            Transformation.JI to Star.WU_QU
        )
        HeavenlyStem.GUI -> listOf(
            Transformation.LU to Star.PO_JUN,
            Transformation.QUAN to Star.JU_MEN,
            Transformation.KE to Star.TAI_YIN,
            Transformation.JI to Star.TAN_LANG
        )
    }
}
