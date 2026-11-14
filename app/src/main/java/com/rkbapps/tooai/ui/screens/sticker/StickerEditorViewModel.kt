package com.rkbapps.tooai.ui.screens.sticker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.hypot

private const val MAX_HISTORY = 40

// Relative to the cut-out's fitted size, and to the base text size.
private val IMAGE_ZOOM_RANGE = 0.25f..4f
private val TEXT_SCALE_RANGE = 0.3f..6f
private const val EMOJI_START_SCALE = 2f

// Where a dragged item is dropped to delete it, in sticker space.
internal const val BIN_CENTER_X = STICKER_SIZE / 2f
internal const val BIN_CENTER_Y = 452f
private const val BIN_RADIUS = 52f

enum class StickerTool { MOVE, DRAW, OUTLINE }

enum class PenWidth(val label: String, val width: Float) {
    FINE("Fine", 6f),
    MEDIUM("Medium", 12f),
    BOLD("Bold", 22f)
}

// The text sheet's starting values. A null id is a new item.
data class TextEditorState(
    val id: Long?,
    val text: String,
    val color: Int,
    val font: StickerFont,
    val isEmoji: Boolean
)

data class StickerEditorState(
    val content: StickerContent? = null,
    val loadFailed: Boolean = false,
    val document: StickerDocument = StickerDocument(),
    // The text item gestures act on. Null means they act on the cut-out.
    val selectedId: Long? = null,
    val tool: StickerTool = StickerTool.MOVE,
    val penColor: Int = Color.WHITE,
    val penWidth: PenWidth = PenWidth.MEDIUM,
    val activeStroke: StrokeItem? = null,
    val canUndo: Boolean = false,
    val isDraggingItem: Boolean = false,
    val isOverBin: Boolean = false,
    val textEditor: TextEditorState? = null,
    val isExporting: Boolean = false
)

