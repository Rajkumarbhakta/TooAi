package com.rkbapps.tooai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.ui.theme.AccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
): ViewModel() {

    val isSystemTheme = repository.isSystemTheme.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        true
    )
    val isDarkTheme = repository.isDarkTheme.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        false
    )

    val isDynamicColor = repository.isDynamicColor.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        true
    )
    val accentColor = repository.accentColor.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        AccentColor.INDIGO
    )

    val appVersion = BuildConfig.VERSION_NAME

    fun updateIsSystemTheme(value: Boolean)=viewModelScope.launch {
        repository.updateIsSystemTheme(value)
    }
    fun updateTheme(value:Boolean) =viewModelScope.launch { repository.updateTheme(value) }
    fun updateDynamicColor(value: Boolean) = viewModelScope.launch { repository.updateDynamicColor(value) }
    fun updateAccentColor(value: AccentColor) = viewModelScope.launch { repository.updateAccentColor(value) }
    fun clearChatHistory() = viewModelScope.launch { repository.clearChatHistory() }


}