package com.rkbapps.tooai.ui.screens.ai_writer

import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.PredefinePrompts
import com.rkbapps.tooai.utils.TypeOfPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the pure prompt/sampler logic. Everything here is plain Kotlin over data classes, so it
 * runs on the JVM without Robolectric.
 */
class AiWriterPromptingTest {

    private fun model(
        topK: Int = 64,
        topP: Double = 0.9,
        temperature: Double = 1.0
    ) = LlmModel(
        id = 1L,
        name = "test.litertlm",
        displayName = "Test",
        sizeInBytes = 0L,
        path = "/tmp/test.litertlm",
        fileLocation = "",
        maxTokens = 1024,
        topK = topK,
        topP = topP,
        temperature = temperature,
        createdAt = 0L
    )

    // --- composePrompt -------------------------------------------------------------------------

    @Test
    fun `composePrompt returns the instruction alone when there is no source`() {
        assertEquals("Write a haiku", composePrompt("Write a haiku", null))
        assertEquals("Write a haiku", composePrompt("Write a haiku", ""))
        assertEquals("Write a haiku", composePrompt("Write a haiku", "   \n  "))
    }

    @Test
    fun `composePrompt delimits the source with a labelled block`() {
        assertEquals(
            "Summarize this\n\nText:\nhello world",
            composePrompt("Summarize this", "hello world")
        )
    }

    @Test
    fun `composePrompt trims both sides`() {
        assertEquals(
            "Summarize this\n\nText:\nhello",
            composePrompt("  Summarize this  ", "\n hello \n")
        )
    }

    /** The predefined prompts end in ": ", which must collapse to a bare colon before the block. */
    @Test
    fun `composePrompt collapses the trailing space of a predefined prompt`() {
        val composed = composePrompt(PredefinePrompts.REWRITE_FORMAL, "hi there")
        assertEquals(
            "Rewrite the following text using a formal tone:\n\nText:\nhi there",
            composed
        )
    }

    // --- sanitize ------------------------------------------------------------------------------

    @Test
    fun `sanitize strips a fenced block`() {
        assertEquals("hello\nworld", sanitize("```\nhello\nworld\n```"))
    }

    @Test
    fun `sanitize strips a fenced block with a language tag`() {
        assertEquals("val x = 1", sanitize("```kotlin\nval x = 1\n```"))
    }

    @Test
    fun `sanitize strips matched surrounding quotes`() {
        assertEquals("hello", sanitize("\"hello\""))
    }

    @Test
    fun `sanitize leaves plain text alone`() {
        assertEquals("hello world", sanitize("  hello world  "))
    }

    @Test
    fun `sanitize leaves an unmatched quote alone`() {
        assertEquals("\"hello", sanitize("\"hello"))
    }

    // --- samplerFor ----------------------------------------------------------------------------

    @Test
    fun `samplerFor clamps proofread down from the import defaults`() {
        val sampler = samplerFor(AiWriterPages.PROOFREAD, model())
        assertEquals(40, sampler.topK)
        assertEquals(0.9, sampler.topP, 0.0)
        assertEquals(0.3, sampler.temperature, 0.0)
    }

    /** Clamp, don't replace — a user who deliberately went lower keeps their setting. */
    @Test
    fun `samplerFor leaves an already-conservative proofread model untouched`() {
        val sampler = samplerFor(AiWriterPages.PROOFREAD, model(topK = 5, topP = 0.5, temperature = 0.1))
        assertEquals(5, sampler.topK)
        assertEquals(0.5, sampler.topP, 0.0)
        assertEquals(0.1, sampler.temperature, 0.0)
    }

    /**
     * Proofread must never go fully greedy: identical output on every run would make Regenerate a
     * no-op that appends duplicate variants.
     */
    @Test
    fun `samplerFor keeps proofread stochastic`() {
        val sampler = samplerFor(AiWriterPages.PROOFREAD, model())
        assertTrue("topK must stay above 1", sampler.topK > 1)
        assertTrue("temperature must stay above 0", sampler.temperature > 0.0)
    }

    @Test
    fun `samplerFor passes the model settings through on every other page`() {
        val m = model()
        listOf(AiWriterPages.POLISH, AiWriterPages.SUMMARIZE, AiWriterPages.PROMPT, AiWriterPages.HOME)
            .forEach { page ->
                val sampler = samplerFor(page, m)
                assertEquals("topK for $page", m.topK, sampler.topK)
                assertEquals("topP for $page", m.topP, sampler.topP, 0.0)
                assertEquals("temperature for $page", m.temperature, sampler.temperature, 0.0)
            }
    }

    /** SamplerConfig throws on out-of-range values, so a bad DB row must be legalized, not fatal. */
    @Test
    fun `samplerFor legalizes an out-of-range model row`() {
        val sampler = samplerFor(AiWriterPages.POLISH, model(topK = 0, topP = 1.5, temperature = -1.0))
        assertEquals(1, sampler.topK)
        assertEquals(1.0, sampler.topP, 0.0)
        assertEquals(0.0, sampler.temperature, 0.0)
    }

    // --- AiWriterPages helpers -----------------------------------------------------------------

    @Test
    fun `promptType maps only the generation pages`() {
        assertEquals(TypeOfPrompt.Rewrite, AiWriterPages.POLISH.promptType())
        assertEquals(TypeOfPrompt.Summary, AiWriterPages.SUMMARIZE.promptType())
        assertEquals(TypeOfPrompt.Proofread, AiWriterPages.PROOFREAD.promptType())
        assertNull(AiWriterPages.HOME.promptType())
        assertNull(AiWriterPages.PROMPT.promptType())
    }

    @Test
    fun `isGenerationPage agrees with promptType`() {
        AiWriterPages.entries.forEach { page ->
            assertEquals(page.promptType() != null, page.isGenerationPage())
        }
    }

    @Test
    fun `every generation page offers chips and a default drawn from them`() {
        AiWriterPages.entries.filter { it.isGenerationPage() }.forEach { page ->
            val prompts = page.prompts()
            assertTrue("$page has no chips", prompts.isNotEmpty())
            val default = page.defaultPrompt()
            assertNotNull("$page has no default", default)
            assertTrue("$page default is not one of its chips", prompts.contains(default))
        }
    }

    @Test
    fun `non-generation pages offer no chips`() {
        assertTrue(AiWriterPages.HOME.prompts().isEmpty())
        assertTrue(AiWriterPages.PROMPT.prompts().isEmpty())
        assertNull(AiWriterPages.HOME.defaultPrompt())
        assertNull(AiWriterPages.PROMPT.defaultPrompt())
    }

    @Test
    fun `proofread starts on the least invasive option`() {
        assertEquals(PredefinePrompts.PROOFREAD_GRAMMAR, AiWriterPages.PROOFREAD.defaultPrompt()?.prompt)
    }
}