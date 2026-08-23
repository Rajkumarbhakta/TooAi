package com.rkbapps.tooai.ui.screens.ai_writer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiWriterStateTest {

    @Test
    fun `hasSourceText is false for missing or whitespace-only text`() {
        assertFalse(AiWriterState().hasSourceText)
        assertFalse(AiWriterState(sourceText = "").hasSourceText)
        assertFalse(AiWriterState(sourceText = "   \n\t ").hasSourceText)
    }

    @Test
    fun `hasSourceText is true once the caller hands over text`() {
        assertTrue(AiWriterState(sourceText = "hello").hasSourceText)
    }
}