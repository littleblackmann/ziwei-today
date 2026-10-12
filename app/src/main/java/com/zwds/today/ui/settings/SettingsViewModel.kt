package com.zwds.today.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.data.prefs.ThemeMode
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.domain.model.ZiHourSchool
import com.zwds.today.ui.theme.FontScale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val fontScale: FontScale = FontScale.Standard,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val ziHourSchool: ZiHourSchool = ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY,
    val showFullChart: Boolean = false,

)

class SettingsViewModel(
    private val userPreferences: UserPreferences,
    private val auth: com.zwds.today.data.auth.ChatGptAuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.fontScaleFlow.collect { v -> _state.update { it.copy(fontScale = v) } }
        }
        viewModelScope.launch {
            userPreferences.themeMode.collect { v -> _state.update { it.copy(themeMode = v) } }
        }
        viewModelScope.launch {
            userPreferences.ziHourSchool.collect { v -> _state.update { it.copy(ziHourSchool = v) } }
        }
        viewModelScope.launch {
            userPreferences.showFullChart.collect { v -> _state.update { it.copy(showFullChart = v) } }
        }

    }

    fun setFontScale(scale: FontScale) = viewModelScope.launch {
        userPreferences.setFontScale(scale)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        userPreferences.setThemeMode(mode)
    }

    fun setZiHourSchool(school: ZiHourSchool) = viewModelScope.launch {
        userPreferences.setZiHourSchool(school)
    }

    fun setShowFullChart(show: Boolean) = viewModelScope.launch {
        userPreferences.setShowFullChart(show)
    }

    val account = auth.state
    private var loginJob: kotlinx.coroutines.Job? = null
    fun signIn(different: Boolean = false) {
        if (loginJob?.isActive == true || account.value.busy) return
        loginJob = com.zwds.today.di.ServiceLocator.applicationScope.launch { auth.signIn(different) }
    }
    fun cancelSignIn() { loginJob?.cancel(); auth.cancelSignIn() }
    fun signOut() = com.zwds.today.di.ServiceLocator.applicationScope.launch { auth.signOut() }
    fun refreshStatus() = viewModelScope.launch { auth.refreshStatus() }
    fun acknowledgePlanWelcome() = auth.acknowledgePlanWelcome()
}
