package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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


    val isSystemTheme = preferenceManager.getBooleanPreference(PreferenceManager.IS_USE_SYSTEM_THEME, true)
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val isDarkTheme = preferenceManager
        .getBooleanPreference(PreferenceManager.IS_DARK_THEME, false)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Held across actions so a second action doesn't reload the model. Closed in [onCleared]. */
    private var engine: Engine? = null
    private var engineModelId: Long? = null
    private var conversation: Conversation? = null
    private var generationJob: Job? = null

    /**
     * Results accumulated on the current generation page. Held here rather than in [Stage.Done] so
     * a regenerate can append to it while the UI is showing [Stage.Generating].
     */
    private val variants = mutableListOf<String>()

    /** The last composed prompt, replayed by [regenerate] and [retry]. */
    private var lastFullPrompt: String? = null

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


    /**
     * Navigates to [page]. Generation pages start from a clean variant list and immediately run
     * their default prompt, so the user lands on a result rather than an empty card.
     */
    fun onCurrentPageChange(page: AiWriterPages) {
        // Leaving a page mid-generation must stop the model, not just detach the UI from it.
        cancelGeneration()
        variants.clear()
        lastFullPrompt = null

        val prompt = page.defaultPrompt()
        _state.update {
            it.copy(
                currentPage = page,
                activePrompt = prompt,
                stage = AiWriterState.Stage.Idle
            )
        }
        // The free-form page has nothing to run until the user types something.
        if (page.isGenerationPage() && prompt != null) {
            run(prompt.prompt + _state.value.sourceText)
        }
    }

    /** Re-runs whatever was last sent and appends the result as a new variant. */
    fun regenerate() {
        lastFullPrompt?.let { run(it) }
    }

    fun updatePromptText(text: String) {
        _state.update { it.copy(promptText = text) }
    }

    fun setUseSourceAsContext(enabled: Boolean) {
        _state.update { it.copy(useSourceAsContext = enabled) }
    }

    /** Runs the free-form instruction typed on the Write anything page. */
    fun runFreeform() {
        val current = _state.value
        val instruction = current.promptText.trim()
        if (instruction.isBlank()) return
        // No tone chips on this page, so nothing owns activePrompt.
        _state.update { it.copy(activePrompt = null) }
        run(
            composeFreeform(
                instruction = instruction,
                source = current.sourceText,
                useContext = current.useSourceAsContext
            )
        )
    }

    private fun composeFreeform(instruction: String, source: String, useContext: Boolean): String =
        if (useContext && source.isNotBlank()) {
            "$instruction\n\nText:\n$source"
        } else {
            instruction
        }

    fun showVariant(index: Int) {
        if (index !in variants.indices) return
        _state.update {
            it.copy(stage = AiWriterState.Stage.Done(variants.toList(), index))
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
        cancelGeneration()
        variants.clear()
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
        lastFullPrompt?.let { run(it) }
    }

    fun stop() {
        cancelGeneration()
        // Keep whatever was generated so far — partial output is often still usable.
        val partial = (_state.value.stage as? AiWriterState.Stage.Generating)?.partial.orEmpty()
        if (partial.isBlank()) {
            _state.update { it.copy(stage = restoredStage()) }
        } else {
            commitVariant(sanitize(partial))
        }
    }

    /**
     * Takes the fully composed prompt rather than a [Prompts], so the tone-chip pages and the
     * free-form page share one path — and so [regenerate] and [retry] can replay either.
     */
    private fun run(fullPrompt: String) {
        val model = _state.value.selectedModel ?: return
        lastFullPrompt = fullPrompt
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
                repository.generate(newConversation, fullPrompt)
                    .collect { partial ->
                        _state.update { it.copy(stage = AiWriterState.Stage.Generating(partial)) }
                    }
                val result = (_state.value.stage as? AiWriterState.Stage.Generating)
                    ?.partial
                    ?.let(::sanitize)
                    .orEmpty()
                if (result.isBlank()) {
                    _state.update {
                        it.copy(stage = AiWriterState.Stage.Error("The model returned nothing."))
                    }
                } else {
                    commitVariant(result)
                }
            } catch (e: CancellationException) {
                // Stop / back / dismiss cancel this job on purpose. Swallowing it here would let
                // the cancellation land as an "error" on top of the state the caller just set.
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(stage = AiWriterState.Stage.Error(e.readableMessage())) }
            }
        }
    }

    /** Appends a finished result and moves the pager to it. */
    private fun commitVariant(result: String) {
        variants += result
        _state.update {
            it.copy(stage = AiWriterState.Stage.Done(variants.toList(), variants.lastIndex))
        }
    }

    /** Where to land when a run produces nothing — the previous variants, or an empty page. */
    private fun restoredStage(): AiWriterState.Stage =
        if (variants.isEmpty()) {
            AiWriterState.Stage.Idle
        } else {
            AiWriterState.Stage.Done(variants.toList(), variants.lastIndex)
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

    /**
     * Stops an in-flight generation.
     *
     * Cancelling the coroutine alone only unsubscribes from the flow — the native LiteRT decode
     * keeps running and burning CPU, so the conversation has to be cancelled too.
     */
    private fun cancelGeneration() {
        conversation?.let { repository.cancel(it) }
        generationJob?.cancel()
        generationJob = null
    }

    /**
     * Called when the sheet is dismissed. Releases the model straight away rather than waiting for
     * [onCleared] to run after the activity finishes — the engine holds gigabytes.
     */
    fun dismiss() {
        cancelGeneration()
        releaseConversation()
        releaseEngine()
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
        // Safety net for destruction paths that never went through dismiss(). Idempotent: both
        // releases null out their handles, and cancelling a finished conversation is a no-op.
        cancelGeneration()
        releaseConversation()
        releaseEngine()
    }
}