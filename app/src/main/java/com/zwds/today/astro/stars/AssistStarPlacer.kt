package com.zwds.today.astro.stars

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.HeavenlyStem
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.Star

/**
 * 安輔星（六吉星 + 祿存 + 天馬）
 *
 * 六吉星：
 *   左輔：辰宮起正月，順數至生月
 *   右弼：戌宮起正月，逆數至生月
 *   文昌：戌宮起子時，逆數至生時
 *   文曲：辰宮起子時，順數至生時
 *   天魁、天鉞：依年干起表（天乙貴人位）
 *
 * 祿存：依年干，分別於某地支起
 * 天馬：依年支三合（申子辰起寅、亥卯未起巳、寅午戌起申、巳酉丑起亥）
 */
object AssistStarPlacer {

    /**
     * @return 各輔星 → 地支
     */
    fun place(
        yearStem: HeavenlyStem,
        yearBranch: EarthlyBranch,
        lunarMonth: Int,
        hourBranch: EarthlyBranch
    ): Map<EarthlyBranch, List<PlacedStar>> {

        val map = HashMap<EarthlyBranch, MutableList<PlacedStar>>()

        fun put(branch: EarthlyBranch, star: Star) {
            map.getOrPut(branch) { mutableListOf() }.add(PlacedStar(star))
        }

        // 左輔：辰宮起正月，順數至生月
        // 即 左輔 index = 辰(4) + (lunarMonth - 1)
        val zuoFu = EarthlyBranch.fromIndex(EarthlyBranch.CHEN.index + (lunarMonth - 1))
        put(zuoFu, Star.ZUO_FU)

        // 右弼：戌宮起正月，逆數至生月
        // 右弼 index = 戌(10) - (lunarMonth - 1)
        val youBi = EarthlyBranch.fromIndex(EarthlyBranch.XU.index - (lunarMonth - 1))
        put(youBi, Star.YOU_BI)

        // 文昌：戌宮起子時，逆數至生時
        // 文昌 index = 戌(10) - hourBranch.index
        val wenChang = EarthlyBranch.fromIndex(EarthlyBranch.XU.index - hourBranch.index)
        put(wenChang, Star.WEN_CHANG)

        // 文曲：辰宮起子時，順數至生時
        val wenQu = EarthlyBranch.fromIndex(EarthlyBranch.CHEN.index + hourBranch.index)
        put(wenQu, Star.WEN_QU)

        // 天魁、天鉞（天乙貴人，依年干）
        // 甲戊庚牛羊（牛=丑、羊=未）：天魁丑、天鉞未
        // 乙己鼠猴鄉（鼠=子、猴=申）：天魁子、天鉞申
        // 丙丁豬雞位（豬=亥、雞=酉）：天魁亥、天鉞酉
        // 壬癸兔蛇藏（兔=卯、蛇=巳）：天魁卯、天鉞巳
        // 辛逢馬虎（馬=午、虎=寅）：天魁午、天鉞寅
        val (kuiBranch, yueBranch) = when (yearStem) {
            HeavenlyStem.JIA, HeavenlyStem.WU, HeavenlyStem.GENG ->
                EarthlyBranch.CHOU to EarthlyBranch.WEI
            HeavenlyStem.YI, HeavenlyStem.JI ->
                EarthlyBranch.ZI to EarthlyBranch.SHEN
            HeavenlyStem.BING, HeavenlyStem.DING ->
                EarthlyBranch.HAI to EarthlyBranch.YOU
            HeavenlyStem.REN, HeavenlyStem.GUI ->
                EarthlyBranch.MAO to EarthlyBranch.SI
            HeavenlyStem.XIN ->
                EarthlyBranch.WU to EarthlyBranch.YIN
        }
        put(kuiBranch, Star.TIAN_KUI)
        put(yueBranch, Star.TIAN_YUE)

        // 祿存（依年干）
        //   甲祿在寅、乙祿在卯、丙戊祿在巳、丁己祿在午、庚祿在申、辛祿在酉、壬祿在亥、癸祿在子
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
        put(luBranch, Star.LU_CUN)

        // 天馬（依年支三合，馬星規則）
        //   申子辰 → 馬在寅
        //   寅午戌 → 馬在申
        //   巳酉丑 → 馬在亥
        //   亥卯未 → 馬在巳
        val maBranch = when (yearBranch) {
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YIN
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.SHEN
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.HAI
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.SI
        }
        put(maBranch, Star.TIAN_MA)

        return map.mapValues { it.value.toList() }
    }
}
