package com.rkbapps.tooai.ui.screens.sticker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withSave
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

// WhatsApp's limit for a static sticker. Other apps accept more, so this is the safe ceiling.
private const val MAX_STICKER_BYTES = 100 * 1024

// Segmenter edges fade out over a few faint pixels. Counting them would leave a visible margin.
private const val ALPHA_THRESHOLD = 16
private const val OUTLINE_STEPS = 36

// Twice the sticker size: enough to pinch the cut-out up without it going soft, and small enough
// to redraw on every frame of a gesture.
private const val MAX_CONTENT_EDGE = 1024

// Text size at scale 1, in sticker pixels.
private const val BASE_TEXT_SIZE = 64f
private const val TEXT_STROKE_RATIO = 0.16f

// Touch slop around a layer, in sticker pixels.
private const val HIT_SLOP = 20f

// The trimmed cut-out and its shape. The silhouette is what the outline is stamped from.
class StickerContent(val bitmap: Bitmap) {
    val silhouette: Bitmap = bitmap.extractAlpha()
}

// Crops away the transparent surround. Null when nothing in the bitmap is visible.
internal fun trimToContent(bitmap: Bitmap): Bitmap? {
    val row = IntArray(bitmap.width)
    var left = bitmap.width
    var right = -1
    var top = -1
    var bottom = -1
    for (y in 0 until bitmap.height) {
        bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
        var rowHasContent = false
        for (x in row.indices) {
            if (row[x] ushr 24 > ALPHA_THRESHOLD) {
                if (x < left) left = x
                if (x > right) right = x
                rowHasContent = true
            }
        }
        if (rowHasContent) {
            if (top < 0) top = y
            bottom = y
        }
    }
    if (top < 0) return null
    return Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
}

// What the editor works on: the cut-out trimmed to its subject and capped in size.
internal fun prepareContent(cutout: Bitmap): Bitmap? {
    val trimmed = trimToContent(cutout) ?: return null
    val longEdge = max(trimmed.width, trimmed.height)
    if (longEdge <= MAX_CONTENT_EDGE) return trimmed
    val factor = MAX_CONTENT_EDGE.toFloat() / longEdge
    return trimmed.scale(
        (trimmed.width * factor).roundToInt().coerceAtLeast(1),
        (trimmed.height * factor).roundToInt().coerceAtLeast(1)
    )
}

// The one drawing routine. The editor's preview and the exported file both go through it, so
// what is saved is what was on screen. The canvas must already be in sticker space.
internal fun drawSticker(
    canvas: Canvas,
    content: StickerContent,
    document: StickerDocument,
    activeStroke: StrokeItem? = null
) {
    drawCutout(canvas, content, document)
    document.items.forEach { item ->
        when (item) {
            is StrokeItem -> drawStroke(canvas, item)
            is TextItem -> drawText(canvas, item)
        }
    }
    activeStroke?.let { drawStroke(canvas, it) }
}

private fun drawCutout(canvas: Canvas, content: StickerContent, document: StickerDocument) {
    val transform = document.image
    val matrix = Matrix().apply {
        postTranslate(-content.bitmap.width / 2f, -content.bitmap.height / 2f)
        postScale(transform.scale, transform.scale)
        postRotate(transform.rotation)
        postTranslate(transform.centerX, transform.centerY)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val outlineWidth = document.outline.width
    if (outlineWidth > 0) {
        paint.color = if (document.darkOutline) Color.BLACK else Color.WHITE
        // Stamping the silhouette around a circle grows it by the outline width on every side,
        // whatever the cut-out's own scale and rotation.
        repeat(OUTLINE_STEPS) { step ->
            val angle = 2 * PI * step / OUTLINE_STEPS
            canvas.withSave {
                translate((cos(angle) * outlineWidth).toFloat(), (sin(angle) * outlineWidth).toFloat())
                drawBitmap(content.silhouette, matrix, paint)
            }
        }
        paint.color = Color.BLACK
    }
    canvas.drawBitmap(content.bitmap, matrix, paint)
}

private fun drawStroke(canvas: Canvas, stroke: StrokeItem) {
    val points = stroke.points
    if (points.isEmpty()) return
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = stroke.width
        color = stroke.color
    }
    if (points.size == 1) {
        canvas.drawPoint(points[0].x, points[0].y, paint)
        return
    }
    // Curving through the midpoints smooths the corners a finger's sample rate leaves.
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            quadTo(previous.x, previous.y, (previous.x + current.x) / 2, (previous.y + current.y) / 2)
        }
        lineTo(points.last().x, points.last().y)
    }
    canvas.drawPath(path, paint)
}

