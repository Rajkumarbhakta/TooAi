package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiWriterViewModel @Inject constructor(
    private val repository: AiWriterRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _state = MutableStateFlow(AiWriterState())
    val state = _state.asStateFlow()

    /** Held across actions so a second action doesn't reload the model. Closed in [onCleared]. */
    private var engine: Engine? = null
    private var engineModelId: Long? = null
    private var conversation: Conversation? = null
    private var generationJob: Job? = null

    private var started = false

    init {
        viewModelScope.launch {
            repository.models.collect { models ->
                val lastUsedId = preferenceManager
                    .getLongPreferenceSynchronous(PreferenceManager.LAST_USED_MODEL_ID, null)
                _state.update { current ->
                    val selected = current.selectedModel?.let { previous ->
                        models.firstOrNull { it.id == previous.id }
                    } ?: models.firstOrNull { it.id == lastUsedId } ?: models.firstOrNull()
                    current.copy(models = models, selectedModel = selected)
                }
            }
        }
    }

    /** Called once by the activity with what the calling app handed over. */
    fun start(sourceText: String, canReplace: Boolean) {
        if (started) return
        started = true
        _state.update { it.copy(sourceText = sourceText, canReplace = canReplace) }
    }

    fun selectModel(model: LlmModel) {
        if (_state.value.selectedModel?.id == model.id) return
        // The loaded engine belongs to the old model — drop it so the next action reloads.
        releaseConversation()
        releaseEngine()
        _state.update { it.copy(selectedModel = model, stage = AiWriterState.Stage.Idle) }
        viewModelScope.launch {
            preferenceManager.saveLongPreference(PreferenceManager.LAST_USED_MODEL_ID, model.id)
        }
    }

    fun runPrompt(prompt: Prompts) {
        _state.update { it.copy(activePrompt = prompt) }
        run(prompt.prompt + _state.value.sourceText)
    }

    fun retry() {
        val prompt = _state.value.activePrompt ?: return
        run(prompt.prompt + _state.value.sourceText)
    }

    fun stop() {
        conversation?.let { repository.cancel(it) }
        generationJob?.cancel()
        generationJob = null
        // Keep whatever was generated so far — partial output is often still usable.
        _state.update { current ->
            val partial = (current.stage as? AiWriterState.Stage.Generating)?.partial.orEmpty()
            if (partial.isBlank()) {
                current.copy(stage = AiWriterState.Stage.Idle)
            } else {
                current.copy(stage = AiWriterState.Stage.Done(partial.trim()))
            }
        }
    }

    /** Back to the action grid without discarding the loaded engine. */
    fun reset() {
        generationJob?.cancel()
        generationJob = null
        _state.update { it.copy(stage = AiWriterState.Stage.Idle, activePrompt = null) }
    }

    private fun run(fullPrompt: String) {
        val model = _state.value.selectedModel ?: return
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val activeEngine = ensureEngine(model) ?: return@launch

            releaseConversation()
            val newConversation = repository.newConversation(activeEngine, model)
                .getOrElse { error ->
                    _state.update {
                        it.copy(stage = AiWriterState.Stage.Error(error.readableMessage()))
                    }
                    return@launch
                }
            conversation = newConversation

            _state.update { it.copy(stage = AiWriterState.Stage.Generating("")) }
            try {
                repository.generate(newConversation, fullPrompt).collect { partial ->
                    _state.update { it.copy(stage = AiWriterState.Stage.Generating(partial)) }
                }
                val result = (_state.value.stage as? AiWriterState.Stage.Generating)
                    ?.partial
                    ?.let(::sanitize)
                    .orEmpty()
                _state.update {
                    if (result.isBlank()) {
                        it.copy(stage = AiWriterState.Stage.Error("The model returned nothing."))
                    } else {
                        it.copy(stage = AiWriterState.Stage.Done(result))
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(stage = AiWriterState.Stage.Error(e.readableMessage())) }
            }
        }
    }

    /** Returns the loaded engine, loading it first if this is the first action. */
    private suspend fun ensureEngine(model: LlmModel): Engine? {
        engine?.takeIf { engineModelId == model.id }?.let { return it }

        _state.update { it.copy(stage = AiWriterState.Stage.LoadingModel) }
        val loaded = repository.loadEngine(model).getOrElse { error ->
            _state.update { it.copy(stage = AiWriterState.Stage.Error(error.readableMessage())) }
            return null
        }
        engine = loaded
        engineModelId = model.id
        return loaded
    }

    private fun releaseConversation() {
        conversation?.let { repository.close(it) }
        conversation = null
    }

    private fun releaseEngine() {
        engine?.let { repository.close(it) }
        engine = null
        engineModelId = null
    }

    /**
     * Small models often ignore the "no fences, no quotes" instruction. Since the result is pasted
     * back into a plain text field, strip the common wrappers rather than shipping them.
     */
    private fun sanitize(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```").substringAfter('\n', "").trim()
            text = text.removeSuffix("```").trim()
        }
        if (text.length > 1 && text.startsWith('"') && text.endsWith('"')) {
            text = text.substring(1, text.length - 1).trim()
        }
        return text
    }

    override fun onCleared() {
        super.onCleared()
        // A leaked engine keeps gigabytes resident after the sheet is gone.
        releaseConversation()
        releaseEngine()
    }
}