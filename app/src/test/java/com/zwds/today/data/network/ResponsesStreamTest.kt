package com.zwds.today.data.network

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.StringReader

class ResponsesStreamTest {
    private fun read(s: String) = ResponsesStream.read(StringReader(s).buffered())
    private fun stream(vararg events: String) = events.joinToString("") { "data: $it\n\n" }
    private val completed = """{"type":"response.completed","response":{"status":"completed","output":[]}}"""
    private fun delta(text: String, output: Int = 0, part: Int = 0) = buildJsonObject {
        put("type", "response.output_text.delta"); put("output_index", output); put("content_index", part); put("delta", text)
    }.toString()
    @Test fun completedEnvelopeMayOmitTextAlreadyDeliveredByStream() {
        val events = "data: {\"type\":\"response.output_text.delta\",\"output_index\":0,\"content_index\":0,\"delta\":\"完整\"}\n\n" +
            "data: {\"type\":\"response.output_text.delta\",\"output_index\":0,\"content_index\":0,\"delta\":\"解讀\"}\n\n" +
            "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[]}}\n\n"
        assertEquals("完整解讀", read(events))
    }
    @Test fun completionCanFollowReasoningAndDeltas() {
        val events = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\n" +
            "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[" +
            "{\"type\":\"reasoning\"},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"完整解讀\"}]}]}}\n\n"
        assertEquals("完整解讀", read(events))
    }
    @Test fun partialAndDoneAreNeverTreatedAsSuccess() {
        assertThrows(OpenAiException::class.java) { read("data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\ndata: [DONE]\n\n") }
    }
    @Test fun quotaFailureAfterStreamingDoesNotSaveResult() {
        val error = assertThrows(OpenAiException::class.java) { read(
            "data: {\"type\":\"response.output_text.delta\",\"delta\":\"partial\"}\n\n" +
                "data: {\"type\":\"response.failed\",\"response\":{\"error\":{\"code\":\"subscription_sharing_usage_limit_exceeded\"}}}\n\n") }
        assertTrue(error.message!!.contains("使用限制"))
        assertTrue(error.usageLimit)
    }
    @Test fun incompleteAndRefusalFailClearly() {
        assertThrows(OpenAiException::class.java) { read("data: {\"type\":\"response.incomplete\"}\n\n") }
        assertThrows(OpenAiException::class.java) { read("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[]}}\n\n") }
    }
    @Test fun finalizedSnapshotsReplaceDeltasWithoutDuplicatingText() {
        assertEquals("完整解讀", read(stream(delta("完整"), delta("解讀"),
            """{"type":"response.output_text.done","text":"完整解讀"}""",
            """{"type":"response.content_part.done","part":{"type":"output_text","text":"完整解讀"}}""",
            """{"type":"response.output_item.done","item":{"type":"message","status":"completed","role":"assistant","content":[{"type":"output_text","text":"完整解讀"}]}}""",
            completed)))
    }
    @Test fun eachFinalizedTextEventCanSupplyTheText() {
        listOf(
            """{"type":"response.output_text.done","text":"完整解讀"}""",
            """{"type":"response.content_part.done","part":{"type":"output_text","text":"完整解讀"}}""",
            """{"type":"response.output_item.done","item":{"type":"message","content":[{"type":"output_text","text":"完整解讀"}]}}"""
        ).forEach { event ->
            assertEquals("完整解讀", read(stream(event, """{"type":"response.completed","response":{"status":"completed"}}""")))
            assertThrows(OpenAiException::class.java) { read(stream(event)) }
        }
    }
    @Test fun interleavedPartsAreOrderedByOutputAndContentIndices() {
        assertEquals("第一段第二段第三段", read(stream(delta("第三段", 2), delta("第二段", 1, 1), delta("第一段", 1),
            """{"type":"response.reasoning_summary_text.delta","delta":"不應進入解讀"}""",
            """{"type":"response.function_call_arguments.delta","delta":"不應進入解讀"}""", completed)))
    }
    @Test fun explicitEmptyFinalMessageCannotReuseEarlierPartialText() {
        assertThrows(OpenAiException::class.java) { read(stream(delta("partial"),
            """{"type":"response.completed","response":{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":""}]}]}}""")) }
    }
    @Test fun refusalIsRecognizedAtEverySupportedLevel() {
        listOf(
            """{"type":"response.refusal.delta","delta":"secret-refusal"}""",
            """{"type":"response.refusal.done","refusal":"secret-refusal"}""",
            """{"type":"response.content_part.added","part":{"type":"refusal","refusal":"secret-refusal"}}""",
            """{"type":"response.content_part.done","part":{"type":"refusal","refusal":"secret-refusal"}}""",
            """{"type":"response.output_item.done","item":{"type":"message","content":[{"type":"refusal","refusal":"secret-refusal"}]}}""",
            """{"type":"response.completed","response":{"status":"completed","output":[{"type":"message","content":[{"type":"refusal","refusal":"secret-refusal"}]}]}}"""
        ).forEach { event ->
            val error = assertThrows(OpenAiException::class.java) { read(stream(delta("partial"), event, completed)) }
            assertTrue(error.diagnostic!!.contains("結果=refusal"))
            assertFalse((error.message + error.diagnostic).contains("secret-refusal"))
        }
    }
    @Test fun unavailableUsageAfterDoneStillFails() {
        val error = assertThrows(OpenAiException::class.java) { read(stream(
            """{"type":"response.output_text.done","text":"partial"}""",
            """{"type":"response.failed","response":{"error":{"code":"subscription_sharing_usage_unavailable"}}}""")) }
        assertTrue(error.message!!.contains("會員額度"))
        assertFalse(error.usageLimit)
    }
    @Test fun incompleteStatusAndIncompleteMessageNeverBecomeSuccess() {
        listOf(
            """{"type":"response.incomplete","response":{"incomplete_details":{"reason":"max_output_tokens"}}}""",
            """{"type":"response.completed","response":{"status":"incomplete","output":[]}}""",
            """{"type":"response.completed","response":{"status":"completed","output":[{"type":"message","status":"incomplete","content":[{"type":"output_text","text":"partial"}]}]}}""",
            """{"type":"response.completed","response":{"status":"completed","error":{"code":"rate_limit_exceeded"}}}"""
        ).forEach { assertThrows(OpenAiException::class.java) { read(stream(delta("partial"), it)) } }
    }
    @Test fun diagnosticsContainOnlyStructuralFieldsAndSafeRequestId() {
        val error = assertThrows(OpenAiException::class.java) {
            ResponsesStream.read(StringReader(stream(delta("private-birth-data"),
                """{"type":"error","code":"secret-token-code","message":"Bearer secret-token email@example.test"}"""))
                .buffered(), "req_qa-142")
        }
        assertTrue(error.diagnostic!!.contains("文字片段=1"))
        assertTrue(error.diagnostic!!.contains("request_id=req_qa-142"))
        listOf("private-birth-data", "secret-token", "email@").forEach {
            assertFalse((error.message + error.diagnostic).contains(it))
        }
        val unsafeId = assertThrows(OpenAiException::class.java) {
            ResponsesStream.read(StringReader(stream(completed)).buffered(), "Bearer private-token")
        }
        assertFalse(unsafeId.diagnostic!!.contains("private-token"))
    }
    @Test fun trulyEmptyCompletionHasDistinctDiagnostic() {
        val error = assertThrows(OpenAiException::class.java) { read(stream(completed)) }
        assertTrue(error.diagnostic!!.contains("結果=empty_output"))
        assertTrue(error.diagnostic!!.contains("文字片段=0"))
        assertTrue(error.diagnostic!!.contains("終態=completed"))
    }
    @Test fun malformedStreamDoesNotExposePayloadInMessage() {
        listOf("{private-token", "[]", """{"type":"response.output_text.delta","delta":123}""",
            """{"type":"response.output_text.delta","output_index":-1,"delta":"x"}""",
            """{"type":"response.completed","response":{"status":"completed","output":{}}}"""
        ).forEach { event ->
            val error = assertThrows(OpenAiException::class.java) { read(stream(event)) }
            assertFalse((error.message + error.diagnostic).contains("private-token"))
            assertNotNull(error.diagnostic)
        }
    }
    @Test fun sseHeadersMultilineCrLfAndFinalEventWithoutBlankLineAreSupported() {
        val s = ": heartbeat\r\nevent: response.output_text.delta\r\ndata: {\"output_index\":0,\r\ndata: \"content_index\":0,\"delta\":\"完整解讀\"}\r\n\r\n" +
            "event: response.completed\r\ndata: {\"response\":{\"status\":\"completed\",\"output\":[]}}"
        assertEquals("完整解讀", read(s))
    }
    @Test fun boundedStreamRejectsExcessiveCumulativeData() {
        val error = assertThrows(OpenAiException::class.java) { read(stream(*Array(8) { delta("x".repeat(260_000)) })) }
        assertTrue(error.diagnostic!!.contains("stream_too_large"))
    }
    @Test fun streamedFortuneStillPassesStrictReadingValidation() {
        val fortune = """{"title":"今日中吉","level":"GOOD","advice":"第一段\n\n第二段\n\n第三段","tags":["慢慢來","講清楚","留餘裕"],"focus_summary":"今天先講清楚"}"""
        val s = stream(*fortune.chunked(7).map { delta(it) }.toTypedArray(), completed)
        val result = ReadingPayload.parseFortune(read(s))
        assertEquals("今日中吉", result.title)
        assertEquals("第一段\n\n第二段\n\n第三段", result.advice)
        assertThrows(OpenAiException::class.java) { ReadingPayload.parseFortune(read(stream(delta("{}"), completed))) }
    }
}
