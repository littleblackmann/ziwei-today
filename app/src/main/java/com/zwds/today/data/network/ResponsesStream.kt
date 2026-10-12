package com.zwds.today.data.network

import com.zwds.today.data.auth.OAuthServiceError
import com.zwds.today.data.auth.OAuthStage
import kotlinx.serialization.json.*
import java.io.BufferedReader

/** Collect text events, but only a terminal response.completed authorizes using the result. */
object ResponsesStream {
    private const val MAX_STREAM_CHARS = 2_000_000
    private val json = Json { ignoreUnknownKeys = true }

    fun read(reader: BufferedReader, requestId: String? = null): String {
        val state = StreamState(requestId)
        val eventData = StringBuilder()
        var eventName: String? = null
        var receivedChars = 0L
        fun consumeEvent(): String? {
            val data = eventData.toString().trim()
            eventData.clear()
            val name = eventName
            eventName = null
            if (data.isEmpty() || data == "[DONE]") return null
            val event = runCatching { json.parseToJsonElement(data) as? JsonObject }.getOrNull()
                ?: state.fail("AI 回傳格式無法辨識，請重試", "invalid_event")
            return state.consume(event, name)
        }
        while (true) {
            val line = reader.readLine() ?: break
            receivedChars += line.length + 1L
            if (receivedChars > MAX_STREAM_CHARS) state.fail("AI 回傳內容過大，請重試", "stream_too_large")
            when {
                line.isEmpty() -> consumeEvent()?.let { return it }
                line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                line.startsWith("data:") -> eventData.append(line.removePrefix("data:").removePrefix(" ")).append('\n')
            }
        }
        if (eventData.isNotEmpty()) consumeEvent()?.let { return it }
        state.fail("連線中斷，未收到完整回覆；請重試", "missing_completed")
    }

    private class StreamState(requestId: String?) {
        // Indices keep multiple messages/parts ordered. Done snapshots replace deltas, never append twice.
        private val parts = sortedMapOf<Pair<Int, Int>, StringBuilder>(compareBy({ it.first }, { it.second }))
        private val safeRequestId = OAuthServiceError.parse(OAuthStage.INFERENCE, 200, "{}", requestId).requestId
        private var events = 0
        private var deltas = 0
        private var done = 0
        private var terminal = "none"
        private var outputShape = "none"

        fun fail(message: String, reason: String, usageLimit: Boolean = false): Nothing =
            throw OpenAiException(message, usageLimit = usageLimit, diagnostic = listOfNotNull(
                "解讀串流", "結果=$reason", "事件=$events", "文字片段=$deltas", "完成文字=$done",
                "字元=${parts.values.sumOf { it.length }}", "終態=$terminal", "輸出=$outputShape",
                safeRequestId?.let { "request_id=$it" }
            ).joinToString(" · "))

