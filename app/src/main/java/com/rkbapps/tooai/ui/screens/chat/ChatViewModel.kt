package com.rkbapps.tooai.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.activity.MainActivity
import com.rkbapps.tooai.navigation.IdType
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.utils.Prompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    private var generationJob: Job? = null

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
        val previousGeneration = generationJob
        generationJob = viewModelScope.launch {
            previousGeneration?.join()
            repository.sendMessage(message)
        }
    }

    // Cancelling the coroutine alone leaves the native decode running.
    fun stopGeneration() {
        repository.cancelGeneration()
        generationJob?.cancel()
    }

    fun startNewChat() {
        viewModelScope.launch(Dispatchers.IO) {
            generationJob?.join()
            repository.startNewChat()
        }
    }

    fun selectAPredefinePrompt(prompt: Prompts?){
        repository.selectAPredefinePrompt(prompt)
    }



    override fun onCleared() {
        chatState.value.instance?.engine?.close()
        chatState.value.instance?.conversation?.close()
        super.onCleared()
    }

}
