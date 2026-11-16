package com.rkbapps.tooai.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelBackendTest {

    @Test
    fun `stored names map back to their backend`() {
        assertEquals(ModelBackend.CPU, ModelBackend.of("CPU"))
        assertEquals(ModelBackend.GPU, ModelBackend.of("GPU"))
    }

    @Test
    fun `anything unrecognised runs on CPU`() {
        assertEquals(ModelBackend.CPU, ModelBackend.of(null))
        assertEquals(ModelBackend.CPU, ModelBackend.of(""))
        assertEquals(ModelBackend.CPU, ModelBackend.of("NPU"))
        assertEquals(ModelBackend.CPU, ModelBackend.of("gpu"))
    }
}
