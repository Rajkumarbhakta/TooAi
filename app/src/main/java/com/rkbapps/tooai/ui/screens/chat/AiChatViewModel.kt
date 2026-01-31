package com.rkbapps.tooai.ui.screens.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


@HiltViewModel
class AiChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {



    private val _status = MutableStateFlow<Int?>(null)
    val status = _status.asStateFlow()
















}