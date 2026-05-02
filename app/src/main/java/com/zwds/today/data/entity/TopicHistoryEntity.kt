package com.zwds.today.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 主題運勢查詢記錄
 *
 * 兼具兩個用途：
 *   1. 「使用者歷史記錄」：可在 UI 列出某位家人哪些日子問過哪些主題
 *   2. 「LLM 結果快取」：同一 (memberId, date, topic) 已有記錄時直接讀取，不重複呼叫 OpenAI
 *
 * 注意：date 以 ISO 字串儲存（yyyy-MM-dd），便於排序與查詢。
 */
@Entity(
    tableName = "topic_history",
    indices = [
        Index(value = ["member_id", "date", "topic"], unique = true),
        Index(value = ["member_id"]),
        Index(value = ["date"])
    ]
)
data class TopicHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    @ColumnInfo(name = "member_id") val memberId: Long,

    /** ISO 日期字串：yyyy-MM-dd */
    @ColumnInfo(name = "date") val date: String,

    /** [com.zwds.today.domain.model.FortuneTopic] 的 enum 名稱 */
    @ColumnInfo(name = "topic") val topic: String,

    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "advice") val advice: String,
    @ColumnInfo(name = "focus_summary") val focusSummary: String,

    /** 用「、」分隔 */
    @ColumnInfo(name = "tags") val tags: String,

    @ColumnInfo(name = "flow_palace_branch") val flowPalaceBranch: String,
    @ColumnInfo(name = "flow_palace_stars") val flowPalaceStars: String,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
