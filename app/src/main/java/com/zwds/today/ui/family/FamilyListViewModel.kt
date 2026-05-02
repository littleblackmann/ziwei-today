package com.zwds.today.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.data.repository.FamilyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FamilyListViewModel(
    private val familyRepository: FamilyRepository
) : ViewModel() {

    val members: StateFlow<List<FamilyMemberEntity>> = familyRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setPrimary(id: Long) {
        viewModelScope.launch {
            familyRepository.setPrimary(id)
        }
    }
}
