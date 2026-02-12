package com.rkbapps.tooai.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.navigation.NavigationEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
): ViewModel() {
    
    val chatState: StateFlow<ChatState> = repository.chatState



    fun initializeModel(llmModel: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeModel(llmModel)
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }
}
