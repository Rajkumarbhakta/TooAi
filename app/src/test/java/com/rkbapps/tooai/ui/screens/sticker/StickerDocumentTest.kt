package com.rkbapps.tooai.ui.screens.sticker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerDocumentTest {

    private val tolerance = 0.001f

    @Test
    fun `a wide cut-out is fitted to the canvas width inside the margin and centred`() {
        val transform = defaultImageTransform(1000, 500)
        // A 24 px margin on each side leaves a 464 box.
        assertEquals(0.464f, transform.scale, tolerance)
        assertEquals(256f, transform.centerX, tolerance)
        assertEquals(256f, transform.centerY, tolerance)
        assertEquals(0f, transform.rotation, tolerance)
    }

    @Test
    fun `a tall cut-out is fitted to the canvas height`() {
        assertEquals(0.5f, defaultImageTransform(300, 928).scale, tolerance)
    }

    @Test
    fun `a small cut-out is scaled up to fill the canvas`() {
        assertEquals(11.6f, defaultImageTransform(40, 20).scale, tolerance)
    }

    @Test
    fun `adjusted applies pan, zoom and rotation`() {
        val moved = LayerTransform(100f, 100f, 1f, 10f).adjusted(20f, -30f, 2f, 15f, 0.5f..4f)
        assertEquals(LayerTransform(120f, 70f, 2f, 25f), moved)
    }

    @Test
    fun `adjusted keeps the scale inside its range`() {
        val range = 0.5f..4f
        assertEquals(4f, LayerTransform(scale = 3f).adjusted(0f, 0f, 10f, 0f, range).scale, tolerance)
        assertEquals(0.5f, LayerTransform(scale = 1f).adjusted(0f, 0f, 0.01f, 0f, range).scale, tolerance)
    }

    @Test
    fun `adjusted keeps the centre on the canvas`() {
        val moved = LayerTransform(500f, 10f).adjusted(100f, -100f, 1f, 0f, 0.5f..4f)
        assertEquals(512f, moved.centerX, tolerance)
        assertEquals(0f, moved.centerY, tolerance)
    }

    @Test
    fun `toLocal undoes the translation and scale`() {
        val local = LayerTransform(centerX = 100f, centerY = 200f, scale = 2f).toLocal(140f, 180f)
        assertEquals(20f, local.x, tolerance)
        assertEquals(-10f, local.y, tolerance)
    }

    @Test
    fun `toLocal undoes the rotation`() {
        // A layer turned a quarter clockwise: the canvas point below its centre is on its own
        // right-hand side.
        val local = LayerTransform(centerX = 100f, centerY = 100f, rotation = 90f).toLocal(100f, 150f)
        assertEquals(50f, local.x, tolerance)
        assertEquals(0f, local.y, tolerance)
    }

    @Test
    fun `isWithin counts the slop on every side`() {
        assertTrue(StickerPoint(54f, -24f).isWithin(halfWidth = 50f, halfHeight = 20f, slop = 5f))
        assertFalse(StickerPoint(56f, 0f).isWithin(halfWidth = 50f, halfHeight = 20f, slop = 5f))
        assertFalse(StickerPoint(0f, 26f).isWithin(halfWidth = 50f, halfHeight = 20f, slop = 5f))
    }
}
