package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withTimeoutOrNull
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

    /**
     * Frees the native handles after the decode coroutine has unwound. Separate from
     * [viewModelScope] because it must outlive the ViewModel — see [dismiss].
     */
    private val teardownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Held across actions so a second action doesn't reload the model. Closed in [dismiss]. */
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
     *
     * Refuses to open a generation page with nothing to work on — those pages exist only to
     * transform the caller's selection, and running one anyway would send a dangling instruction
     * and let the model invent its own subject. The home page already disables those cards, so this
     * is a guard rather than a path a user can reach.
     */
    fun onCurrentPageChange(page: AiWriterPages) {
        if (page.isGenerationPage() && !_state.value.hasSourceText) return

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
            run(composePrompt(prompt.prompt, _state.value.sourceText))
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
            composePrompt(
                instruction = instruction,
                source = current.sourceText.takeIf { current.useSourceAsContext }
            )
        )
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
        // The loaded engine belongs to the old model — drop it so the next action reloads. Goes
        // through the same deferred teardown as dismiss: switching models mid-generation would
        // otherwise free the engine out from under a running decode.
        releaseNativeResources()
        variants.clear()
        _state.update { it.copy(selectedModel = model, stage = AiWriterState.Stage.Idle) }
        viewModelScope.launch {
            preferenceManager.saveLongPreference(PreferenceManager.LAST_USED_MODEL_ID, model.id)
        }
    }

    fun runPrompt(prompt: Prompts) {
        if (!_state.value.hasSourceText) return
        _state.update { it.copy(activePrompt = prompt) }
        run(composePrompt(prompt.prompt, _state.value.sourceText))
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
        val model = _state.value.selectedModel
        if (model == null) {
            _state.update { it.copy(stage = AiWriterState.Stage.Error("No model selected.")) }
            return
        }
        // Resolved here rather than inside the coroutine so a page change mid-load cannot swap the
        // policy out from under a run that has already started.
        val sampler = samplerFor(_state.value.currentPage, model)
        lastFullPrompt = fullPrompt

        // Starting a second run while one is in flight — a double-tapped chip or Regenerate — used
        // to SIGSEGV inside liblitertlm_jni: cancelling the coroutine only unsubscribes from the
        // flow, so releaseConversation() below could close a Conversation whose native decode was
        // still reading it. Stop the native process first, then wait for the old collector to
        // unwind, and only then let anything free it.
        val previous = generationJob
        conversation?.let { repository.cancel(it) }
        previous?.cancel()

        generationJob = viewModelScope.launch {
            previous?.join()

            val activeEngine = ensureEngine(model) ?: return@launch

            releaseConversation()
            val newConversation = repository.newConversation(activeEngine, sampler)
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
     *
     * The job reference is deliberately kept: [run] joins it before freeing the conversation, and
     * clearing it here would let a page change slip a close past that wait. A finished job costs
     * nothing to hold.
     */
    private fun cancelGeneration() {
        conversation?.let { repository.cancel(it) }
        generationJob?.cancel()
    }

    /**
     * Called when the sheet is dismissed. Releases the model rather than waiting for [onCleared] to
     * run after the activity finishes — the engine holds gigabytes.
     *
     * Closing cannot happen inline: cancelling the coroutine and calling `cancelProcess()` do not
     * synchronously stop the native decode, so freeing the Conversation here crashed
     * liblitertlm_jni with a null dereference when the sheet was dismissed mid-generation. The
     * handles are detached immediately and freed once the decode coroutine has unwound.
     */
    fun dismiss() = releaseNativeResources()

    /**
     * Stops generation and frees the engine and conversation, deferring the actual `close()` until
     * the decode coroutine has unwound.
     */
    private fun releaseNativeResources() {
        val job = generationJob
        val closingConversation = conversation
        val closingEngine = engine

        // Tell the native side to stop before anything is freed.
        closingConversation?.let { repository.cancel(it) }
        job?.cancel()

        // Detach first, so a re-entrant call (onCleared after dismiss) cannot free them twice.
        generationJob = null
        conversation = null
        engine = null
        engineModelId = null

        if (closingConversation == null && closingEngine == null) return

        // Deliberately not viewModelScope: on dismiss the activity is already finishing, and a
        // cancelled teardown would strand the engine's gigabytes for the life of the process.
        teardownScope.launch {
            withTimeoutOrNull(TEARDOWN_JOIN_TIMEOUT_MS) { job?.join() }
            closingConversation?.let { repository.close(it) }
            closingEngine?.let { repository.close(it) }
        }
    }

    /**
     * Only safe from inside [run], which has already joined the previous decode coroutine. Every
     * other caller must go through [releaseNativeResources].
     */
    private fun releaseConversation() {
        conversation?.let { repository.close(it) }
        conversation = null
    }

    override fun onCleared() {
        super.onCleared()
        // Safety net for destruction paths that never went through dismiss(). Idempotent — after a
        // dismiss the handles are already null, so this does nothing.
        //
        // teardownScope is intentionally left running: its only job is to free the native handles,
        // and cancelling it here would strand them. It completes on its own and is then collectable.
        dismiss()
    }

    companion object {
        /**
         * How long teardown waits for the decode coroutine to unwind before freeing anyway. The
         * native process has already been cancelled by then; this only covers a decode that is slow
         * to notice, and the cap stops a wedged one from holding the engine forever.
         */
        private const val TEARDOWN_JOIN_TIMEOUT_MS = 3_000L
    }
}