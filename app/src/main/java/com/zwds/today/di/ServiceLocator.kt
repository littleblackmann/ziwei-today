package com.zwds.today.di

import android.content.Context
import com.zwds.today.data.AppDatabase
import com.zwds.today.data.network.ReadingClient
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.data.repository.FamilyRepository
import com.zwds.today.data.repository.TopicHistoryRepository
import com.zwds.today.domain.usecase.GetTopicFortune
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 手寫 Service Locator
 */
object ServiceLocator {

    lateinit var appContext: Context
        private set

    val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    val database: AppDatabase by lazy { AppDatabase.create(appContext) }

    val familyRepository: FamilyRepository by lazy {
        FamilyRepository(database, database.familyMemberDao())
    }

    val topicHistoryRepository: TopicHistoryRepository by lazy {
        TopicHistoryRepository(database.topicHistoryDao())
    }

    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }

    val accountRepository by lazy { com.zwds.today.data.auth.ChatGptAuthRepository(appContext) }
    val updateRepository by lazy { com.zwds.today.data.update.UpdateRepository(appContext) }
    val readingClient: ReadingClient by lazy { ReadingClient(accountRepository) }

    val getTopicFortune: GetTopicFortune by lazy {
        GetTopicFortune(readingClient, topicHistoryRepository)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
