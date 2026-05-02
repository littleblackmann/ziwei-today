package com.zwds.today.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zwds.today.astro.lunar.LunarCalendar
import com.zwds.today.data.entity.FamilyMemberEntity
import com.zwds.today.data.prefs.UserPreferences
import com.zwds.today.data.repository.FamilyRepository
import com.zwds.today.domain.model.Gender
import com.zwds.today.domain.model.SolarBirth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FamilyEditUiState(
    val mode: EditMode = EditMode.CREATE,
    val name: String = "",
    val isLunarInput: Boolean = false,
    /** 國曆年（無論輸入模式為何，最終存的都是國曆）*/
    val solarYear: Int = 1955,
    val solarMonth: Int = 6,
    val solarDay: Int = 15,
    /** 農曆暫存（僅在 isLunarInput=true 時用作 UI 顯示）*/
    val lunarYear: Int = 1955,
    val lunarMonth: Int = 5,
    val lunarDay: Int = 1,
    val isLeapMonth: Boolean = false,
    val hour24: Int = 6,
    val gender: Gender = Gender.MALE,
    val isPrimary: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
    /** 每次 loadForEdit 完成後 +1，讓 Screen 用 key() 強制重建滾輪到正確位置 */
    val dataVersion: Int = 0
)

enum class EditMode { CREATE, EDIT }

class FamilyEditViewModel(
    private val familyRepository: FamilyRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(FamilyEditUiState())
    val state: StateFlow<FamilyEditUiState> = _state.asStateFlow()

    private var editingId: Long? = null

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val member = familyRepository.getById(id) ?: return@launch
            editingId = id
            _state.update {
                it.copy(
                    mode = EditMode.EDIT,
                    name = member.name,
                    isLunarInput = member.isLunarInput,
                    solarYear = member.solarYear,
                    solarMonth = member.solarMonth,
                    solarDay = member.solarDay,
                    hour24 = member.hour24,
                    gender = if (member.gender == "FEMALE") Gender.FEMALE else Gender.MALE,
                    isPrimary = member.isPrimary,
                    dataVersion = it.dataVersion + 1
                )
            }
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun setIsLunarInput(lunar: Boolean) = _state.update { it.copy(isLunarInput = lunar) }
    fun setSolarDate(year: Int, month: Int, day: Int) =
        _state.update { it.copy(solarYear = year, solarMonth = month, solarDay = day) }
    fun setLunarDate(year: Int, month: Int, day: Int, isLeap: Boolean) =
        _state.update { it.copy(lunarYear = year, lunarMonth = month, lunarDay = day, isLeapMonth = isLeap) }
    fun setHour(hour: Int) = _state.update { it.copy(hour24 = hour) }
    fun setGender(gender: Gender) = _state.update { it.copy(gender = gender) }
    fun setPrimary(primary: Boolean) = _state.update { it.copy(isPrimary = primary) }

    /**
     * 若使用者用農曆輸入，存檔前要轉成國曆
     */
    private fun resolveSolarBirth(state: FamilyEditUiState): Result<SolarBirth> = runCatching {
        if (state.isLunarInput) {
            val solarDate = LunarCalendar.lunarToSolar(
                state.lunarYear,
                state.lunarMonth,
                state.lunarDay,
                state.isLeapMonth
            )
            SolarBirth(solarDate.year, solarDate.monthValue, solarDate.dayOfMonth, state.hour24)
        } else {
            SolarBirth(state.solarYear, state.solarMonth, state.solarDay, state.hour24)
        }
    }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(error = "請填寫稱呼") }
            return
        }

        val solarResult = resolveSolarBirth(current)
        val solar = solarResult.getOrElse {
            _state.update { it.copy(error = "日期無效：${it.error ?: ""}") }
            return
        }

        // 驗證日期合理（生於 1900-01-31 之後）
        try {
            LocalDate.of(solar.year, solar.month, solar.day)
        } catch (e: Exception) {
            _state.update { it.copy(error = "日期不存在") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                val entity = FamilyMemberEntity(
                    id = editingId ?: 0L,
                    name = current.name.trim(),
                    solarYear = solar.year,
                    solarMonth = solar.month,
                    solarDay = solar.day,
                    hour24 = solar.hour24,
                    isLunarInput = current.isLunarInput,
                    gender = current.gender.name,
                    isPrimary = current.isPrimary
                )
                if (current.mode == EditMode.CREATE) {
                    familyRepository.insert(entity)
                } else {
                    familyRepository.update(entity)
                }
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: Exception) {
                _state.update { it.copy(saving = false, error = e.message ?: "儲存失敗") }
            }
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch {
            val member = familyRepository.getById(id) ?: return@launch
            familyRepository.delete(member)
            onDone()
        }
    }

    fun consumeSaved() = _state.update { it.copy(saved = false) }
    fun consumeError() = _state.update { it.copy(error = null) }
}
