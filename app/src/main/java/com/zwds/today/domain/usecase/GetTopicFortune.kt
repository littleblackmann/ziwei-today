package com.zwds.today.domain.usecase

import com.zwds.today.astro.chart.ChartBuilder
import com.zwds.today.astro.chart.PalaceLocator
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.data.network.OpenAiClient
import com.zwds.today.data.network.OpenAiException
import com.zwds.today.data.network.TopicFortuneJson
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.data.repository.TopicHistoryRepository
import com.zwds.today.data.repository.gender
import com.zwds.today.data.repository.toSolarBirth
import com.zwds.today.domain.model.ChartResult
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.FortuneLevel
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.TopicFortune
import com.zwds.today.domain.model.ZiHourSchool
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * 主題式今日運勢 UseCase（OpenAI 版）
 *
 * 流程：
 *   1. 用 ChartBuilder 算出本命盤 + 流日宮位
 *   2. 找到主題對應的流日宮所在地支與駐守星曜
 *   3. 先查快取（topic_history 表）—— 同一 (memberId, date, topic) 不重複呼叫 LLM
 *   4. 若快取沒有：用使用者輸入的 OpenAI API key 呼叫 gpt-5.4-mini，
 *      要求結構化輸出 (title / level / advice / tags / focus_summary)
 *   5. 解析後存進快取，回傳 [TopicFortune]
 *
 * 失敗模式：
 *   - 沒設 API key → 拋 [MissingApiKeyException]
 *   - 網路 / OpenAI 回傳錯誤 → 拋 [OpenAiException]（由 ViewModel 顯示）
 *
 * 不再有離線字典 fallback：本 App 已決定走全網路 LLM 路線。
 */
