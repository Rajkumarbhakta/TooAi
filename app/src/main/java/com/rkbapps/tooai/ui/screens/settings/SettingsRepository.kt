package com.rkbapps.tooai.ui.screens.settings

import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.db.dao.ChatDao
import com.rkbapps.tooai.ui.theme.AccentColor
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val prefManager: PreferenceManager,
    private val chatDao: ChatDao,
) {
    val isSystemTheme = prefManager.getBooleanPreference(PreferenceManager.IS_USE_SYSTEM_THEME,true)
    val isDarkTheme = prefManager.getBooleanPreference(PreferenceManager.IS_DARK_THEME)
    val isDynamicColor = prefManager.getBooleanPreference(PreferenceManager.IS_DYNAMIC_COLOR,true)
    val accentColor = prefManager.getStringPreference(PreferenceManager.ACCENT_COLOR)
        .map { AccentColor.fromName(it) }

    suspend fun updateIsSystemTheme(value: Boolean)=prefManager.saveBooleanPreference(PreferenceManager.IS_USE_SYSTEM_THEME,value)
    suspend fun updateTheme(value:Boolean) = prefManager.saveBooleanPreference(PreferenceManager.IS_DARK_THEME,value)
    suspend fun updateDynamicColor(value: Boolean) = prefManager.saveBooleanPreference(PreferenceManager.IS_DYNAMIC_COLOR,value)
    suspend fun updateAccentColor(value: AccentColor) = prefManager.saveStringPreference(PreferenceManager.ACCENT_COLOR,value.name)

    suspend fun clearChatHistory() = chatDao.deleteAllSessions()

}
