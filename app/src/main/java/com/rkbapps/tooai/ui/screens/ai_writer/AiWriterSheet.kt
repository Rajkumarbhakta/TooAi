package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.Prompts

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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The Write anything page has a text field; without this the IME covers the
                // composer and its Generate button.
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(
                currentPage = state.currentPage,
                onBack = { onCurrentPageChange(AiWriterPages.HOME) },
                onDismiss = onDismiss
            )

            if (!state.hasModels) {
                NoModelContent(onImportModel = onImportModel)
                return@Column
            }

            when (state.currentPage) {
                AiWriterPages.HOME -> AiWriterHomePage(
                    state = state,
                    onAiWriterOptionClick = onCurrentPageChange,
                    onSelectModel = onSelectModel
                )

                AiWriterPages.POLISH,
                AiWriterPages.SUMMARIZE,
                AiWriterPages.PROOFREAD -> AiWriterGenerationPage(
                    state = state,
                    onStop = onStop,
                    onRegenerate = onRegenerate,
                    onSelectPrompt = onRunPrompt,
                    onShowVariant = onShowVariant,
                    onCopy = onCopy,
                    onShare = onShare,
                    onReplace = onReplace,
                    onRetry = onRetry
                )

                AiWriterPages.PROMPT -> AiWriterPromptPage(
                    state = state,
                    onPromptTextChange = onPromptTextChange,
                    onToggleContext = onToggleContext,
                    onGenerate = onGenerate,
                    onStop = onStop,
                    onRegenerate = onRegenerate,
                    onShowVariant = onShowVariant,
                    onCopy = onCopy,
                    onShare = onShare,
                    onReplace = onReplace,
                    onRetry = onRetry
                )


            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center){
                Text(stringResource(R.string.app_name))
            }
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
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onReplace(done.current) }
            ) {
                Text(stringResource(R.string.ai_writer_replace))
            }
        }

        if (state.sourceText.isNotBlank()) {
            SourceContextCard(
                text = state.sourceText,
                enabled = state.useSourceAsContext,
                onToggle = onToggleContext
            )
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.promptText,
            onValueChange = onPromptTextChange,
            placeholder = { Text(stringResource(R.string.ai_writer_prompt_placeholder)) },
            minLines = 2,
            maxLines = 4,
            shape = RoundedCornerShape(16.dp)
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
                        label = { Text(stringResource(starter.labelRes)) }
                    )
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                // Get the IME out of the way so the streaming result is visible.
                focusManager.clearFocus()
                onGenerate()
            },
            enabled = state.promptText.isNotBlank() && !isBusy
        ) {
            Text(stringResource(R.string.ai_writer_generate))
        }
    }
}

@Composable
private fun SourceContextCard(
    text: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ai_writer_use_selected_text),
                    style = MaterialTheme.typography.labelLarge
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
                    modifier = Modifier.padding(bottom = 8.dp)
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
            ToneChips(
                prompts = state.currentPage.prompts(),
                selected = state.activePrompt,
                onSelectPrompt = onSelectPrompt
            )
        }

        if (done != null && state.canReplace) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onReplace(done.current) }
            ) {
                Text(stringResource(R.string.ai_writer_replace))
            }
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
    val shape = RoundedCornerShape(20.dp)

    // The animated border is the "working" signal; once done the card falls back to a quiet outline.
    val borderModifier = if (isBusy) {
        Modifier.animatedGradientBorder(width = 2.dp, cornerRadius = 20.dp)
    } else {
        Modifier.staticBorder(
            width = 1.dp,
            cornerRadius = 20.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (stage) {
                // Reached by stopping before the first token. Must not keep animating dots —
                // that reads as "still working" and spins the UI thread for nothing.
                AiWriterState.Stage.Idle -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) {
                        Text(
                            text = stringResource(R.string.ai_writer_pick_style),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AiWriterState.Stage.LoadingModel -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) { TypingDots() }
                }

                is AiWriterState.Stage.Generating -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) {
                        if (stage.partial.isBlank()) {
                            TypingDots()
                        } else {
                            ScrollableText(stage.partial)
                        }
                    }
                }

                is AiWriterState.Stage.Done -> {
                    Box(modifier = Modifier.heightIn(min = 160.dp)) {
                        ScrollableText(stage.current)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stage.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.ai_writer_retry))
                        }
                    }
                }
            }
        }
    }
}

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
        CircleIconButton(
            icon = R.drawable.content_copy,
            contentDescription = stringResource(R.string.ai_writer_copy),
            onClick = { onCopy(done.current) }
        )
        CircleIconButton(
            icon = R.drawable.share,
            contentDescription = stringResource(R.string.ai_writer_share),
            onClick = { onShare(done.current) }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Only worth showing once there is something to page between.
        if (done.variants.size > 1) {
            VariantPager(done = done, onShowVariant = onShowVariant)
            Spacer(modifier = Modifier.size(8.dp))
        }

        CircleIconButton(
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
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            text = "${done.index + 1}",
            style = MaterialTheme.typography.labelLarge
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToneChips(
    prompts: List<Prompts>,
    selected: Prompts?,
    onSelectPrompt: (Prompts) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { prompt ->
            FilterChip(
                selected = prompt == selected,
                onClick = { onSelectPrompt(prompt) },
                label = { Text(prompt.subType) }
            )
        }
    }
}

@Composable
private fun StopButton(onStop: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        FilledIconButton(
            onClick = onStop,
            modifier = Modifier.size(56.dp),
            colors = iconButtonStopColors()
        ) {
            // A plain square reads as "stop" without needing another vector asset.
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSecondaryContainer)
            )
        }
    }
}

