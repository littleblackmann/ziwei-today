package com.zwds.today.data.repository

import com.zwds.today.data.dao.TopicHistoryDao
import com.zwds.today.data.entity.TopicHistoryEntity
import com.zwds.today.domain.model.FortuneLevel
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.domain.model.PlacedStar
import com.zwds.today.domain.model.TopicFortune
import com.zwds.today.domain.model.EarthlyBranch
import com.zwds.today.domain.model.Star
import com.zwds.today.domain.model.Transformation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TopicHistoryRepository(
    private val dao: TopicHistoryDao
) {

    suspend fun find(memberId: Long, date: LocalDate, topic: FortuneTopic): TopicFortune? {
        val entity = dao.findOne(memberId, date.format(ISO_DATE), topic.name) ?: return null
        return entity.toDomain(topic)
    }

    suspend fun save(fortune: TopicFortune, focusSummary: String): Long {
        val entity = TopicHistoryEntity(
            memberId = fortune.familyMemberId,
            date = fortune.date.format(ISO_DATE),
            topic = fortune.topic.name,
            title = fortune.title,
            level = fortune.level.name,
            advice = fortune.advice,
            focusSummary = focusSummary,
            tags = fortune.tags.joinToString("、"),
            flowPalaceBranch = fortune.flowPalaceBranch.name,
            flowPalaceStars = encodeStars(fortune.flowPalaceStars)
        )
        return dao.upsert(entity)
    }

    fun observeByMember(memberId: Long): Flow<List<TopicHistoryRecord>> =
        dao.observeByMember(memberId).map { list -> list.map { it.toRecord() } }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteByMember(memberId: Long) = dao.deleteByMember(memberId)

    private fun TopicHistoryEntity.toDomain(topic: FortuneTopic): TopicFortune = TopicFortune(
        familyMemberId = memberId,
        familyMemberName = "",
        date = LocalDate.parse(date, ISO_DATE),
        topic = topic,
        flowPalaceBranch = runCatching { EarthlyBranch.valueOf(flowPalaceBranch) }
            .getOrDefault(EarthlyBranch.ZI),
        flowPalaceStars = decodeStars(flowPalaceStars),
        title = title,
        level = runCatching { FortuneLevel.valueOf(level) }.getOrDefault(FortuneLevel.NEUTRAL),
        focusSummary = focusSummary,
        advice = advice,
        tags = tags.split("、").filter { it.isNotBlank() }
    )

    private fun TopicHistoryEntity.toRecord(): TopicHistoryRecord = TopicHistoryRecord(
        id = id,
        memberId = memberId,
        date = LocalDate.parse(date, ISO_DATE),
        topic = runCatching { FortuneTopic.valueOf(topic) }.getOrDefault(FortuneTopic.OVERVIEW),
        title = title,
        level = runCatching { FortuneLevel.valueOf(level) }.getOrDefault(FortuneLevel.NEUTRAL),
        focusSummary = focusSummary,
        advice = advice,
        tags = tags.split("、").filter { it.isNotBlank() }
    )

    private fun encodeStars(stars: List<PlacedStar>): String =
        stars.joinToString("|") { p ->
            val t = p.transformation?.name ?: ""
            "${p.star.name}:$t"
        }

    private fun decodeStars(encoded: String): List<PlacedStar> = encoded
        .split("|")
        .filter { it.isNotBlank() }
        .mapNotNull { token ->
            val parts = token.split(":", limit = 2)
            val starName = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val star = runCatching { Star.valueOf(starName) }.getOrNull() ?: return@mapNotNull null
            val transformation = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let {
                runCatching { Transformation.valueOf(it) }.getOrNull()
            }
            PlacedStar(star, transformation)
        }

    companion object {
        private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}

/** UI 用的歷史記錄列表項 */
data class TopicHistoryRecord(
    val id: Long,
    val memberId: Long,
    val date: LocalDate,
    val topic: FortuneTopic,
    val title: String,
    val level: FortuneLevel,
    val focusSummary: String,
    val advice: String,
    val tags: List<String>
)
