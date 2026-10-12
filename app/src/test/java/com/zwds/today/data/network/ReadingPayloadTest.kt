package com.zwds.today.data.network

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ReadingPayloadTest {
    @Test fun membershipRequestUsesResponsesPreviewContract() {
        val payload = ReadingPayload.buildRequestBody("account-catalog-model", "instructions", "chart-summary")
        assertFalse(payload["store"]!!.jsonPrimitive.boolean)
        assertTrue(payload["stream"]!!.jsonPrimitive.boolean)
        assertEquals("instructions", payload["instructions"]!!.jsonPrimitive.content)
        val input = payload["input"]!!.jsonArray.single().jsonObject
        assertEquals("user", input["role"]!!.jsonPrimitive.content)
        assertEquals("chart-summary", input["content"]!!.jsonPrimitive.content)
        assertEquals("json_schema", payload["text"]!!.jsonObject["format"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        listOf("messages", "response_format", "provider", "max_tokens", "max_output_tokens", "temperature", "previous_response_id").forEach {
            assertFalse(it, payload.containsKey(it))
        }
    }
    @Test fun acceptsCompleteReadingAndRejectsPartialOrUnstructuredOutput() {
        val valid = """{"title":"今日中吉","level":"GOOD","advice":"完整內容","tags":["慢慢來","講清楚","留餘裕"],"focus_summary":"今天先講清楚"}"""
        assertEquals("GOOD", ReadingPayload.parseFortune(valid).level)
        listOf("partial", "{}", valid.replace("GOOD", "UNKNOWN"), valid.replace("\"完整內容\"", "null"),
            valid.replace("\"慢慢來\",", "")).forEach {
            assertThrows(OpenAiException::class.java) { ReadingPayload.parseFortune(it) }
        }
    }
}
