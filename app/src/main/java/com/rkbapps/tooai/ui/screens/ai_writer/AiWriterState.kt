package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.annotation.StringRes
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.PredefinePrompts
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.TypeOfPrompt

enum class AiWriterPages {
    HOME,
    PROMPT,
    POLISH,
    SUMMARIZE,
    PROOFREAD
}

/**
 * Each generation page is just a filter over [PredefinePrompts.listOfPrompts] — the tone chips a
 * page offers are the sub-types of its [TypeOfPrompt]. Returns null for pages that do not generate.
 */
fun AiWriterPages.promptType(): TypeOfPrompt? = when (this) {
    AiWriterPages.POLISH -> TypeOfPrompt.Rewrite
    AiWriterPages.SUMMARIZE -> TypeOfPrompt.Summary
    AiWriterPages.PROOFREAD -> TypeOfPrompt.Proofread
    else -> null
}

fun AiWriterPages.prompts(): List<Prompts> {
    val type = promptType() ?: return emptyList()
    return PredefinePrompts.listOfPrompts.filter { it.type == type }
}

/** Run automatically when the page opens, so the user sees a result without another tap. */
fun AiWriterPages.defaultPrompt(): Prompts? = when (this) {
    AiWriterPages.POLISH -> prompts().firstOrNull { it.prompt == PredefinePrompts.REWRITE_FORMAL }
    AiWriterPages.SUMMARIZE ->
        prompts().firstOrNull { it.prompt == PredefinePrompts.SUMMARY_SHORT_PARAGRAPH }

    // "Proofread" implies fixing mistakes, not rewriting — start with the least invasive option.
    AiWriterPages.PROOFREAD ->
        prompts().firstOrNull { it.prompt == PredefinePrompts.PROOFREAD_GRAMMAR }

    else -> null
} ?: prompts().firstOrNull()

/** True for pages that run the model and therefore show the generation UI. */
fun AiWriterPages.isGenerationPage(): Boolean = promptType() != null

@StringRes
fun AiWriterPages.titleRes(): Int = when (this) {
    AiWriterPages.HOME -> R.string.ai_writer
    AiWriterPages.PROMPT -> R.string.ai_writer_page_prompt
    AiWriterPages.POLISH -> R.string.ai_writer_page_polish
    AiWriterPages.SUMMARIZE -> R.string.ai_writer_page_summarize
    AiWriterPages.PROOFREAD -> R.string.ai_writer_page_proofread
}

data class AiWriterState(
    /** Text handed over by the calling app. */
    val sourceText: String = "",
    /** True only for ACTION_PROCESS_TEXT on a writable field — otherwise Copy is the only option. */
    val canReplace: Boolean = false,
    val models: List<LlmModel> = emptyList(),
    val selectedModel: LlmModel? = null,
    val activePrompt: Prompts? = null,
    /** Free-form instruction typed on the Write anything page. */
    val promptText: String = "",
    /** Whether that instruction is sent with the selected text appended as context. */
    val useSourceAsContext: Boolean = true,
    val stage: Stage = Stage.Idle,
    val currentPage: AiWriterPages = AiWriterPages.HOME,
) {
    /** Models are loaded asynchronously; only treat the list as empty once it has actually loaded. */
    val hasModels: Boolean get() = models.isNotEmpty()

    /**
     * The tone pages only transform text the caller handed over, so they are unavailable without
     * it. The free-form page still works — it can write from an instruction alone.
     */
    val hasSourceText: Boolean get() = sourceText.isNotBlank()

    sealed interface Stage {
        /** Nothing running — the home page, or a generation page before its first run. */
        data object Idle : Stage

        data object LoadingModel : Stage

        data class Generating(val partial: String) : Stage

        /**
         * Every regenerate or tone change appends a variant, so the user can page back to an
         * earlier result instead of losing it.
         */
        data class Done(val variants: List<String>, val index: Int) : Stage {
            val current: String get() = variants.getOrElse(index) { "" }
            val hasPrev: Boolean get() = index > 0
            val hasNext: Boolean get() = index < variants.lastIndex
        }

        data class Error(val message: String) : Stage
    }
}