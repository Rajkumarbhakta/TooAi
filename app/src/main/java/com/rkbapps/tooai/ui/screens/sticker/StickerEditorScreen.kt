package com.rkbapps.tooai.ui.screens.sticker

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withSave
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.checkerboard
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import kotlin.math.PI
import kotlin.math.abs

// Fixed colors, so a sticker doesn't depend on the app's theme.
private data class InkColor(val label: String, val color: Color)

private val inkColors = listOf(
    InkColor("White", Color.White),
    InkColor("Black", Color.Black),
    InkColor("Red", Color(0xFFE53935)),
    InkColor("Yellow", Color(0xFFFFC107)),
    InkColor("Green", Color(0xFF43A047)),
    InkColor("Blue", Color(0xFF1E88E5)),
    InkColor("Pink", Color(0xFFD81B60)),
)

private val outlineColors = inkColors.take(2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerEditorScreen(
    backStack: SnapshotStateList<Any>,
    sourcePath: String,
    viewModel: StickerEditorViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(sourcePath) { viewModel.load(sourcePath) }
    // A tool's options sit under the canvas, and the canvas keeps drags for itself, so the page
    // is awkward to scroll. Collapsing the bar makes room for them instead.
    LaunchedEffect(state.tool) {
        if (state.tool != StickerTool.MOVE) {
            val bar = scrollBehavior.state
            animate(bar.heightOffset, bar.heightOffsetLimit) { value, _ -> bar.heightOffset = value }
        }
    }
    LaunchedEffect(state.loadFailed) {
        if (state.loadFailed) {
            Toast.makeText(context, "Unable to open this cut-out.", Toast.LENGTH_SHORT).show()
            backStack.removeLastOrNull()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "Sticker",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { backStack.removeLastOrNull() },
                subtitle = { Text("$STICKER_SIZE × $STICKER_SIZE · WebP") },
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = state.canUndo) {
                        Icon(painter = painterResource(R.drawable.undo), contentDescription = "Undo")
                    }
                }
            )
        },
        bottomBar = {
            StickerActions(
                enabled = state.content != null && !state.isExporting,
                onShare = { viewModel.share(context) },
                onSave = { viewModel.save(context) },
                onCopy = { viewModel.copy(context) }
            )
        },
        containerColor = pageContainerColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StickerCanvas(state = state, viewModel = viewModel)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolTile(
                    icon = R.drawable.text_fields,
                    label = "Text",
                    onClick = { viewModel.addText(isEmoji = false) },
                    modifier = Modifier.weight(1f)
                )
                ToolTile(
                    icon = R.drawable.mood,
                    label = "Emoji",
                    onClick = { viewModel.addText(isEmoji = true) },
                    modifier = Modifier.weight(1f)
                )
                ToolTile(
                    icon = R.drawable.edit,
                    label = "Draw",
                    selected = state.tool == StickerTool.DRAW,
                    onClick = { viewModel.selectTool(StickerTool.DRAW) },
                    modifier = Modifier.weight(1f)
                )
                ToolTile(
                    icon = R.drawable.outline_ring,
                    label = "Outline",
                    selected = state.tool == StickerTool.OUTLINE,
                    onClick = { viewModel.selectTool(StickerTool.OUTLINE) },
                    modifier = Modifier.weight(1f)
                )
            }

            when (state.tool) {
                StickerTool.DRAW -> {
                    ColorStrip(
                        colors = inkColors,
                        selected = state.penColor,
                        noun = "pen",
                        onSelect = viewModel::setPenColor
                    )
                    OptionGroup(
                        options = PenWidth.entries,
                        selected = state.penWidth,
                        label = { it.label },
                        onSelect = viewModel::setPenWidth
                    )
                }

                StickerTool.OUTLINE -> {
                    OptionGroup(
                        options = StickerOutline.entries,
                        selected = state.document.outline,
                        label = { it.label },
                        onSelect = viewModel::setOutline
                    )
                    if (state.document.outline != StickerOutline.NONE) {
                        ColorStrip(
                            colors = outlineColors,
                            selected = if (state.document.darkOutline) blackArgb else whiteArgb,
                            noun = "outline",
                            onSelect = { viewModel.setDarkOutline(it == blackArgb) }
                        )
                    }
                }

                StickerTool.MOVE -> Text(
                    text = if (state.selectedId == null) {
                        "Drag to move the cut-out. Pinch to resize or rotate it."
                    } else {
                        "Tap the text again to edit it. Drag it to the bin to delete it."
                    },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    state.textEditor?.let { editor ->
        TextEditorSheet(
            editor = editor,
            onDismiss = viewModel::closeTextEditor,
            onDone = viewModel::commitText
        )
    }
}

private val whiteArgb = Color.White.toArgb()
private val blackArgb = Color.Black.toArgb()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StickerCanvas(state: StickerEditorState, viewModel: StickerEditorViewModel) {
    val content = state.content
    val drawing = state.tool == StickerTool.DRAW
    val selectionColor = MaterialTheme.toolPalette.background.action.toArgb()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(32.dp))
            // Darker than the usual pattern, or a white outline disappears into it.
            .checkerboard(
                cell = 10.dp,
                light = MaterialTheme.colorScheme.surfaceContainerHighest,
                dark = MaterialTheme.colorScheme.outlineVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content == null) {
            LoadingIndicator(modifier = Modifier.size(64.dp))
            return@BoxWithConstraints
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawing) {
                    // Touches arrive in pixels. The document lives in sticker space.
                    val toSticker = STICKER_SIZE / size.width.toFloat()
                    if (drawing) {
                        detectDragGestures(
                            onDragStart = { viewModel.startStroke(it.x * toSticker, it.y * toSticker) },
                            onDrag = { change, _ ->
                                change.consume()
                                viewModel.extendStroke(change.position.x * toSticker, change.position.y * toSticker)
                            },
                            onDragEnd = viewModel::finishStroke,
                            onDragCancel = viewModel::finishStroke
                        )
                    } else {
                        detectLayerGestures(
                            onStart = { viewModel.onGestureStart(it.x * toSticker, it.y * toSticker) },
                            onTransform = { pan, zoom, rotation, centroid ->
                                viewModel.onTransform(
                                    panX = pan.x * toSticker,
                                    panY = pan.y * toSticker,
                                    zoom = zoom,
                                    rotation = rotation,
                                    centroidX = centroid.x * toSticker,
                                    centroidY = centroid.y * toSticker
                                )
                            },
                            onEnd = viewModel::onGestureEnd,
                            onTap = { viewModel.onTap(it.x * toSticker, it.y * toSticker) }
                        )
                    }
                }
                .semantics { contentDescription = "Sticker canvas" }
        ) {
            val fromSticker = size.width / STICKER_SIZE
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.withSave {
                    scale(fromSticker, fromSticker)
                    drawSticker(this, content, state.document, state.activeStroke)
                    val selected = state.document.items.firstOrNull { it.id == state.selectedId }
                    if (selected is TextItem) drawSelection(this, selected, selectionColor)
                }
            }
        }

        // Sits where the ViewModel's bin zone is, so dropping on the icon is dropping in the zone.
        val binSize = 52.dp
        val dpPerStickerPixel = maxWidth / STICKER_SIZE
        AnimatedVisibility(
            visible = state.isDraggingItem,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = dpPerStickerPixel * BIN_CENTER_X - binSize / 2,
                    y = dpPerStickerPixel * BIN_CENTER_Y - binSize / 2
                ),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                modifier = Modifier.size(binSize),
                shape = CircleShape,
                color = if (state.isOverBin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.inverseSurface,
                contentColor = if (state.isOverBin) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.inverseOnSurface,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.delete), contentDescription = "Drop here to delete")
                }
            }
        }
    }
}

