package com.zwds.today.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.data.repository.FamilyRepository
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.domain.model.TopicFortune
import com.zwds.today.domain.usecase.GetTopicFortune
import com.zwds.today.domain.usecase.MissingApiKeyException
import com.zwds.today.data.network.OpenAiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface TopicResultError {
    object MissingApiKey : TopicResultError
    data class Network(val message: String) : TopicResultError
    data class Unknown(val message: String) : TopicResultError
}

data class TopicResultUiState(
    val memberId: Long = 0L,
    val topic: FortuneTopic = FortuneTopic.OVERVIEW,
    val loading: Boolean = true,
    val fortune: TopicFortune? = null,
    val fromCache: Boolean = false,
    val error: TopicResultError? = null
)

class TopicResultViewModel(
    private val familyRepository: FamilyRepository,
    private val userPreferences: UserPreferences,
    private val getTopicFortune: GetTopicFortune
) : ViewModel() {

    private val _state = MutableStateFlow(TopicResultUiState())
    val state: StateFlow<TopicResultUiState> = _state.asStateFlow()

    fun load(memberId: Long, topic: FortuneTopic, forceRefresh: Boolean = false) {
        val current = _state.value
        if (!forceRefresh && current.memberId == memberId && current.topic == topic && current.fortune != null) return
        viewModelScope.launch {
            _state.update {
                it.copy(memberId = memberId, topic = topic, loading = true, error = null)
            }
            try {
                val member = familyRepository.getById(memberId)
                    ?: throw IllegalStateException("找不到家人資料")
                val school = userPreferences.ziHourSchool.first()
                val fortune = getTopicFortune.invoke(
                    member, topic, LocalDate.now(), school, forceRefresh = forceRefresh
                )
                _state.update {
                    it.copy(loading = false, fortune = fortune, fromCache = !forceRefresh && existedBefore(memberId, topic))
                }
            } catch (e: MissingApiKeyException) {
                _state.update { it.copy(loading = false, error = TopicResultError.MissingApiKey) }
            } catch (e: OpenAiException) {
                _state.update { it.copy(loading = false, error = TopicResultError.Network(e.message ?: "OpenAI 呼叫失敗")) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = TopicResultError.Unknown(e.message ?: "載入失敗")) }
            }
        }
    }

    fun retry() {
        val s = _state.value
        if (s.memberId != 0L) load(s.memberId, s.topic, forceRefresh = false)
    }

    fun regenerate() {
        val s = _state.value
        if (s.memberId != 0L) load(s.memberId, s.topic, forceRefresh = true)
    }

    private fun existedBefore(memberId: Long, topic: FortuneTopic): Boolean {
        // 簡化：若狀態原本沒有 fortune 就視為新生成；有則為快取命中
        // 真正的「快取 vs 新生成」資訊由 UseCase 內部處理，此值僅供 UI 提示
        return false
    }
}