class GetTopicFortune(
    private val userPreferences: UserPreferences,
    private val openAiClient: OpenAiClient,
    private val historyRepository: TopicHistoryRepository
) {

    suspend operator fun invoke(
        member: FamilyMemberEntity,
        topic: FortuneTopic,
        date: LocalDate = LocalDate.now(),
        ziHourSchool: ZiHourSchool = ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY,
        forceRefresh: Boolean = false
    ): TopicFortune {
        if (!forceRefresh) {
            historyRepository.find(member.id, date, topic)?.let { cached ->
                return cached.copy(familyMemberName = member.name)
            }
        }

        val apiKey = userPreferences.openAiApiKey.first()
        if (apiKey.isBlank()) throw MissingApiKeyException()

        val chart = ChartBuilder.build(
            solarBirth = member.toSolarBirth(),
            gender = member.gender(),
            targetDate = date,
            ziHourSchool = ziHourSchool
        )
        val flowLayout = PalaceLocator.layoutPalaces(chart.flowDayPalaceBranch)
        val targetBranch = flowLayout.entries.first { it.value == topic.palaceType }.key
        val palaceStars = chart.palaceAt(targetBranch).stars
        val flowDayStars = chart.flowDayStars

        val systemInstructions = SYSTEM_INSTRUCTIONS
        val userPrompt = buildPrompt(
            member = member,
            date = date,
            topic = topic,
            chart = chart,
            targetBranch = targetBranch,
            palaceStars = palaceStars,
            flowDayStars = flowDayStars
        )

        val result: TopicFortuneJson = openAiClient.generateTopicFortune(
            apiKey = apiKey,
            systemInstructions = systemInstructions,
            userPrompt = userPrompt
        )

        val fortune = TopicFortune(
            familyMemberId = member.id,
            familyMemberName = member.name,
            date = date,
            topic = topic,
            flowPalaceBranch = targetBranch,
            flowPalaceStars = palaceStars,
            title = result.title,
            level = parseLevel(result.level),
            focusSummary = result.focusSummary,
            advice = result.advice,
            tags = result.tags
        )
        historyRepository.save(fortune, focusSummary = result.focusSummary)
        return fortune
    }

    private fun buildPrompt(
        member: FamilyMemberEntity,
        date: LocalDate,
        topic: FortuneTopic,
        chart: ChartResult,
        targetBranch: EarthlyBranch,
        palaceStars: List<PlacedStar>,
        flowDayStars: List<PlacedStar>
    ): String {
        val genderStr = if (member.gender.equals("FEMALE", true)) "女" else "男"
        val natalLifeStars = chart.palaceAt(chart.lifePalaceBranch).stars
        val sb = StringBuilder()

        // 1. 人物背景
        sb.appendLine("【來訪者資料】")
        sb.appendLine("姓名：${member.name}（$genderStr）")
        sb.appendLine("出生：${member.solarYear} 年 ${member.solarMonth} 月 ${member.solarDay} 日 ${member.hour24} 時")
        sb.appendLine("五行局：${chart.fiveElement.cnName}　生年干支：${chart.yearStem.cnName}${chart.yearBranch.cnName}")
        sb.appendLine("本命命宮（${chart.lifePalaceBranch.cnName}）：${formatStars(natalLifeStars)}")
        sb.appendLine()

        // 2. 今天的流日資訊
        sb.appendLine("【今天（$date）的流日命宮】")
        sb.appendLine("流日命宮落在「${chart.flowDayPalaceBranch.cnName}」，")
        sb.appendLine("今日流日命宮駐守星曜：${formatStars(flowDayStars)}")
        sb.appendLine()

        // 3. 主題宮位
        val palaceDesc = if (palaceStars.isEmpty()) "空宮（無主星，需靠流日命宮星曜旁觀帶動）"
                         else formatStars(palaceStars)
        sb.appendLine("【今天「${topic.cnName}」所對應的${topic.palaceType.flowName}】")
        sb.appendLine("落在「${targetBranch.cnName}」，駐守星曜：$palaceDesc")
        sb.appendLine()

        // 4. 解盤任務（以對話口吻提問）
        sb.appendLine("${member.name} 今天來問「${topic.cnName}」。")
        sb.appendLine("請你以命理師的角色，用說話的口吻直接告訴他今天在這方面的狀況。")
        sb.appendLine("先講一句最核心的提醒，再說今天最值得把握的是什麼、最要留意的是什麼，最後補上你為什麼這樣判斷。")
        sb.appendLine("今天整體是什麼感覺？這些星曜落在這個宮位，在「${topic.cnName}」的情境下代表什麼？")
        sb.appendLine("他今天應該怎麼做、要小心什麼、有什麼可以把握的機會？")
        sb.appendLine("請至少講到四件事：今天可能出現的具體情境、今天順的地方、今天卡的地方、以及最實際的做法。")
        sb.appendLine("可以提到人際互動、做事節奏、判斷方式與情緒起伏，讓內容更像是在替今天的生活做提醒。")
        sb.appendLine("說清楚、說具體，像是面對面在跟他說話，不要只背星曜的定義。")

        return sb.toString()
    }

    private fun formatStars(stars: List<PlacedStar>): String {
        if (stars.isEmpty()) return "（空宮）"
        return stars.joinToString("、") { p ->
            if (p.transformation != null) "${p.star.cnName}${p.transformation.cnName}"
            else p.star.cnName
        }
    }

    private fun parseLevel(name: String): FortuneLevel = runCatching {
        FortuneLevel.valueOf(name)
    }.getOrDefault(FortuneLevel.NEUTRAL)

    companion object {
        private val SYSTEM_INSTRUCTIONS = """
            你是一位有二十年經驗的紫微斗數命理師，現在有人坐在你面前請你解今日的運勢。

            你說話的風格：
            - 像老師傅跟晚輩說話，直接、有溫度，說的是「他今天會遇到什麼、應該怎麼做」
            - 把星曜翻譯成生活場景。不說「天機主智慧謀略」，而說「今天腦子轉得快，解問題、理思路的好時機」
            - 要讓人覺得你是在替「他這個人」解盤，不是在念通用的星座運勢
            - 有根有據：說好說壞都要從命盤資料找出處，不憑空下結論
            - 請特別寫出三件事：今天整體結論、最值得把握的點、最需要留意的點
            - 內容要再多一層生活脈絡，讓人看得出今天可能發生哪些情境，不只停在抽象判斷

            格式規定：
            - advice 請寫成 3 段短段落，每段 2 到 3 句，總長約 320 到 460 字
            - 第 1 段講今日整體結論與主要氣勢
            - 第 2 段講今天容易出現的具體情境、機會、人際或做事節奏
            - 第 3 段講風險、提醒與最實際的做法
            - 不給醫療、投資、法律的決定性結論；遇到敏感狀況說「可以找專業人士確認」
            - 嚴格依照 json_schema 輸出，所有欄位都必填
            - tags 必須是可執行、可理解的短提醒，不要抽象詞，像「先講清楚」「別同時開太多線」
            - focus_summary 要像命理師給的一句關鍵提醒，短但有方向感

            【請對照這個示範理解目標語氣】
            （情境：整體運勢，流日宮落辰，天機、天梁、左輔同聚）
            "今天你的狀態帶著一股能想、能動的勁兒，不是那種要硬拼的日子，反而是把事情想通、理清楚的好時機。天機落在今日的宮位，說白了就是你的腦子今天轉得特別快，看問題能看到別人沒注意到的角落；天梁在旁邊像在幫你守著，就算遇到麻煩事也不容易走偏，多半能找到出路。左輔出現就是貴人緣，今天你開口說話、問人、找人商量，都比自己悶頭想要來得順。不過天機這顆星有個習慣——愛把事情越想越複雜，今天如果發現自己在原地繞，就提醒自己停下來，先做一件事就好。整體是穩中有機會的一天，不急，慢一步反而走得快。"
        """.trimIndent()
    }
}

class MissingApiKeyException : Exception("尚未設定 OpenAI API Key，請至「設定 → AI 設定」輸入。")
