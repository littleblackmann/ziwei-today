package com.zwds.today.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.repository.FamilyRepository
import com.zwds.today.data.repository.TopicHistoryRecord
import com.zwds.today.data.repository.TopicHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicHistoryUiState(
    val memberId: Long = 0L,
    val memberName: String = "",
    val records: List<TopicHistoryRecord> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class TopicHistoryViewModel(
    private val familyRepository: FamilyRepository,
    private val historyRepository: TopicHistoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TopicHistoryUiState())
    val state: StateFlow<TopicHistoryUiState> = _state.asStateFlow()

    fun load(memberId: Long) {
        if (_state.value.memberId == memberId && !_state.value.loading) return
        _state.update { it.copy(memberId = memberId, loading = true, error = null) }
        viewModelScope.launch {
            val member = familyRepository.getById(memberId)
            if (member == null) {
                _state.update { it.copy(loading = false, error = "找不到家人資料") }
                return@launch
            }
            _state.update { it.copy(memberName = member.name) }
        }
        viewModelScope.launch {
            historyRepository.observeByMember(memberId).collect { list ->
                _state.update { it.copy(records = list, loading = false) }
            }
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        historyRepository.deleteById(id)
    }
}
