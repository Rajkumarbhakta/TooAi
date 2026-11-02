package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.utils.ModelConfigs
import com.rkbapps.tooai.utils.roundTo2Decimals
import java.util.Locale

private val MaxTokenOptions = listOf(512, 1024, 2048, 4096)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmModelConfigurationScreen(
    modifier: Modifier = Modifier,
    title: String,
    confirmButtonText: String,
    model: LlmModel,
    importProgress: Float = 0f,
    fileSizeText: String,
    onChangeFile: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onDone: (LlmModel) -> Unit,
) {
    // Picking another file starts over with that file's defaults.
    key(model) {
        var llmModelUpdated by remember { mutableStateOf(model) }
        val isImporting = importProgress > 0f
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

        // The copy can't be cancelled, so stay here until it finishes.
        BackHandler { if (!isImporting) onDismiss() }

        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeCollapsingTopBar(
                    title = title,
                    scrollBehavior = scrollBehavior,
                    onNavigationIconClick = { if (!isImporting) onDismiss() },
                    navigationIcon = R.drawable.close,
                    navigationContentDescription = "Close"
                )
            },
            bottomBar = {
                ConfigurationActions(
                    confirmButtonText = confirmButtonText,
                    enabled = !isImporting,
                    canConfirm = llmModelUpdated.displayName.isNotBlank(),
                    onCancel = onDismiss,
                    onConfirm = { onDone(llmModelUpdated) }
                )
            },
            containerColor = pageContainerColor()
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ModelFileCard(
                    model = model,
                    fileSizeText = fileSizeText,
                    importProgress = importProgress,
                    onChangeFile = onChangeFile.takeIf { !isImporting }
                )

                OutlinedTextField(
                    value = llmModelUpdated.displayName,
                    onValueChange = { newName ->
                        llmModelUpdated = llmModelUpdated.copy(displayName = newName)
                    },
                    label = { Text("Display name") },
                    singleLine = true,
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = rowContainerColor()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Generation",
                            style = MaterialTheme.typography.titleMediumEmphasized
                        )
                        ParameterSlider(
                            title = "Temperature",
                            valueText = String.format(Locale.getDefault(), "%.2f", llmModelUpdated.temperature),
                            value = llmModelUpdated.temperature.toFloat(),
                            valueRange = ModelConfigs.MIN_TEMPERATURE.toFloat()..ModelConfigs.MAX_TEMPERATURE.toFloat(),
                            enabled = !isImporting,
                            supportingText = "Lower is focused, higher is more creative",
                            onValueChange = { temp ->
                                llmModelUpdated = llmModelUpdated.copy(temperature = temp.roundTo2Decimals())
                            }
                        )
                        ParameterSlider(
                            title = "Top-k",
                            valueText = "${llmModelUpdated.topK}",
                            value = llmModelUpdated.topK.toFloat(),
                            valueRange = ModelConfigs.MIN_TOP_K.toFloat()..ModelConfigs.MAX_TOP_K.toFloat(),
                            enabled = !isImporting,
                            onValueChange = { topK ->
                                llmModelUpdated = llmModelUpdated.copy(topK = topK.toInt())
                            }
                        )
                        ParameterSlider(
                            title = "Top-p",
                            valueText = String.format(Locale.getDefault(), "%.2f", llmModelUpdated.topP),
                            value = llmModelUpdated.topP.toFloat(),
                            valueRange = ModelConfigs.MIN_TOP_P.toFloat()..ModelConfigs.MAX_TOP_P.toFloat(),
                            enabled = !isImporting,
                            onValueChange = { topP ->
                                llmModelUpdated = llmModelUpdated.copy(topP = topP.roundTo2Decimals())
                            }
                        )
                        MaxTokensSelector(
                            maxTokens = llmModelUpdated.maxTokens,
                            enabled = !isImporting,
                            onSelect = { maxTokens ->
                                llmModelUpdated = llmModelUpdated.copy(maxTokens = maxTokens)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelFileCard(
    model: LlmModel,
    fileSizeText: String,
    importProgress: Float,
    onChangeFile: (() -> Unit)?
) {
    val fileType = if (model.name.endsWith(".task")) "LiteRT task bundle" else "LiteRT-LM model"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = rowContainerColor()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sparkles),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                    Text(
                        text = "$fileSizeText · $fileType",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                onChangeFile?.let {
                    TextButton(
                        onClick = it,
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text("Change")
                    }
                }
            }
            if (importProgress > 0f) {
                LinearWavyProgressIndicator(
                    progress = { importProgress },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Copying to app storage · ${(importProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParameterSlider(
    title: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    supportingText: String? = null,
    onValueChange: (Float) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val state = remember(valueRange) { SliderState(value = value, trackRange = valueRange) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            if (isDragged || isPressed) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.inverseSurface
                ) {
                    Text(
                        text = valueText,
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Slider(
            state = state,
            onValueChange = { newValue ->
                state.value = newValue
                onValueChange(newValue)
            },
            enabled = enabled,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth()
        )
        supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaxTokensSelector(
    maxTokens: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Max tokens",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = String.format(Locale.getDefault(), "%,d", maxTokens),
                style = MaterialTheme.typography.labelLargeEmphasized,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            MaxTokenOptions.forEachIndexed { index, option ->
                ToggleButton(
                    checked = option == maxTokens,
                    onCheckedChange = { onSelect(option) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    enabled = enabled,
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        MaxTokenOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    colors = ToggleButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,d", option),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConfigurationActions(
    confirmButtonText: String,
    enabled: Boolean,
    canConfirm: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Surface(color = pageContainerColor()) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.heightIn(min = 56.dp),
                enabled = enabled,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp),
                enabled = enabled && canConfirm,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(
                    text = confirmButtonText,
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

@Preview
@Composable
fun LlmModelConfigurationScreenPreview() {
    TooAiTheme {
        LlmModelConfigurationScreen(
            model = LlmModel(
                name = "Qwen3-0.6B.litertlm",
                displayName = "Qwen3-0.6B.litertlm",
                sizeInBytes = 614236160,
                path = "",
                fileLocation = "",
                maxTokens = ModelConfigs.DEFAULT_MAX_TOKEN,
                topK = ModelConfigs.DEFAULT_TOP_K,
                topP = ModelConfigs.DEFAULT_TOP_P,
                temperature = ModelConfigs.DEFAULT_TEMPERATURE,
                createdAt = 170000
            ),
            title = "Configure model",
            confirmButtonText = "Update",
            fileSizeText = "585.8 MB",
            onDismiss = {}
        ) { }
    }
}
