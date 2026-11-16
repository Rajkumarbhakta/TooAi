package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.ui.theme.ToolColors
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.rememberClickGate

private val CardCorner = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWriterSheet(
    state: AiWriterState,
    onSelectModel: (LlmModel) -> Unit,
    onRunPrompt: (Prompts) -> Unit,
    onReplace: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
    onShowVariant: (Int) -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
    onPromptTextChange: (String) -> Unit,
    onToggleContext: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onImportModel: () -> Unit,
    onCurrentPageChange: (page: AiWriterPages) -> Unit,
    onDismiss: () -> Unit
) {
    // No partially expanded stop: the sheet opens fully, as it did before.
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    // One gate for the whole sheet: every one of these starts or restarts on-device inference, and
    // two in quick succession — a double-tapped chip, or a chip followed by Regenerate — means two
    // runs racing. Sharing the window covers different buttons hit back to back, not just repeats
    // of the same one.
    val gate = rememberClickGate()
    val onRunPromptDebounced: (Prompts) -> Unit = { gate { onRunPrompt(it) } }
    val onRegenerateDebounced: () -> Unit = { gate { onRegenerate() } }
    val onRetryDebounced: () -> Unit = { gate { onRetry() } }
    val onGenerateDebounced: () -> Unit = { gate { onGenerate() } }
    val onReplaceDebounced: (String) -> Unit = { gate { onReplace(it) } }
    val onCurrentPageChangeDebounced: (AiWriterPages) -> Unit = { gate { onCurrentPageChange(it) } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The Write anything page has a text field; without this the IME covers the
                // composer and its Generate button. Must stay *outside* the scroll below —
                // inset padding applied inside a scroll container pads the content instead of
                // shrinking the viewport, which silently defeats it.
                .imePadding()
                // Long results, a keyboard, or a large font scale can overflow the sheet; without
                // this the bottom of the page (Generate, Replace) becomes unreachable.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(
                currentPage = state.currentPage,
                onBack = { onCurrentPageChangeDebounced(AiWriterPages.HOME) },
                onDismiss = onDismiss
            )

            if (!state.hasModels) {
                NoModelContent(onImportModel = onImportModel)
                return@Column
            }

            when (state.currentPage) {
                AiWriterPages.HOME -> AiWriterHomePage(
                    state = state,
                    onAiWriterOptionClick = onCurrentPageChangeDebounced,
                    onSelectModel = onSelectModel
                )

                AiWriterPages.POLISH,
                AiWriterPages.SUMMARIZE,
                AiWriterPages.PROOFREAD -> AiWriterGenerationPage(
                    state = state,
                    onStop = onStop,
                    onRegenerate = onRegenerateDebounced,
                    onSelectPrompt = onRunPromptDebounced,
                    onShowVariant = onShowVariant,
                    onCopy = onCopy,
                    onShare = onShare,
                    onReplace = onReplaceDebounced,
                    onRetry = onRetryDebounced
                )

                AiWriterPages.PROMPT -> AiWriterPromptPage(
                    state = state,
                    onPromptTextChange = onPromptTextChange,
                    onToggleContext = onToggleContext,
                    onGenerate = onGenerateDebounced,
                    onStop = onStop,
                    onRegenerate = onRegenerateDebounced,
                    onShowVariant = onShowVariant,
                    onCopy = onCopy,
                    onShare = onShare,
                    onReplace = onReplaceDebounced,
                    onRetry = onRetryDebounced
                )
            }

            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Write anything page
// ---------------------------------------------------------------------------------------------

/** Label shown on the chip, and the instruction it drops into the input (still editable). */
private data class PromptStarter(val labelRes: Int, val textRes: Int)

private val PROMPT_STARTERS = listOf(
    PromptStarter(R.string.ai_writer_starter_reply, R.string.ai_writer_starter_reply_text),
    PromptStarter(R.string.ai_writer_starter_email, R.string.ai_writer_starter_email_text),
    PromptStarter(R.string.ai_writer_starter_list, R.string.ai_writer_starter_list_text),
    PromptStarter(R.string.ai_writer_starter_explain, R.string.ai_writer_starter_explain_text)
)

/**
 * Unlike the tone pages, this one has nothing to run until the user types an instruction, so the
 * composer stays on screen and results appear above it.
 */
@Composable
fun AiWriterPromptPage(
    modifier: Modifier = Modifier,
    state: AiWriterState,
    onPromptTextChange: (String) -> Unit,
    onToggleContext: (Boolean) -> Unit,
    onGenerate: () -> Unit,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onShowVariant: (Int) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onReplace: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isBusy = state.stage is AiWriterState.Stage.Generating ||
        state.stage is AiWriterState.Stage.LoadingModel
    val done = state.stage as? AiWriterState.Stage.Done
    val isIdle = state.stage is AiWriterState.Stage.Idle
    val focusManager = LocalFocusManager.current
    val composerColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isIdle) {
            GenerationCard(
                stage = state.stage,
                isBusy = isBusy,
                onCopy = onCopy,
                onShare = onShare,
                onRegenerate = onRegenerate,
                onShowVariant = onShowVariant,
                onRetry = onRetry
            )
        }

        if (isBusy) {
            StopButton(onStop = onStop)
        }

        if (done != null && state.canReplace) {
            ReplaceButton(onClick = { onReplace(done.current) })
        }

        if (state.sourceText.isNotBlank()) {
            SourceContextCard(
                text = state.sourceText,
                enabled = state.useSourceAsContext,
                onToggle = onToggleContext
            )
        }

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.promptText,
            onValueChange = onPromptTextChange,
            placeholder = { Text(stringResource(R.string.ai_writer_prompt_placeholder)) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = composerColor,
                unfocusedContainerColor = composerColor,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        // Only a hint for an empty box; once there is a prompt or a result they are just clutter.
        if (isIdle && state.promptText.isBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PROMPT_STARTERS.forEach { starter ->
                    val text = stringResource(starter.textRes)
                    AssistChip(
                        onClick = { onPromptTextChange(text) },
                        label = { Text(stringResource(starter.labelRes)) },
                        shape = CircleShape,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.sparkles),
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }

        LargeActionButton(
            label = stringResource(R.string.ai_writer_generate),
            icon = R.drawable.send,
            enabled = state.promptText.isNotBlank() && !isBusy,
            onClick = {
                // Get the IME out of the way so the streaming result is visible.
                focusManager.clearFocus()
                onGenerate()
            }
        )
    }
}

@Composable
private fun SourceContextCard(
    text: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = rowContainerColor()
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ai_writer_use_selected_text),
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
                Switch(checked = enabled, onCheckedChange = onToggle)
            }
            AnimatedVisibility(visible = enabled) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Generation page
