package com.rkbapps.tooai.ui.screens.ai_writer

import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts

data class AiWriterState(
    /** Text handed over by the calling app. */
    val sourceText: String = "",
    /** True only for ACTION_PROCESS_TEXT on a writable field — otherwise Copy is the only option. */
    val canReplace: Boolean = false,
    val models: List<LlmModel> = emptyList(),
    val selectedModel: LlmModel? = null,
    val activePrompt: Prompts? = null,
    val stage: Stage = Stage.Idle
) {
    /** Models are loaded asynchronously; only treat the list as empty once it has actually loaded. */
    val hasModels: Boolean get() = models.isNotEmpty()

    sealed interface Stage {
        /** Showing the action grid. */
        data object Idle : Stage

        data object LoadingModel : Stage

        data class Generating(val partial: String) : Stage

        data class Done(val result: String) : Stage

        data class Error(val message: String) : Stage
    }
}