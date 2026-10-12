package com.zwds.today.data.network

import kotlinx.serialization.json.*

/** The ChatGPT plan route uses Responses with no server-side conversation storage. */
object ReadingPayload {
    private val json = Json { ignoreUnknownKeys = true }
    fun buildRequestBody(model: String, systemInstructions: String, userPrompt: String): JsonObject = buildJsonObject {
        require(model.isNotBlank())
        put("model", model)
        put("store", false)
        put("stream", true)
        put("instructions", systemInstructions)
        putJsonArray("input") {
            add(buildJsonObject { put("role", "user"); put("content", userPrompt) })
        }
        putJsonObject("text") {
            putJsonObject("format") {
                put("type", "json_schema")
                put("name", "topic_fortune")
                put("strict", true)
                put("schema", schemaJson())
            }
        }
    }
    private fun schemaJson(): JsonElement = buildJsonObject {
        put("type", "object")
        put("additionalProperties", false)
        putJsonArray("required") {
            add("title")
            add("level")
            add("advice")
            add("tags")
            add("focus_summary")
        }
        putJsonObject("properties") {
            putJsonObject("title") {
                put("type", "string")
                put("description", "8 個字內的吉凶標題，例如「今日中吉」「事業留意」")
            }
            putJsonObject("level") {
                put("type", "string")
                putJsonArray("enum") {
                    add("VERY_GOOD")
                    add("GOOD")
                    add("NEUTRAL")
                    add("CAUTION")
                    add("BAD")
                }
            }
            putJsonObject("advice") {
                put("type", "string")
                put("description", "320-460 字的白話運勢解讀。請分成 3 段短段落：第 1 段講今天整體感覺與主軸，第 2 段講今天容易出現的具體情境、可以把握的機會、人際或做事節奏，第 3 段講最要留意的風險與最實際的做法。用說話的口吻直接告訴來訪者今天在這個主題上的狀況，把星曜翻譯成生活場景，讀起來像命理師面對面在說話。")
            }
            putJsonObject("tags") {
                put("type", "array")
                put("minItems", 3)
                put("maxItems", 4)
                putJsonObject("items") {
                    put("type", "string")
                    put("description", "6 個字內的可執行提醒標籤，例如「先講清楚」「別急著答應」「適合主動談」")
                }
            }
            putJsonObject("focus_summary") {
                put("type", "string")
                put("description", "20 字內的今日一句話提醒，要像命理師給的關鍵叮嚀，例如「今天先把事情講清楚再往前推」「有機會，但別急著一次答太滿」。")
            }
        }
    }

    fun parseFortune(outputText: String): TopicFortuneJson {
        val parsed = runCatching { json.parseToJsonElement(outputText).jsonObject }
            .getOrElse { throw OpenAiException("回傳的解讀格式不完整，請重試") }
        fun text(key: String): String = (parsed[key] as? kotlinx.serialization.json.JsonPrimitive)
            ?.takeIf { it.isString }?.content?.trim().orEmpty()
        val title = text("title")
        val advice = text("advice")
        val level = text("level")
        val tags = (parsed["tags"] as? kotlinx.serialization.json.JsonArray)?.map {
            (it as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { tag -> tag.isString }?.content?.trim().orEmpty()
        }.orEmpty()
        val summary = text("focus_summary")
        if (parsed.keys != setOf("title", "level", "advice", "tags", "focus_summary") ||
            title.isBlank() || advice.isBlank() || summary.isBlank() || tags.size !in 3..4 ||
            tags.any { it.isBlank() } || level !in listOf("VERY_GOOD", "GOOD", "NEUTRAL", "CAUTION", "BAD"))
            throw OpenAiException("回傳的解讀內容不完整，請重試")
        return TopicFortuneJson(title, level, advice, tags, summary)
    }
}

private inline fun kotlinx.serialization.json.JsonObjectBuilder.putJsonObject(
    key: String,
    block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit
) {
    put(key, buildJsonObject(block))
}

private inline fun kotlinx.serialization.json.JsonObjectBuilder.putJsonArray(
    key: String,
    block: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit
) {
    put(key, buildJsonArray(block))
}
