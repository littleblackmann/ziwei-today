package com.zwds.today.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zwds.today.data.entity.TopicHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicHistoryDao {

    @Query(
        """
        SELECT * FROM topic_history
        WHERE member_id = :memberId AND date = :date AND topic = :topic
        LIMIT 1
        """
    )
    suspend fun findOne(memberId: Long, date: String, topic: String): TopicHistoryEntity?

    @Query(
        """
        SELECT * FROM topic_history
        WHERE member_id = :memberId
        ORDER BY date DESC, created_at DESC
        """
    )
    fun observeByMember(memberId: Long): Flow<List<TopicHistoryEntity>>

    @Query(
        """
        SELECT * FROM topic_history
        ORDER BY date DESC, created_at DESC
        LIMIT :limit
        """
    )
    suspend fun recent(limit: Int = 100): List<TopicHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TopicHistoryEntity): Long

    @Query("DELETE FROM topic_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM topic_history WHERE member_id = :memberId")
    suspend fun deleteByMember(memberId: Long)
}
