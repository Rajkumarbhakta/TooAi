package com.rkbapps.tooai.ui.screens.settings

import com.rkbapps.tooai.db.PreferenceManager
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val prefManager: PreferenceManager,
) {
    val isSystemTheme = prefManager.getBooleanPreference(PreferenceManager.IS_USE_SYSTEM_THEME,true)
    val isDarkTheme = prefManager.getBooleanPreference(PreferenceManager.IS_DARK_THEME)

    suspend fun updateIsSystemTheme(value: Boolean)=prefManager.saveBooleanPreference(PreferenceManager.IS_USE_SYSTEM_THEME,value)
    suspend fun updateTheme(value:Boolean) = prefManager.saveBooleanPreference(PreferenceManager.IS_DARK_THEME,value)

}