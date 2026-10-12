package com.zwds.today.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zwds.today.domain.model.ZiHourSchool
import com.zwds.today.ui.theme.FontScale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * App 偏好設定（DataStore Preferences）
 */
class UserPreferences(private val context: Context) {

    private val dataStore = context.appDataStore

    val fontScaleFlow: Flow<FontScale> = dataStore.data.map { prefs ->
        FontScale.fromName(prefs[KEY_FONT_SCALE])
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromName(prefs[KEY_THEME_MODE])
    }

    val ziHourSchool: Flow<ZiHourSchool> = dataStore.data.map { prefs ->
        when (prefs[KEY_ZI_HOUR_SCHOOL]) {
            "SAME" -> ZiHourSchool.LATE_BELONGS_TO_SAME_DAY
            else -> ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY
        }
    }

    val disclaimerAccepted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DISCLAIMER_ACCEPTED] ?: false
    }

    val showFullChart: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SHOW_FULL_CHART] ?: false
    }

    suspend fun setFontScale(scale: FontScale) =
        dataStore.edit { it[KEY_FONT_SCALE] = scale.name }

    suspend fun setThemeMode(mode: ThemeMode) =
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }

    suspend fun setZiHourSchool(school: ZiHourSchool) {
        val name = when (school) {
            ZiHourSchool.LATE_BELONGS_TO_SAME_DAY -> "SAME"
            ZiHourSchool.LATE_BELONGS_TO_NEXT_DAY -> "NEXT"
        }
        dataStore.edit { it[KEY_ZI_HOUR_SCHOOL] = name }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) =
        dataStore.edit { it[KEY_DISCLAIMER_ACCEPTED] = accepted }

    suspend fun setShowFullChart(show: Boolean) =
        dataStore.edit { it[KEY_SHOW_FULL_CHART] = show }

    /** Remove obsolete credentials without touching family data or display settings. */
    suspend fun removeLegacyApiKey() = dataStore.edit { it.remove(stringPreferencesKey("openai_api_key")) }

    companion object {
        private val KEY_FONT_SCALE = stringPreferencesKey("font_scale")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_ZI_HOUR_SCHOOL = stringPreferencesKey("zi_hour_school")
        private val KEY_DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        private val KEY_SHOW_FULL_CHART = booleanPreferencesKey("show_full_chart")
    }
}

private val Context.appDataStore by preferencesDataStore(name = "zwds_prefs")

enum class ThemeMode(val cnName: String) {
    SYSTEM("跟隨系統"),
    LIGHT("永遠淺色"),
    DARK("永遠深色");

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}