private val typefaces = HashMap<StickerFont, Typeface>()

private fun textPaint(item: TextItem) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = BASE_TEXT_SIZE
    textAlign = Paint.Align.CENTER
    color = item.color
    typeface = typefaces.getOrPut(item.font) { Typeface.create(item.font.family, Typeface.BOLD) }
}

private fun drawText(canvas: Canvas, item: TextItem) {
    val paint = textPaint(item)
    val lines = item.text.split('\n')
    val lineHeight = paint.fontSpacing
    // A contrasting edge keeps a caption readable on any chat background. Emoji have their own
    // colours and no outline to draw.
    val edge = if (item.isEmoji) null else Paint(paint).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = BASE_TEXT_SIZE * TEXT_STROKE_RATIO
        color = if (Color.luminance(item.color) > 0.5f) Color.BLACK else Color.WHITE
    }
    canvas.withSave {
        translate(item.transform.centerX, item.transform.centerY)
        rotate(item.transform.rotation)
        scale(item.transform.scale, item.transform.scale)
        var baseline = -(lines.size - 1) * lineHeight / 2f - (paint.ascent() + paint.descent()) / 2f
        lines.forEach { line ->
            edge?.let { drawText(line, 0f, baseline, it) }
            drawText(line, 0f, baseline, paint)
            baseline += lineHeight
        }
    }
}

// Unscaled bounds around the item's own centre.
private fun textBounds(item: TextItem): RectF {
    val paint = textPaint(item)
    val lines = item.text.split('\n')
    val halfWidth = lines.maxOf { paint.measureText(it) } / 2f + BASE_TEXT_SIZE * TEXT_STROKE_RATIO
    val halfHeight = lines.size * paint.fontSpacing / 2f
    return RectF(-halfWidth, -halfHeight, halfWidth, halfHeight)
}

internal fun hitsText(item: TextItem, x: Float, y: Float): Boolean {
    val bounds = textBounds(item)
    return item.transform.toLocal(x, y)
        .isWithin(bounds.right, bounds.bottom, HIT_SLOP / item.transform.scale)
}

// Editor-only: the dashed frame around the selected text. Never part of the exported sticker.
internal fun drawSelection(canvas: Canvas, item: TextItem, color: Int) {
    val scale = item.transform.scale
    val bounds = textBounds(item).apply { inset(-8f / scale, -4f / scale) }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f / scale
        pathEffect = DashPathEffect(floatArrayOf(12f / scale, 8f / scale), 0f)
        this.color = color
    }
    canvas.withSave {
        translate(item.transform.centerX, item.transform.centerY)
        rotate(item.transform.rotation)
        scale(scale, scale)
        drawRoundRect(bounds, 12f / scale, 12f / scale, paint)
    }
}

internal fun renderSticker(content: StickerContent, document: StickerDocument): Bitmap {
    val sticker = createBitmap(STICKER_SIZE, STICKER_SIZE)
    drawSticker(Canvas(sticker), content, document)
    return sticker
}

// Lossy WebP keeps transparency. Quality steps down until the file fits the sticker limit.
internal fun encodeSticker(sticker: Bitmap): ByteArray {
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
    var quality = 95
    while (true) {
        val output = ByteArrayOutputStream()
        sticker.compress(format, quality, output)
        if (output.size() <= MAX_STICKER_BYTES || quality <= 35) return output.toByteArray()
        quality -= 15
    }
}
