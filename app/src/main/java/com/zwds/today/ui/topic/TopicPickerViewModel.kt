package com.zwds.today.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TopicPickerUiState(
    val memberId: Long = 0L,
    val memberName: String = "",
    val loading: Boolean = true,
    val error: String? = null
)

class TopicPickerViewModel(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TopicPickerUiState())
    val state: StateFlow<TopicPickerUiState> = _state.asStateFlow()

    fun load(memberId: Long) {
        if (_state.value.memberId == memberId && !_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(memberId = memberId, loading = true, error = null) }
            val member = familyRepository.getById(memberId)
            if (member == null) {
                _state.update { it.copy(loading = false, error = "找不到家人資料") }
            } else {
                _state.update {
                    it.copy(memberName = member.name, loading = false, error = null)
                }
            }
        }
    }
}
