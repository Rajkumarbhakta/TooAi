package com.rkbapps.tooai.ui.screens.sticker

import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// The size chat apps expect for a static sticker. Every coordinate in a StickerDocument is in
// this space, so the preview and the exported file are drawn from the same numbers.
const val STICKER_SIZE = 512

// Room kept around a freshly placed cut-out, so its outline isn't clipped by the canvas edge.
private const val DEFAULT_MARGIN = 24

enum class StickerOutline(val label: String, val width: Int) {
    NONE("Off", 0),
    THIN("Thin", 8),
    THICK("Thick", 16)
}

// Android's generic font families. A missing one falls back to the default typeface.
enum class StickerFont(val label: String, val family: String) {
    CLASSIC("Classic", "sans-serif"),
    SERIF("Serif", "serif"),
    MONO("Mono", "monospace"),
    CASUAL("Casual", "casual")
}

data class StickerPoint(val x: Float, val y: Float)

// A layer is drawn around its own centre: scaled, then rotated (degrees), then moved to the centre.
data class LayerTransform(
    val centerX: Float = STICKER_SIZE / 2f,
    val centerY: Float = STICKER_SIZE / 2f,
    val scale: Float = 1f,
    val rotation: Float = 0f
) {
    // The centre stays on the canvas, so a layer can't be dragged somewhere it can't be reached.
    fun adjusted(
        panX: Float,
        panY: Float,
        zoom: Float,
        rotationDelta: Float,
        scaleRange: ClosedFloatingPointRange<Float>
    ) = LayerTransform(
        centerX = (centerX + panX).coerceIn(0f, STICKER_SIZE.toFloat()),
        centerY = (centerY + panY).coerceIn(0f, STICKER_SIZE.toFloat()),
        scale = (scale * zoom).coerceIn(scaleRange),
        rotation = rotation + rotationDelta
    )

    // A canvas point in the layer's own space: unscaled, unrotated, origin at the layer's centre.
    fun toLocal(x: Float, y: Float): StickerPoint {
        val radians = Math.toRadians(-rotation.toDouble())
        val dx = x - centerX
        val dy = y - centerY
        return StickerPoint(
            x = ((dx * cos(radians) - dy * sin(radians)) / scale).toFloat(),
            y = ((dx * sin(radians) + dy * cos(radians)) / scale).toFloat()
        )
    }
}

sealed interface StickerItem {
    val id: Long
}

// Colors are ARGB ints, which is what the renderer's Paint takes.
data class TextItem(
    override val id: Long,
    val text: String,
    val color: Int,
    val font: StickerFont,
    val isEmoji: Boolean,
    val transform: LayerTransform
) : StickerItem

data class StrokeItem(
    override val id: Long,
    val points: List<StickerPoint>,
    val color: Int,
    val width: Float
) : StickerItem

// Everything that makes up the sticker besides the cut-out's pixels. Immutable, so undo is a
// stack of earlier documents.
data class StickerDocument(
    val image: LayerTransform = LayerTransform(),
    val outline: StickerOutline = StickerOutline.THIN,
    val darkOutline: Boolean = false,
    // Drawn over the cut-out, oldest first.
    val items: List<StickerItem> = emptyList()
)

// Fits the cut-out inside the canvas, centred.
internal fun defaultImageTransform(contentWidth: Int, contentHeight: Int): LayerTransform {
    val box = (STICKER_SIZE - DEFAULT_MARGIN * 2).toFloat()
    return LayerTransform(scale = min(box / contentWidth, box / contentHeight))
}

// Half extents with slop: small layers stay easy to hit.
internal fun StickerPoint.isWithin(halfWidth: Float, halfHeight: Float, slop: Float): Boolean =
    x in -(halfWidth + slop)..(halfWidth + slop) && y in -(halfHeight + slop)..(halfHeight + slop)
