package com.zwds.today.data.auth

import kotlinx.serialization.json.*

/** Preserve the account catalog's order; never guess a paid API model name. */
object AccountModels {
    fun parse(root: JsonObject): List<ChatGptModel> = root["models"]?.jsonArray.orEmpty().mapNotNull {
        val obj = it as? JsonObject ?: return@mapNotNull null
        fun text(key: String) = (obj[key] as? JsonPrimitive)?.contentOrNull.orEmpty()
        if (text("visibility") != "list" || text("slug").isBlank()) null
        else ChatGptModel(text("slug"), text("display_name").ifBlank { text("slug") })
    }.distinctBy { it.id }
    fun select(models: List<ChatGptModel>, previous: String): String =
        previous.takeIf { id -> models.any { it.id == id } } ?: models.firstOrNull()?.id.orEmpty()
}
