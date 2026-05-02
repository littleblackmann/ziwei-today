package com.zwds.today.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.data.repository.FamilyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val members: List<FamilyMemberEntity> = emptyList(),
    val selectedIndex: Int = 0
)

class HomeViewModel(
    familyRepository: FamilyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val membersFlow: StateFlow<List<FamilyMemberEntity>> = familyRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            membersFlow.collect { list ->
                val current = _uiState.value
                val newIndex = when {
                    list.isEmpty() -> 0
                    current.members.isEmpty() -> list.indexOfFirst { it.isPrimary }.coerceAtLeast(0)
                    else -> {
                        val currentId = current.members.getOrNull(current.selectedIndex)?.id
                        list.indexOfFirst { it.id == currentId }.takeIf { it >= 0 }
                            ?: list.indexOfFirst { it.isPrimary }.coerceAtLeast(0)
                    }
                }
                _uiState.update { it.copy(members = list, selectedIndex = newIndex) }
            }
        }
    }

    fun selectIndex(index: Int) {
        val current = _uiState.value
        if (index < 0 || index >= current.members.size) return
        _uiState.update { it.copy(selectedIndex = index) }
    }

    fun selectedMember(): FamilyMemberEntity? {
        val s = _uiState.value
        return s.members.getOrNull(s.selectedIndex)
    }
}
