package com.rkbapps.tooai.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.BuildConfig
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

    val appVersion = BuildConfig.VERSION_NAME

    fun updateIsSystemTheme(value: Boolean)=viewModelScope.launch {
        repository.updateIsSystemTheme(value)
    }
    fun updateTheme(value:Boolean) =viewModelScope.launch { repository.updateTheme(value) }


}