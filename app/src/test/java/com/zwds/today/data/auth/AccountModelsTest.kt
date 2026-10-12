package com.zwds.today.data.auth

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class AccountModelsTest {
    @Test fun usesOnlyVisibleCatalogModelsInServerOrder() {
        val models = AccountModels.parse(Json.parseToJsonElement("""{"models":[
            {"slug":"hidden","visibility":"hide"},
            {"slug":"first","visibility":"list","display_name":"First"},
            {"slug":"second","visibility":"list"},
            {"slug":"","visibility":"list"},
            {"slug":"first","visibility":"list"}]}""").jsonObject)
        assertEquals(listOf("first", "second"), models.map { it.id })
        assertEquals("first", AccountModels.select(models, "model-from-another-account"))
        assertEquals("second", AccountModels.select(models, "second"))
    }
    @Test fun noCatalogDoesNotInventFallbackModel() {
        assertEquals("", AccountModels.select(AccountModels.parse(buildJsonObject {}), "old"))
    }
    @Test fun switchingOrLoggingOutInvalidatesEarlierResultReceipts() {
        val generation = SessionGeneration()
        val old = generation.current()
        assertTrue(generation.matches(old))
        generation.advance()
        assertFalse(generation.matches(old))
        assertTrue(generation.matches(generation.current()))
    }
}
