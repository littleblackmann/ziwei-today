package com.zwds.today.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.auth.ChatGptSignInRequired
import com.zwds.today.data.auth.OAuthServiceError
import com.zwds.today.data.network.OpenAiException
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.data.repository.FamilyRepository
import com.zwds.today.domain.model.FortuneTopic
import com.zwds.today.domain.model.TopicFortune
import com.zwds.today.domain.usecase.GetTopicFortune
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

sealed interface TopicResultError {
    val diagnostic: String? get() = null
    data object SignInRequired : TopicResultError
    data class UsageLimit(val message: String, override val diagnostic: String? = null) : TopicResultError
    data class Network(val message: String, override val diagnostic: String? = null) : TopicResultError
    data class Unknown(val message: String) : TopicResultError
}
data class TopicResultUiState(
    val memberId: Long = 0L, val topic: FortuneTopic = FortuneTopic.OVERVIEW,
    val date: LocalDate = LocalDate.now(), val loading: Boolean = true,
    val fortune: TopicFortune? = null, val fromCache: Boolean = false,
    val error: TopicResultError? = null
)
class TopicResultViewModel(
    private val familyRepository: FamilyRepository,
    private val userPreferences: UserPreferences,
    private val getTopicFortune: GetTopicFortune
) : ViewModel() {
    private val _state = MutableStateFlow(TopicResultUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    fun load(memberId: Long, topic: FortuneTopic, forceRefresh: Boolean = false, date: LocalDate = LocalDate.now()) {
        val current = _state.value
        val same = current.memberId == memberId && current.topic == topic && current.date == date
        if (same && (loadJob?.isActive == true || (!forceRefresh && current.fortune != null))) return
        loadJob?.cancel()
        _state.value = TopicResultUiState(memberId = memberId, topic = topic, date = date)
        loadJob = viewModelScope.launch {
            try {
                val member = familyRepository.getById(memberId) ?: error("找不到家人資料")
                val school = userPreferences.ziHourSchool.first()
                val fromCache = !forceRefresh && getTopicFortune.hasCached(memberId, date, topic)
                val fortune = getTopicFortune(member, topic, date, school, forceRefresh)
                ensureActive()
                _state.update { it.copy(loading = false, fortune = fortune, fromCache = fromCache) }
            } catch (e: CancellationException) { throw e }
            catch (_: ChatGptSignInRequired) {
                _state.update { it.copy(loading = false, error = TopicResultError.SignInRequired) }
            } catch (e: OpenAiException) {
                _state.update { it.copy(loading = false, error = if (e.usageLimit)
                    TopicResultError.UsageLimit(e.message ?: "已達使用限制", e.diagnostic)
                    else TopicResultError.Network(e.message ?: "無法取得解讀", e.diagnostic)) }
            } catch (e: OAuthServiceError) {
                _state.update { it.copy(loading = false, error = if (e.status == 429)
                    TopicResultError.UsageLimit(e.message, e.diagnostic) else TopicResultError.Network(e.message, e.diagnostic)) }
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, error = TopicResultError.Unknown("無法載入此份解讀，請重試")) }
            }
        }
    }
    fun retry() { val s = _state.value; if (s.memberId != 0L) load(s.memberId, s.topic, date = s.date) }
    fun regenerate() { val s = _state.value; if (s.memberId != 0L) load(s.memberId, s.topic, forceRefresh = true, date = s.date) }
}
