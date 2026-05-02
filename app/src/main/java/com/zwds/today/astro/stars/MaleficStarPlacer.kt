package com.zwds.today.astro.stars

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.Gender
import com.zwds.today.domain.model.HeavenlyStem
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.Star

/**
 * 安煞星（六煞）
 *
 * 擎羊、陀羅：祿前一位為擎羊、祿後一位為陀羅
 * 火星、鈴星：依「年支三合 + 時支」起表（陽男陰女與陰男陽女順逆有別）
 * 地空、地劫：依「亥宮起子時」一順一逆
 *   地劫：亥宮順數至生時
 *   地空：亥宮逆數至生時
 */
object MaleficStarPlacer {

    fun place(
        yearStem: HeavenlyStem,
        yearBranch: EarthlyBranch,
        hourBranch: EarthlyBranch,
        gender: Gender
    ): Map<EarthlyBranch, List<PlacedStar>> {

        val map = HashMap<EarthlyBranch, MutableList<PlacedStar>>()
        fun put(branch: EarthlyBranch, star: Star) {
            map.getOrPut(branch) { mutableListOf() }.add(PlacedStar(star))
        }

        // 擎羊、陀羅（依祿存位置）
        val luBranch = when (yearStem) {
            HeavenlyStem.JIA -> EarthlyBranch.YIN
            HeavenlyStem.YI -> EarthlyBranch.MAO
            HeavenlyStem.BING, HeavenlyStem.WU -> EarthlyBranch.SI
            HeavenlyStem.DING, HeavenlyStem.JI -> EarthlyBranch.WU
            HeavenlyStem.GENG -> EarthlyBranch.SHEN
            HeavenlyStem.XIN -> EarthlyBranch.YOU
            HeavenlyStem.REN -> EarthlyBranch.HAI
            HeavenlyStem.GUI -> EarthlyBranch.ZI
        }
        put(EarthlyBranch.fromIndex(luBranch.index + 1), Star.QING_YANG)
        put(EarthlyBranch.fromIndex(luBranch.index - 1), Star.TUO_LUO)

        // 火星、鈴星（依年支三合 + 時支起例）
        // 標準口訣：
        //   寅午戌人 → 丑卯
        //   申子辰人 → 寅戌
        //   巳酉丑人 → 卯戌
        //   亥卯未人 → 酉戌
        //   每組第一個是「火星起點」、第二個是「鈴星起點」
        // 然後從起點順數至生時
        val (huoStart, lingStart) = when (yearBranch) {
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU ->
                EarthlyBranch.CHOU to EarthlyBranch.MAO
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN ->
                EarthlyBranch.YIN to EarthlyBranch.XU
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU ->
                EarthlyBranch.MAO to EarthlyBranch.XU
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI ->
                EarthlyBranch.YOU to EarthlyBranch.XU
        }
        put(EarthlyBranch.fromIndex(huoStart.index + hourBranch.index), Star.HUO_XING)
        put(EarthlyBranch.fromIndex(lingStart.index + hourBranch.index), Star.LING_XING)

        // 地空、地劫（依生時，亥宮起子時）
        // 地劫：順數
        // 地空：逆數
        val diJie = EarthlyBranch.fromIndex(EarthlyBranch.HAI.index + hourBranch.index)
        val diKong = EarthlyBranch.fromIndex(EarthlyBranch.HAI.index - hourBranch.index)
        put(diJie, Star.DI_JIE)
        put(diKong, Star.DI_KONG)

        // gender 目前未用於本層級，但保留參數供未來派系擴充
        @Suppress("UNUSED_PARAMETER")
        gender

        return map.mapValues { it.value.toList() }
    }
}