        fun consume(event: JsonObject, eventName: String?): String? {
            events++
            val type = event.string("type") ?: eventName
            when (type) {
                "response.output_text.delta" -> {
                    deltas++
                    val delta = event.string("delta") ?: fail("AI 回傳格式無法辨識，請重試", "invalid_text")
                    parts.getOrPut(key(event)) { StringBuilder() }.append(delta)
                }
                "response.output_text.done" -> {
                    done++
                    replace(key(event), event.string("text") ?: fail("AI 回傳格式無法辨識，請重試", "invalid_text"))
                }
                "response.content_part.added", "response.content_part.done" -> {
                    val part = event["part"] as? JsonObject ?: fail("AI 回傳格式無法辨識，請重試", "invalid_part")
                    rejectRefusal(part)
                    if (type == "response.content_part.done" && part.string("type") == "output_text") {
                        done++
                        replace(key(event), part.string("text") ?: fail("AI 回傳格式無法辨識，請重試", "invalid_text"))
                    }
                }
                "response.output_item.added", "response.output_item.done" -> {
                    val item = event["item"] as? JsonObject ?: fail("AI 回傳格式無法辨識，請重試", "invalid_item")
                    checkMessage(item)
                    if (type == "response.output_item.done" && item.string("type") == "message") {
                        done++
                        val index = index(event, "output_index")
                        parts.keys.filter { it.first == index }.forEach { parts.remove(it) }
                        collectMessage(item, index)
                    }
                }
                "response.refusal.delta", "response.refusal.done" -> refusal()
                "response.completed" -> {
                    terminal = "completed"
                    val response = event["response"] as? JsonObject ?: fail("AI 回傳缺少完成結果，請重試", "missing_response")
                    if (response.string("status") != "completed")
                        fail("AI 回覆尚未完成，請重試", "invalid_status")
                    if (response["error"] != null && response["error"] != JsonNull)
                        serviceFailure(response["error"] as? JsonObject)
                    val output = when (val value = response["output"]) {
                        null, JsonNull -> emptyList()
                        is JsonArray -> value.map { it as? JsonObject ?: fail("AI 回傳格式無法辨識，請重試", "invalid_output") }
                        else -> fail("AI 回傳格式無法辨識，請重試", "invalid_output")
                    }
                    outputShape = if (output.isEmpty()) "empty" else output.map {
                        when (it.string("type")) {
                            "message" -> "message"
                            "reasoning" -> "reasoning"
                            else -> "other"
                        }
                    }.distinct().sorted().joinToString(",")
                    // A full terminal snapshot is authoritative. A compact envelope uses collected text.
                    if (output.isNotEmpty()) {
                        parts.clear()
                        output.forEachIndexed { index, item -> collectMessage(item, index) }
                    }
                    val text = parts.values.joinToString("")
                    if (text.isBlank()) fail("AI 未回傳解讀內容，請重試", "empty_output")
                    return text
                }
                "response.failed", "response.incomplete", "error" -> {
                    terminal = when (type) {
                        "response.failed" -> "failed"
                        "response.incomplete" -> "incomplete"
                        else -> "error"
                    }
                    serviceFailure((event["response"] as? JsonObject)?.get("error") as? JsonObject
                        ?: event["error"] as? JsonObject ?: event)
                }
            }
            return null
        }

        private fun key(event: JsonObject) = index(event, "output_index") to index(event, "content_index")
        private fun index(event: JsonObject, name: String): Int = if (name !in event) 0 else
            (event[name] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull?.takeIf { it >= 0 }
                ?: fail("AI 回傳格式無法辨識，請重試", "invalid_index")
        private fun replace(key: Pair<Int, Int>, text: String) { parts[key] = StringBuilder(text) }

        private fun rejectRefusal(part: JsonObject) {
            if (part.string("type") == "refusal") refusal()
        }
        private fun refusal(): Nothing = fail("這次服務未提供解讀。可稍後重試或改選其他主題。", "refusal")
        private fun checkMessage(item: JsonObject) {
            if (item.string("type") != "message") return
            (item["content"] as? JsonArray)?.forEach { (it as? JsonObject)?.let(::rejectRefusal) }
            if (item.string("role")?.let { it != "assistant" } == true)
                fail("AI 回傳格式無法辨識，請重試", "invalid_role")
        }
        private fun collectMessage(item: JsonObject, outputIndex: Int) {
            checkMessage(item)
            if (item.string("type") != "message") return
            if (item.string("status")?.let { it != "completed" } == true)
                fail("AI 回覆尚未完成，請重試", "incomplete_message")
            val content = item["content"] as? JsonArray ?: fail("AI 回傳格式無法辨識，請重試", "invalid_content")
            content.forEachIndexed { contentIndex, value ->
                val part = value as? JsonObject ?: fail("AI 回傳格式無法辨識，請重試", "invalid_part")
                if (part.string("type") == "output_text")
                    replace(outputIndex to contentIndex, part.string("text") ?: fail("AI 回傳格式無法辨識，請重試", "invalid_text"))
            }
        }
        private fun serviceFailure(error: JsonObject?): Nothing {
            // Reuse the credential-safe code whitelist; provider descriptions never leave this parser.
            val safe = OAuthServiceError.parse(OAuthStage.INFERENCE, 200,
                buildJsonObject { put("error", error ?: JsonNull) }.toString(), safeRequestId)
            val limited = safe.code in setOf("subscription_sharing_usage_limit_exceeded", "rate_limit_exceeded")
            val message = if (safe.code != null) safe.message else "AI 回覆未完成，請稍後重試"
            fail(message, safe.code ?: terminal, limited)
        }
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull
}

// Only app-owned descriptions and structural diagnostics are exposed to the UI.
class OpenAiException(message: String, cause: Throwable? = null, val usageLimit: Boolean = false,
                      val diagnostic: String? = null) : Exception(message, cause)
