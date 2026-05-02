package com.zwds.today.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI Responses API 客戶端
 *
 * 使用 GPT-5.4 model family。模型固定 `gpt-5.4-mini`，
 * 透過 json_schema 結構化輸出強制回傳 title / level / advice / tags。
 *
 * 此 client 不快取也不重試（呼叫端負責）。錯誤統一以 [OpenAiException] 拋出。
 */
class OpenAiClient(
    private val httpClient: OkHttpClient = defaultClient()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /**
     * 呼叫 Responses API，回傳模型輸出的 JSON 物件（已經是 [TopicFortuneJson] schema）
     *
     * @param apiKey 使用者自己的 OpenAI API key
     * @param systemInstructions 模型行為指令
     * @param userPrompt 結構化的使用者輸入（含命盤摘要與主題）
     * @return 解析後的 [TopicFortuneJson]
     * @throws OpenAiException 若 API key 無效、網路失敗或回傳格式不符
     */
    suspend fun generateTopicFortune(
        apiKey: String,
        systemInstructions: String,
        userPrompt: String
    ): TopicFortuneJson = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "OpenAI API key 為空" }

        val body = buildRequestBody(
            systemInstructions = systemInstructions,
            userPrompt = userPrompt
        )
        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: IOException) {
            throw OpenAiException("網路連線失敗：${e.message}", e)
        }

        response.use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                val message = parseErrorMessage(text) ?: "HTTP ${res.code}"
                throw OpenAiException("OpenAI 回傳錯誤：$message")
            }
            parseResponseBody(text)
        }
    }

    private fun buildRequestBody(
        systemInstructions: String,
        userPrompt: String
    ): JsonObject = buildJsonObject {
        put("model", MODEL)
        put("instructions", systemInstructions)
        put("input", userPrompt)
        putJsonObject("reasoning") {
            put("effort", "low")
        }
        putJsonObject("text") {
            put("verbosity", "high")
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

    private fun parseResponseBody(text: String): TopicFortuneJson {
        val root = runCatching { json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw OpenAiException("OpenAI 回傳內容無法解析為 JSON") }

        // Responses API: output[0].content[0].text 即為 json_schema 結果（字串形式的 JSON）
        val outputArray = root["output"]?.jsonArray
            ?: throw OpenAiException("OpenAI 回傳缺少 output 欄位")

        // 找第一個 type=message 的 item
        val message = outputArray.firstOrNull {
            it.jsonObject["type"]?.jsonPrimitive?.content == "message"
        }?.jsonObject ?: throw OpenAiException("OpenAI 回傳缺少 message")

        val contents = message["content"]?.jsonArray
            ?: throw OpenAiException("OpenAI 訊息缺少 content")

        val outputText = contents.firstNotNullOfOrNull { c ->
            val obj = c.jsonObject
            val type = obj["type"]?.jsonPrimitive?.content
            if (type == "output_text") obj["text"]?.jsonPrimitive?.content else null
        } ?: throw OpenAiException("OpenAI 回傳沒有 output_text")

        val parsed = runCatching {
            json.parseToJsonElement(outputText).jsonObject
        }.getOrElse { throw OpenAiException("模型輸出非合法 JSON：${outputText.take(200)}") }

        return TopicFortuneJson(
            title = parsed["title"]?.jsonPrimitive?.content.orEmpty(),
            level = parsed["level"]?.jsonPrimitive?.content.orEmpty(),
            advice = parsed["advice"]?.jsonPrimitive?.content.orEmpty(),
            tags = parsed["tags"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.content }
                .orEmpty(),
            focusSummary = parsed["focus_summary"]?.jsonPrimitive?.content.orEmpty()
        )
    }

    private fun parseErrorMessage(text: String): String? = runCatching {
        val obj = json.parseToJsonElement(text).jsonObject
        obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
    }.getOrNull()

    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/responses"
        private const val MODEL = "gpt-5.4-mini"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}

data class TopicFortuneJson(
    val title: String,
    val level: String,
    val advice: String,
    val tags: List<String>,
    val focusSummary: String
)

class OpenAiException(message: String, cause: Throwable? = null) : Exception(message, cause)

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