// ---------------------------------------------------------------------------------------------

@Composable
fun AiWriterGenerationPage(
    modifier: Modifier = Modifier,
    state: AiWriterState,
    onStop: () -> Unit,
    onRegenerate: () -> Unit,
    onSelectPrompt: (Prompts) -> Unit,
    onShowVariant: (Int) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onReplace: (String) -> Unit,
    onRetry: () -> Unit
) {
    val isBusy = state.stage is AiWriterState.Stage.Generating ||
        state.stage is AiWriterState.Stage.LoadingModel
    val done = state.stage as? AiWriterState.Stage.Done

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GenerationCard(
            stage = state.stage,
            isBusy = isBusy,
            onCopy = onCopy,
            onShare = onShare,
            onRegenerate = onRegenerate,
            onShowVariant = onShowVariant,
            onRetry = onRetry
        )

        if (isBusy) {
            StopButton(onStop = onStop)
        }

        // Available whenever nothing is running, so a stopped or failed run still has a way
        // forward rather than dead-ending.
        AnimatedVisibility(visible = !isBusy) {
            ToneButtons(
                prompts = state.currentPage.prompts(),
                selected = state.activePrompt,
                onSelectPrompt = onSelectPrompt
            )
        }

        if (done != null && state.canReplace) {
            ReplaceButton(onClick = { onReplace(done.current) })
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.selectedModel?.displayName
                ?.let { stringResource(R.string.ai_writer_disclaimer, it) }
                ?: stringResource(R.string.ai_writer_disclaimer_generic),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GenerationCard(
    stage: AiWriterState.Stage,
    isBusy: Boolean,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
    onShowVariant: (Int) -> Unit,
    onRetry: () -> Unit
) {
    // The animated border is the "working" signal; once done the card has no outline at all.
    val borderModifier = if (isBusy) {
        Modifier.animatedGradientBorder(width = 2.dp, cornerRadius = CardCorner)
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = RoundedCornerShape(CardCorner),
        color = rowContainerColor()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            when (stage) {
                // Reached by stopping before the first token. Must not keep animating — that
                // reads as "still working" and spins the UI thread for nothing.
                AiWriterState.Stage.Idle -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) {
                        Text(
                            text = stringResource(R.string.ai_writer_pick_style),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AiWriterState.Stage.LoadingModel -> {
                    WorkingIndicator(label = stringResource(R.string.ai_writer_loading_model))
                }

                is AiWriterState.Stage.Generating -> {
                    if (stage.partial.isBlank()) {
                        WorkingIndicator(label = null)
                    } else {
                        Box(modifier = Modifier.heightIn(min = 160.dp)) {
                            ScrollableText(stage.partial)
                        }
                    }
                }

                is AiWriterState.Stage.Done -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) {
                        ScrollableText(stage.current)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ResultActionRow(
                        done = stage,
                        onCopy = onCopy,
                        onShare = onShare,
                        onRegenerate = onRegenerate,
                        onShowVariant = onShowVariant
                    )
                }

                is AiWriterState.Stage.Error -> {
                    Column(
                        modifier = Modifier.heightIn(min = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ) {
                            Text(
                                text = stage.message,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Button(onClick = onRetry, shapes = ButtonDefaults.shapes()) {
                            Icon(
                                painter = painterResource(R.drawable.refresh),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.ai_writer_retry))
                        }
                    }
                }
            }
        }
    }
}

// Only shown while the model loads or before the first token, never in Idle: an indicator left
// running there keeps a CPU core busy.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WorkingIndicator(label: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LoadingIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.toolPalette.aiChat.action
        )
        label?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultActionRow(
    done: AiWriterState.Stage.Done,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onRegenerate: () -> Unit,
    onShowVariant: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResultIconButton(
            icon = R.drawable.content_copy,
            contentDescription = stringResource(R.string.ai_writer_copy),
            onClick = { onCopy(done.current) }
        )
        ResultIconButton(
            icon = R.drawable.share,
            contentDescription = stringResource(R.string.ai_writer_share),
            onClick = { onShare(done.current) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Only worth showing once there is something to page between.
        if (done.variants.size > 1) {
            VariantPager(done = done, onShowVariant = onShowVariant)
        }

        ResultIconButton(
            icon = R.drawable.refresh,
            contentDescription = stringResource(R.string.ai_writer_regenerate),
            onClick = onRegenerate
        )
    }
}

@Composable
private fun VariantPager(
    done: AiWriterState.Stage.Done,
    onShowVariant: (Int) -> Unit
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onShowVariant(done.index - 1) },
                enabled = done.hasPrev
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.ai_writer_previous_variant),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "${done.index + 1} / ${done.variants.size}",
                style = MaterialTheme.typography.labelLargeEmphasized
            )
            IconButton(
                onClick = { onShowVariant(done.index + 1) },
                enabled = done.hasNext
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = stringResource(R.string.ai_writer_next_variant),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// A connected group, since exactly one tone is in effect at a time.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToneButtons(
    prompts: List<Prompts>,
    selected: Prompts?,
    onSelectPrompt: (Prompts) -> Unit
) {
    val accent = MaterialTheme.toolPalette.aiChat
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        prompts.forEachIndexed { index, prompt ->
            ToggleButton(
                checked = prompt == selected,
                // Tapping the active tone again runs it again, like the chips did.
                onCheckedChange = { onSelectPrompt(prompt) },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    prompts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkedContainerColor = accent.action,
                    checkedContentColor = accent.onAction
                )
            ) {
                Text(
                    text = prompt.subType,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLargeEmphasized
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StopButton(onStop: () -> Unit) {
    FilledTonalButton(
        onClick = onStop,
        modifier = Modifier.fillMaxWidth(),
        shapes = ButtonDefaults.shapes()
    ) {
        Icon(
            painter = painterResource(R.drawable.stop),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(stringResource(R.string.ai_writer_stop))
    }
}

// Replace is the result's one job when the caller can take text back, so it gets the big button.
@Composable
private fun ReplaceButton(onClick: () -> Unit) {
    LargeActionButton(
        label = stringResource(R.string.ai_writer_replace),
        icon = R.drawable.check,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LargeActionButton(
    label: String,
    @DrawableRes icon: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.toolPalette.aiChat
    val height = ButtonDefaults.MediumContainerHeight
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        shapes = ButtonDefaults.shapesFor(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent.action,
            contentColor = accent.onAction
        )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        Text(text = label, style = MaterialTheme.typography.titleMediumEmphasized)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ResultIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledTonalIconButton(
        onClick = onClick,
        shapes = IconButtonDefaults.shapes(),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun ScrollableText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------------------------------------------------------------------------------------------
// Border modifier
// ---------------------------------------------------------------------------------------------

/**
 * A rotating sweep-gradient outline, used to signal that the model is working.
 *
 * The gradient has to rotate while the outline itself stays put, so this clips to the border ring
 * (outer rounded rect minus inner rounded rect) and spins an oversized gradient-filled rect inside
 * that clip. Rotating the stroke directly would spin the card's whole shape instead.
 */
@Composable
private fun Modifier.animatedGradientBorder(width: Dp, cornerRadius: Dp): Modifier {
    val transition = rememberInfiniteTransition(label = "gradientBorder")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    // Theme colors, so the sweep follows dynamic color and dark mode. The first is repeated last
    // so the sweep closes without a hard seam.
    val palette = MaterialTheme.toolPalette
    val colors = listOf(
        palette.aiChat.action,
        palette.text.action,
        palette.qr.action,
        palette.docs.action,
        palette.background.action,
        palette.aiChat.action
    )
    val brush = remember(colors) { Brush.sweepGradient(colors) }

    return this.drawWithContent {
        drawContent()

        val strokeWidth = width.toPx()
        val radius = cornerRadius.toPx()

        val outer = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(Offset.Zero, size),
                    cornerRadius = CornerRadius(radius, radius)
                )
            )
        }
        val inner = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(
                        Offset(strokeWidth, strokeWidth),
                        Size(size.width - strokeWidth * 2f, size.height - strokeWidth * 2f)
                    ),
                    cornerRadius = CornerRadius(
                        (radius - strokeWidth).coerceAtLeast(0f),
                        (radius - strokeWidth).coerceAtLeast(0f)
                    )
                )
            )
        }
        val ring = Path.combine(PathOperation.Difference, outer, inner)

        // Big enough that the rotating rect still covers every corner of the ring.
        val span = kotlin.math.hypot(size.width, size.height)
        val topLeft = Offset((size.width - span) / 2f, (size.height - span) / 2f)

        clipPath(ring) {
            rotate(angle) {
                drawRect(brush = brush, topLeft = topLeft, size = Size(span, span))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Home page
// ---------------------------------------------------------------------------------------------

@Composable
fun AiWriterHomePage(
    modifier: Modifier = Modifier,
    state: AiWriterState,
    onAiWriterOptionClick: (option: AiWriterPages) -> Unit,
    onSelectModel: (LlmModel) -> Unit
) {
    val palette = MaterialTheme.toolPalette
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModelSelector(
            models = state.models,
            selected = state.selectedModel,
            enabled = state.stage !is AiWriterState.Stage.Generating &&
                state.stage !is AiWriterState.Stage.LoadingModel,
            onSelectModel = onSelectModel
        )

        // Free-form writing works from an instruction alone, so it stays available even with
        // nothing selected.
        AiWriterFullWidthCard {
            onAiWriterOptionClick(AiWriterPages.PROMPT)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_polish),
                icon = R.drawable.sparkles,
                accent = palette.aiChat,
                enabled = state.hasSourceText
            ) {
                onAiWriterOptionClick(AiWriterPages.POLISH)
            }
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_summarize),
                icon = R.drawable.notes,
                accent = palette.text,
                enabled = state.hasSourceText
            ) {
                onAiWriterOptionClick(AiWriterPages.SUMMARIZE)
            }
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_proofread),
                icon = R.drawable.check,
                accent = palette.qr,
                enabled = state.hasSourceText
            ) {
                onAiWriterOptionClick(AiWriterPages.PROOFREAD)
            }
        }

        // The three above only transform the caller's selection. Say why they are unavailable
        // rather than leaving three dead tiles.
        if (!state.hasSourceText) {
            Text(
                text = stringResource(R.string.ai_writer_no_text),
                modifier = Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// The hero of the home page: the one action that works with or without a selection.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AiWriterFullWidthCard(modifier: Modifier = Modifier, onCLick: () -> Unit) {
    val accent = MaterialTheme.toolPalette.aiChat
    Surface(
        onClick = onCLick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCorner),
        color = accent.container,
        contentColor = accent.onContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_writer_page_prompt),
                    style = MaterialTheme.typography.titleLargeEmphasized
                )
                Text(
                    text = stringResource(R.string.ai_writer_page_prompt_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent.onContainer.copy(alpha = 0.8f)
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accent.action, ToolShapes.AiChat.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    tint = accent.onAction
                )
            }
        }
    }
}

