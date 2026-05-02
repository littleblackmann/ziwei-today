package com.zwds.today.astro.stars

import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.FiveElementClass
import kotlin.math.ceil

/**
 * 起紫微星位置
 *
 * 古訣依「五行局 + 農曆生日」推算紫微星落於哪個地支。
 *
 * 公式（iztro / lunar-javascript / 多數現代派系一致採用）：
 *
 *   設五行局數 c（水二=2、木三=3、金四=4、土五=5、火六=6）
 *   設農曆生日 d
 *
 *   q = ceil(d / c)            # 商，向上取整
 *   r = q * c - d              # 餘
 *
 *   若 r 為偶數：位移量 = -r/2
 *   若 r 為奇數：位移量 = (r+1)/2
 *
 *   紫微地支索引（從子=0 起算）= (q - 1) + 位移量，再對 12 取模。
 */
object ZiWeiPlacer {

    fun placeZiWei(fiveElementClass: FiveElementClass, lunarDay: Int): EarthlyBranch {
        require(lunarDay in 1..30) { "lunarDay must be 1..30, got $lunarDay" }
        val c = fiveElementClass.days
        val q = ceil(lunarDay.toDouble() / c).toInt()
        val r = q * c - lunarDay

        val shift = if (r % 2 == 0) -r / 2 else (r + 1) / 2
        val raw = (q - 1) + shift
        val idx = ((raw % 12) + 12) % 12

        return EarthlyBranch.fromIndex(idx)
    }
}
