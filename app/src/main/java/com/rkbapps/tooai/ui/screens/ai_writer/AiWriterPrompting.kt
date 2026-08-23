package com.rkbapps.tooai.ui.screens.ai_writer

import com.google.ai.edge.litertlm.SamplerConfig
import com.rkbapps.tooai.db.entity.LlmModel

/**
 * How the AI Writer asks the model: prompt composition, response cleanup, and per-page decoding
 * policy.
 *
 * Kept out of the ViewModel so all of it is unit-testable without an Android runtime — everything
 * here is a plain function over data.
 */

/** Separates the instruction from the user's text so the model can tell them apart. */
private const val SOURCE_LABEL = "Text:"

/**
 * Builds the string actually sent to the model.
 *
 * Every writer action goes through here, so the tone pages and the free-form page compose their
 * prompts identically. Concatenating instruction and selection directly — which the tone pages used
 * to do — leaves the model guessing where the instruction ends, and a selection that itself reads
 * like an instruction can hijack the request.
 *
 * [source] is null or blank on the free-form page with "use selected text" switched off.
 */
internal fun composePrompt(instruction: String, source: String?): String {
    val head = instruction.trim()
    return if (source.isNullOrBlank()) head else "$head\n\n$SOURCE_LABEL\n${source.trim()}"
}

/**
 * Small models often ignore the "no fences, no quotes" instruction. Since the result is pasted
 * back into a plain text field, strip the common wrappers rather than shipping them.
 */
internal fun sanitize(raw: String): String {
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

/**
 * Ceilings applied on the Proofread page. Proofread-Grammar tells the model to "correct only the
 * spelling, grammar, and punctuation … do not rephrase anything", but models import at temperature
 * 1.0, so the sampler was contradicting the instruction and the page rewrote text it was told to
 * leave alone.
 *
 * These are deliberately ceilings, not fixed values: full greedy decoding (`topK = 1`,
 * `temperature = 0`) would make every run byte-identical, which turns Regenerate into a no-op that
 * appends duplicate variants. Clamping also leaves a user who deliberately configured something
 * lower exactly where they put it.
 */
private const val PROOFREAD_MAX_TEMPERATURE = 0.3
private const val PROOFREAD_MAX_TOP_K = 40
private const val PROOFREAD_MAX_TOP_P = 0.9

/**
 * Decoding settings for one writer action.
 *
 * Only Proofread deviates from the model's own settings. Rewrite and free-form writing are exactly
 * the cases where those settings are the point, and summaries promise a *format* ("bullet points
 * (3-5)", "~50 words") rather than fidelity — that is instruction-following, not sampler entropy —
 * so damping them would cost variety on Regenerate to buy nothing.
 */
internal fun samplerFor(page: AiWriterPages, model: LlmModel): SamplerConfig =
    if (page == AiWriterPages.PROOFREAD) {
        legalSampler(
            topK = model.topK.coerceAtMost(PROOFREAD_MAX_TOP_K),
            topP = model.topP.coerceAtMost(PROOFREAD_MAX_TOP_P),
            temperature = model.temperature.coerceAtMost(PROOFREAD_MAX_TEMPERATURE)
        )
    } else {
        legalSampler(topK = model.topK, topP = model.topP, temperature = model.temperature)
    }

/**
 * [SamplerConfig]'s constructor rejects `topK < 1`, `topP` outside `[0, 1]`, and a negative
 * temperature with an [IllegalArgumentException]. Values come from a user-editable Room row, so
 * clamp here rather than surfacing a construction failure as an opaque generation error.
 */
private fun legalSampler(topK: Int, topP: Double, temperature: Double) = SamplerConfig(
    topK = topK.coerceAtLeast(1),
    topP = topP.coerceIn(0.0, 1.0),
    temperature = temperature.coerceAtLeast(0.0)
)