@HiltViewModel
class StickerEditorViewModel @Inject constructor(
    private val repository: StickerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StickerEditorState())
    val state = _state.asStateFlow()

    private val history = ArrayDeque<StickerDocument>()
    private var loadedPath: String? = null
    private var nextId = 1L

    // The layer the gesture in progress is moving. Null is the cut-out.
    private var gestureTarget: Long? = null
    private var gestureRecorded = false
    private var imageScaleRange = IMAGE_ZOOM_RANGE

    fun load(path: String) {
        if (path == loadedPath) return
        loadedPath = path
        viewModelScope.launch {
            val content = repository.loadSource(path)
            if (content == null) {
                _state.update { it.copy(loadFailed = true) }
                return@launch
            }
            val fitted = defaultImageTransform(content.bitmap.width, content.bitmap.height)
            imageScaleRange = fitted.scale * IMAGE_ZOOM_RANGE.start..fitted.scale * IMAGE_ZOOM_RANGE.endInclusive
            _state.update { it.copy(content = content, document = StickerDocument(image = fitted)) }
        }
    }

    // Every change to the document goes through here, so every change can be undone.
    private fun edit(change: (StickerDocument) -> StickerDocument) {
        record(_state.value.document)
        _state.update { it.copy(document = change(it.document), canUndo = true) }
    }

    private fun record(document: StickerDocument) {
        history.addLast(document)
        if (history.size > MAX_HISTORY) history.removeFirst()
    }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        _state.update { state ->
            state.copy(
                document = previous,
                canUndo = history.isNotEmpty(),
                selectedId = state.selectedId?.takeIf { id -> previous.items.any { it.id == id } }
            )
        }
    }

    fun selectTool(tool: StickerTool) {
        // Tapping the active tool again closes it.
        _state.update { it.copy(tool = if (it.tool == tool) StickerTool.MOVE else tool, selectedId = null) }
    }

    // --- Moving layers ---

    private fun textAt(x: Float, y: Float): TextItem? =
        _state.value.document.items.lastOrNull { it is TextItem && hitsText(it, x, y) } as TextItem?

    fun onGestureStart(x: Float, y: Float) {
        // A finger on a text item grabs it. Anywhere else moves whatever is selected, which lets
        // two fingers resize a caption too small to hold.
        gestureTarget = textAt(x, y)?.id ?: _state.value.selectedId
        gestureRecorded = false
    }

    fun onTransform(panX: Float, panY: Float, zoom: Float, rotation: Float, centroidX: Float, centroidY: Float) {
        val target = gestureTarget
        if (!gestureRecorded) {
            // One undo step for the whole gesture, not one per frame.
            record(_state.value.document)
            gestureRecorded = true
        }
        _state.update { state ->
            val document = state.document
            val moved = if (target == null) {
                document.copy(image = document.image.adjusted(panX, panY, zoom, rotation, imageScaleRange))
            } else {
                document.copy(
                    items = document.items.map { item ->
                        if (item is TextItem && item.id == target) {
                            item.copy(transform = item.transform.adjusted(panX, panY, zoom, rotation, TEXT_SCALE_RANGE))
                        } else {
                            item
                        }
                    }
                )
            }
            state.copy(
                document = moved,
                canUndo = true,
                selectedId = target,
                isDraggingItem = target != null,
                isOverBin = target != null &&
                        hypot(centroidX - BIN_CENTER_X, centroidY - BIN_CENTER_Y) <= BIN_RADIUS
            )
        }
    }

    fun onGestureEnd() {
        val target = gestureTarget
        _state.update { state ->
            // The step recorded when the drag began already holds the item, so undo brings it back.
            if (state.isOverBin && target != null) {
                state.copy(
                    document = state.document.copy(items = state.document.items.filterNot { it.id == target }),
                    selectedId = null,
                    isDraggingItem = false,
                    isOverBin = false
                )
            } else {
                state.copy(isDraggingItem = false, isOverBin = false)
            }
        }
        gestureTarget = null
    }

    fun onTap(x: Float, y: Float) {
        val hit = textAt(x, y)
        _state.update { state ->
            when {
                hit == null -> state.copy(selectedId = null)
                // A second tap on the selected text opens it for editing.
                hit.id == state.selectedId -> state.copy(
                    textEditor = TextEditorState(hit.id, hit.text, hit.color, hit.font, hit.isEmoji)
                )
                else -> state.copy(selectedId = hit.id)
            }
        }
    }

    // --- Text and emoji ---

    fun addText(isEmoji: Boolean) {
        _state.update {
            it.copy(
                textEditor = TextEditorState(null, "", Color.WHITE, StickerFont.CLASSIC, isEmoji),
                tool = StickerTool.MOVE
            )
        }
    }

    fun closeTextEditor() {
        _state.update { it.copy(textEditor = null) }
    }

    fun commitText(text: String, color: Int, font: StickerFont) {
        val editor = _state.value.textEditor ?: return
        val trimmed = text.trim()
        val id = editor.id
        when {
            id == null && trimmed.isNotEmpty() -> {
                val item = TextItem(
                    id = nextId++,
                    text = trimmed,
                    color = color,
                    font = font,
                    isEmoji = editor.isEmoji,
                    transform = LayerTransform(scale = if (editor.isEmoji) EMOJI_START_SCALE else 1f)
                )
                edit { it.copy(items = it.items + item) }
                _state.update { it.copy(selectedId = item.id) }
            }
            // Clearing an existing item's text removes it.
            id != null && trimmed.isEmpty() -> {
                edit { document -> document.copy(items = document.items.filterNot { it.id == id }) }
                _state.update { it.copy(selectedId = null) }
            }
            id != null -> edit { document ->
                document.copy(
                    items = document.items.map { item ->
                        if (item is TextItem && item.id == id) item.copy(text = trimmed, color = color, font = font) else item
                    }
                )
            }
        }
        closeTextEditor()
    }

    // --- Drawing ---

    fun setPenColor(color: Int) {
        _state.update { it.copy(penColor = color) }
    }

    fun setPenWidth(width: PenWidth) {
        _state.update { it.copy(penWidth = width) }
    }

    fun startStroke(x: Float, y: Float) {
        _state.update {
            it.copy(activeStroke = StrokeItem(nextId++, listOf(StickerPoint(x, y)), it.penColor, it.penWidth.width))
        }
    }

    fun extendStroke(x: Float, y: Float) {
        _state.update { state ->
            val stroke = state.activeStroke ?: return@update state
            state.copy(activeStroke = stroke.copy(points = stroke.points + StickerPoint(x, y)))
        }
    }

    fun finishStroke() {
        val stroke = _state.value.activeStroke ?: return
        edit { it.copy(items = it.items + stroke) }
        _state.update { it.copy(activeStroke = null) }
    }

    // --- Outline ---

    fun setOutline(outline: StickerOutline) {
        if (outline != _state.value.document.outline) edit { it.copy(outline = outline) }
    }

    fun setDarkOutline(dark: Boolean) {
        if (dark != _state.value.document.darkOutline) edit { it.copy(darkOutline = dark) }
    }

    // --- Export ---

    // There is no sticker type on the Android clipboard. The WebP goes on as an image, and the
    // app it is pasted into decides how to show it.
    fun copy(context: Context) = export(context) { webp ->
        val uri = repository.writeStickerFile(context, webp) ?: return@export false
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "Sticker", uri))
        // Android 13 and up show their own clipboard preview.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, "Sticker copied", Toast.LENGTH_SHORT).show()
        }
        true
    }

    fun share(context: Context) = export(context) { webp ->
        val uri = repository.writeStickerFile(context, webp) ?: return@export false
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/webp"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share sticker"))
        true
    }

    fun save(context: Context) = export(context) { webp ->
        val saved = repository.saveToGallery(context, webp)
        if (saved) Toast.makeText(context, "Saved to Pictures/TooAi/Stickers", Toast.LENGTH_SHORT).show()
        saved
    }

    private fun export(context: Context, deliver: suspend (ByteArray) -> Boolean) {
        val state = _state.value
        val content = state.content ?: return
        if (state.isExporting) return
        _state.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            val webp = withContext(Dispatchers.Default) {
                encodeSticker(renderSticker(content, state.document))
            }
            if (!deliver(webp)) {
                Toast.makeText(context, "Unable to export the sticker.", Toast.LENGTH_SHORT).show()
            }
            _state.update { it.copy(isExporting = false) }
        }
    }
}