@Composable
private fun iconButtonStopColors() = IconButtonDefaults.filledIconButtonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
)

@Composable
private fun CircleIconButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
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



/** Three pulsing dots, shown while the model is loading or before the first token arrives. */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typingDots")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
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
// Border modifiers
// ---------------------------------------------------------------------------------------------

private val GradientBorderColors = listOf(
    Color(0xFF2E6BFF),
    Color(0xFF00C2FF),
    Color(0xFF3DDC84),
    Color(0xFFFFC400),
    Color(0xFFFF6D3F),
    Color(0xFFE040FB),
    Color(0xFF7C4DFF),
    // Repeat the first colour so the sweep closes without a hard seam.
    Color(0xFF2E6BFF)
)

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
    val brush = remember { Brush.sweepGradient(GradientBorderColors) }

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

private fun Modifier.staticBorder(width: Dp, cornerRadius: Dp, color: Color): Modifier =
    this.drawWithContent {
        drawContent()
        val strokeWidth = width.toPx()
        val radius = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeWidth)
        )
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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ModelSelector(
            models = state.models,
            selected = state.selectedModel,
            enabled = state.stage !is AiWriterState.Stage.Generating &&
                state.stage !is AiWriterState.Stage.LoadingModel,
            onSelectModel = onSelectModel
        )

        HorizontalDivider()

        AiWriterFullWidthCard {
            onAiWriterOptionClick(AiWriterPages.PROMPT)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_polish),
                icon = R.drawable.ai_polish_text_icon,
            ) {
                onAiWriterOptionClick(AiWriterPages.POLISH)
            }
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_summarize),
                icon = R.drawable.ai_summarize_icon
            ) {
                onAiWriterOptionClick(AiWriterPages.SUMMARIZE)
            }
            AiWriterOptionCard(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.ai_writer_page_proofread),
                icon = R.drawable.ai_proofread_icon
            ) {
                onAiWriterOptionClick(AiWriterPages.PROOFREAD)
            }
        }
    }
}

@Composable
fun AiWriterFullWidthCard(modifier: Modifier = Modifier, onCLick: () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onCLick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Write anything", style = MaterialTheme.typography.titleMedium)
                Text("Create content with a prompt", style = MaterialTheme.typography.bodySmall)
            }

            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ai_writing_prompt_icon),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
            )
        }
    }
}

@Composable
fun AiWriterOptionCard(
    modifier: Modifier = Modifier,
    text: String,
    icon: Int,
    onCLick: () -> Unit
) {
    Card(
        modifier = modifier,
        onClick = onCLick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                imageVector = ImageVector.vectorResource(icon),
                contentDescription = null,
                modifier = Modifier.size(50.dp)
            )
            Text(text, style = MaterialTheme.typography.titleSmall)
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
                    contentDescription = "Navigate up",
                )
            }
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(currentPage.titleRes()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        FilledIconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Close"
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

    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = { expanded = true }, enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = selected?.displayName ?: "Select a model",
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.arrow_drop_down),
                contentDescription = null,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model.displayName) },
                    onClick = {
                        expanded = false
                        onSelectModel(model)
                    }
                )
            }
        }
    }
}

@Composable
private fun ComingSoonContent() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        text = stringResource(R.string.ai_writer_coming_soon),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NoModelContent(onImportModel: () -> Unit) {
    Column(
        modifier = Modifier.padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.ai_writer_no_model_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(R.string.ai_writer_no_model_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onImportModel) {
            Text(stringResource(R.string.ai_writer_import_model))
        }
    }
}