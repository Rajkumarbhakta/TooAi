package com.rkbapps.tooai.ui.screens.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rkbapps.tooai.activity.MainActivity
import com.rkbapps.tooai.navigation.IdType
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.utils.Prompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    private var modelJob: Job? = null

    // Frees the model after viewModelScope is gone. See onCleared.
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        val route = MainActivity.backStack.lastOrNull()
        if (route!=null && route is NavigationEntry.AiChat){
            val id = route.id
            val type = route.type
            repository.markModelLoading()
            if(type== IdType.MODEL){
                runModelTask { repository.startWithModel(id.toLong()) }
            }else{
                runModelTask { repository.loadSession(id) }
            }
        }else{
            Log.d("ChatViewModel","No route found")
        }
    }

    // Model work runs one task at a time. A load can't be interrupted, so a second one started
    // alongside it would put two models in memory at once.
    private fun runModelTask(task: suspend () -> Unit) {
        val previous = modelJob
        modelJob = viewModelScope.launch(Dispatchers.IO) {
            previous?.join()
            task()
        }
    }

    // The UI only offers a switch while nothing is loading or generating. The checks here stop a
    // double tap from queueing a second switch behind the first.
    fun switchModel(modelId: Long) {
        val state = chatState.value
        if (state.modelInitializingStatus.isLoading || state.isChatRunning) return
        repository.markModelLoading()
        runModelTask {
            awaitGenerationStopped()
            repository.switchModel(modelId)
        }
    }

    // The model can only be freed once its decode has unwound.
    private suspend fun awaitGenerationStopped() {
        val job = generationJob ?: return
        if (job.isActive) {
            repository.cancelGeneration()
            job.cancel()
        }
        job.join()
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

    fun startNewChat() = runModelTask {
        generationJob?.join()
        repository.startNewChat()
    }

    fun selectAPredefinePrompt(prompt: Prompts?){
        repository.selectAPredefinePrompt(prompt)
    }



    // viewModelScope is already cancelled here, but neither a model load nor a native decode stops
    // at once. Freeing the engine under either crashes the native library, so the close waits for
    // both. A load that finishes after this frees its own engine (see ChatRepository.openInstance).
    override fun onCleared() {
        repository.cancelGeneration()
        val modelTask = modelJob
        val generation = generationJob
        teardownScope.launch {
            modelTask?.join()
            withTimeoutOrNull(TEARDOWN_JOIN_TIMEOUT_MS) { generation?.join() }
            repository.releaseModel()
        }
        super.onCleared()
    }

    companion object {
        // Caps the wait for a decode that is slow to notice its cancel, so a wedged one can't hold
        // the model forever.
        private const val TEARDOWN_JOIN_TIMEOUT_MS = 3_000L
    }

}
