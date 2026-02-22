package com.rkbapps.tooai.ui.screens.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rkbapps.tooai.MainActivity
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.navigation.IdType
import com.rkbapps.tooai.navigation.NavigationEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
): ViewModel() {
    
    val chatState: StateFlow<ChatState> = repository.chatState
    val llmModels = repository.llmModels.stateIn(
        viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    init {
        val route = MainActivity.backStack.lastOrNull()
        if (route!=null && route is NavigationEntry.AiChat){
            val id = route.id
            val type = route.type
            if(type== IdType.MODEL){
                initializeModel(id.toLong())
            }else{
                loadSession(id)
            }
        }else{
            Log.d("ChatViewModel","No route found")
        }
    }

    fun initializeModel(llmModel: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.initializeModel(llmModel)
        }
    }

    fun loadSession(sessionId: String,modelId: Long? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadSession(sessionId,modelId)
        }
    }

    fun sendMessage(message: String) {
        viewModelScope.launch {
            repository.sendMessage(message)
        }
    }


    override fun onCleared() {
        chatState.value.instance?.engine?.close()
        chatState.value.instance?.conversation?.close()
        super.onCleared()
    }

}