@Composable
fun AiWriterOptionCard(
    modifier: Modifier = Modifier,
    text: String,
    @DrawableRes icon: Int,
    accent: ToolColors,
    enabled: Boolean = true,
    onCLick: () -> Unit
) {
    Surface(
        onClick = onCLick,
        enabled = enabled,
        modifier = modifier.alpha(if (enabled) 1f else 0.38f),
        shape = RoundedCornerShape(24.dp),
        color = rowContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(accent.container, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = accent.onContainer
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmallEmphasized,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Shared chrome
// ---------------------------------------------------------------------------------------------

/**
 * Three-slot layout so the title stays optically centred whether or not the back button is
 * present — a plain SpaceBetween row shifts the title on the home page.
 */
@Composable
private fun Header(
    currentPage: AiWriterPages,
    onBack: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = currentPage != AiWriterPages.HOME,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.ai_writer_back),
                )
            }
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(currentPage.titleRes()),
            style = MaterialTheme.typography.titleLargeEmphasized
        )

        FilledTonalIconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = stringResource(R.string.ai_writer_close)
            )
        }
    }
}

@Composable
private fun ModelSelector(
    models: List<LlmModel>,
    selected: LlmModel?,
    enabled: Boolean,
    onSelectModel: (LlmModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val accent = MaterialTheme.toolPalette.aiChat

    Box {
        AssistChip(
            onClick = { expanded = true },
            enabled = enabled,
            shape = CircleShape,
            label = {
                Text(
                    text = selected?.displayName ?: stringResource(R.string.ai_writer_select_model),
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.sparkles),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.arrow_drop_down),
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize)
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = accent.container,
                labelColor = accent.onContainer,
                leadingIconContentColor = accent.onContainer,
                trailingIconContentColor = accent.onContainer
            ),
            border = null
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.displayName) },
                    leadingIcon = if (model.id == selected?.id) {
                        { Icon(painter = painterResource(R.drawable.check), contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelectModel(model)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoModelContent(onImportModel: () -> Unit) {
    val accent = MaterialTheme.toolPalette.aiChat
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp)
                .background(accent.container, ToolShapes.AiChat.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.chatbot),
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            text = stringResource(R.string.ai_writer_no_model_title),
            style = MaterialTheme.typography.titleMediumEmphasized
        )
        Text(
            text = stringResource(R.string.ai_writer_no_model_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LargeActionButton(
            label = stringResource(R.string.ai_writer_import_model),
            icon = R.drawable.add,
            onClick = onImportModel
        )
    }
}
