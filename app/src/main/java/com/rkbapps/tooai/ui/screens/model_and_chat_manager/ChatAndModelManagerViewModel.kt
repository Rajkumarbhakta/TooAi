package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.db.entity.ChatSession
import com.rkbapps.tooai.db.entity.LlmModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@HiltViewModel
class ChatAndModelManagerViewModel @Inject constructor(
    private val repository: ChatAndModelManagerRepository,
) : ViewModel() {
    val llmModels = repository.llmModels.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chatSessions = repository.chatSessions.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    fun importModel(
        context: Context,
        fileSize: Long,
        fileName:String,
        uri: Uri,
        onDone:(path: String)-> Unit,
        onProgress:(progress: Float)-> Unit,
        onError:(message: String)-> Unit
    ){
        repository.importModel(
            context = context,
            scope = viewModelScope,
            fileSize = fileSize,
            fileName = fileName,
            uri = uri,
            onDone= onDone,
            onProgress = onProgress,
            onError = onError
        )
    }

    fun addModel(model: LlmModel){
        viewModelScope.launch(Dispatchers.IO) {
            repository.addNewModel(model = model)
        }
    }

    fun updateModel(model: LlmModel){
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateNewModel(model = model)
        }
    }

    fun deleteModel(model: LlmModel){
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteModel(model = model)
        }
    }

    fun deleteChat(chatSeason: ChatSession){
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteChatSeason(chatSeason)
        }
    }

    fun updateChat(chatSeason: ChatSession){
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateChatSeason(chatSeason)
        }

    }



}