package com.rkbapps.tooai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.BuildConfig
import com.rkbapps.tooai.ui.theme.AccentColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        SharingStarted.WhileSubscribed(5000),
        true
    )
    val isDarkTheme = repository.isDarkTheme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val isDynamicColor = repository.isDynamicColor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        true
    )
    val accentColor = repository.accentColor.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AccentColor.INDIGO
    )

    val appVersion = BuildConfig.VERSION_NAME

    fun updateIsSystemTheme(value: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateIsSystemTheme(value)
    }
    fun updateTheme(value:Boolean) =viewModelScope.launch(Dispatchers.IO) { repository.updateTheme(value) }
    fun updateDynamicColor(value: Boolean) = viewModelScope.launch(Dispatchers.IO) { repository.updateDynamicColor(value) }
    fun updateAccentColor(value: AccentColor) = viewModelScope.launch(Dispatchers.IO) { repository.updateAccentColor(value) }
    fun clearChatHistory() = viewModelScope.launch(Dispatchers.IO) { repository.clearChatHistory() }


}