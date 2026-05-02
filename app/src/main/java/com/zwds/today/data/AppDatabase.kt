package com.zwds.today.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zwds.today.data.dao.FamilyMemberDao
import com.zwds.today.data.dao.TopicHistoryDao
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.data.entity.TopicHistoryEntity

/**
 * AppDatabase
 *
 * 重要原則：
 *   - family_members 表必須走真 migration，不可破壞性重建（長者資料不能重輸）
 *   - topic_history 表是查詢結果快取兼歷史記錄
 *
 * Schema 版本變更時：
 *   1. 提升 [DB_VERSION]
 *   2. 為 family_members 表寫 Migration（其他表也要小心）
 *   3. 把 Migration 加到 Room.databaseBuilder().addMigrations(...)
 */
@Database(
    entities = [FamilyMemberEntity::class, TopicHistoryEntity::class],
    version = AppDatabase.DB_VERSION,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun topicHistoryDao(): TopicHistoryDao

    companion object {
        const val DB_VERSION = 2
        private const val DB_NAME = "zwds_today.db"

        /**
         * V1 → V2：
         *   - 移除字典表（star_dictionary）：改用 OpenAI 動態生成，不再離線打底
         *   - 新增 topic_history 表：兼歷史記錄與 LLM 結果快取
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `star_dictionary`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `topic_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`member_id` INTEGER NOT NULL, " +
                        "`date` TEXT NOT NULL, " +
                        "`topic` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`level` TEXT NOT NULL, " +
                        "`advice` TEXT NOT NULL, " +
                        "`focus_summary` TEXT NOT NULL, " +
                        "`tags` TEXT NOT NULL, " +
                        "`flow_palace_branch` TEXT NOT NULL, " +
                        "`flow_palace_stars` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_topic_history_member_id_date_topic` " +
                        "ON `topic_history` (`member_id`, `date`, `topic`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_topic_history_member_id` " +
                        "ON `topic_history` (`member_id`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_topic_history_date` " +
                        "ON `topic_history` (`date`)"
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