// Like detectTransformGestures, but it reports where the gesture started and ended, and tells a
// tap from a drag. The editor needs all three to pick a layer, drop it in the bin and select it.
private suspend fun PointerInputScope.detectLayerGestures(
    onStart: (Offset) -> Unit,
    onTransform: (pan: Offset, zoom: Float, rotation: Float, centroid: Offset) -> Unit,
    onEnd: () -> Unit,
    onTap: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onStart(down.position)

        var pan = Offset.Zero
        var zoom = 1f
        var rotation = 0f
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop

        do {
            val event = awaitPointerEvent()
            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange
                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    if (zoomMotion > touchSlop || rotationMotion > touchSlop || pan.getDistance() > touchSlop) {
                        pastTouchSlop = true
                    }
                }
                if (pastTouchSlop) {
                    if (zoomChange != 1f || rotationChange != 0f || panChange != Offset.Zero) {
                        onTransform(panChange, zoomChange, rotationChange, event.calculateCentroid(useCurrent = true))
                    }
                    // Consumed so the page behind doesn't scroll while a layer is being moved.
                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })

        if (pastTouchSlop) onEnd() else onTap(down.position)
    }
}

// A null selected is a plain action. True or false is a tool that stays on.
@Composable
private fun ToolTile(
    @DrawableRes icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null
) {
    val accent = MaterialTheme.toolPalette.background
    val isOn = selected == true
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(72.dp)
            .semantics { selected?.let { this.selected = it } },
        shape = RoundedCornerShape(20.dp),
        color = if (isOn) accent.action else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (isOn) accent.onAction else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text = label, maxLines = 1, style = MaterialTheme.typography.labelMediumEmphasized)
        }
    }
}

@Composable
private fun ColorStrip(
    colors: List<InkColor>,
    selected: Int,
    noun: String,
    onSelect: (Int) -> Unit
) {
    val ring = MaterialTheme.toolPalette.background.action
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
    ) {
        colors.forEach { ink ->
            val isSelected = ink.color.toArgb() == selected
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .then(if (isSelected) Modifier.border(2.dp, ring, CircleShape) else Modifier)
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(ink.color)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(ink.color.toArgb()) },
                        role = Role.RadioButton
                    )
                    .semantics { contentDescription = "${ink.label} $noun" }
            )
        }
    }
}

// A connected group, since exactly one option is in effect at a time.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> OptionGroup(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    val accent = MaterialTheme.toolPalette.background
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = accent.action,
                    checkedContentColor = accent.onAction
                )
            ) {
                Text(text = label(option), maxLines = 1, style = MaterialTheme.typography.labelLargeEmphasized)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TextEditorSheet(
    editor: TextEditorState,
    onDismiss: () -> Unit,
    onDone: (text: String, color: Int, font: StickerFont) -> Unit
) {
    // No partially expanded stop: the sheet opens fully, above the keyboard.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    // The cursor starts after the existing text, ready to add to it.
    var text by rememberSaveable(editor, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(editor.text, TextRange(editor.text.length)))
    }
    var color by rememberSaveable(editor) { mutableIntStateOf(editor.color) }
    var font by rememberSaveable(editor) { mutableStateOf(editor.font) }
    val focusRequester = remember { FocusRequester() }
    val accent = MaterialTheme.toolPalette.background
    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHigh

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Without this the keyboard covers the Done button.
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when {
                    editor.isEmoji -> "Add emoji"
                    editor.id == null -> "Add text"
                    else -> "Edit text"
                },
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.headlineSmallEmphasized
            )
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(if (editor.isEmoji) "Pick emoji from your keyboard" else "Type a caption")
                },
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = fieldColor,
                    unfocusedContainerColor = fieldColor,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            // Emoji carry their own colours and glyphs, so neither option applies to them.
            if (!editor.isEmoji) {
                ColorStrip(colors = inkColors, selected = color, noun = "text", onSelect = { color = it })
                OptionGroup(
                    options = StickerFont.entries,
                    selected = font,
                    label = { it.label },
                    onSelect = { font = it }
                )
            }
            Button(
                onClick = { onDone(text.text, color, font) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.action,
                    contentColor = accent.onAction
                )
            ) {
                Text(text = "Done", style = MaterialTheme.typography.titleMediumEmphasized)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StickerActions(
    enabled: Boolean,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onCopy: () -> Unit
) {
    val accent = MaterialTheme.toolPalette.background
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onShare, enabled = enabled) {
                    Icon(painter = painterResource(R.drawable.share), contentDescription = "Share sticker")
                }
                IconButton(onClick = onSave, enabled = enabled) {
                    Icon(painter = painterResource(R.drawable.download), contentDescription = "Save sticker")
                }
            }
        }
        Button(
            onClick = onCopy,
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.action,
                contentColor = accent.onAction
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.content_copy),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "Copy sticker", style = MaterialTheme.typography.titleMediumEmphasized)
        }
    }
}
