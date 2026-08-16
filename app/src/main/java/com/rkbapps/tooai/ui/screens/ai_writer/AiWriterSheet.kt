package com.rkbapps.tooai.ui.screens.ai_writer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.utils.PredefinePrompts
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.TypeOfPrompt

/**
 * The AI Writer sheet, shown over whichever app the user selected text in.
 *
 * Only [TypeOfPrompt.Rewrite] and [TypeOfPrompt.Summary] are offered: the CodeSnippet entries in
 * [PredefinePrompts] are prefixes for a task description ("Write a Kotlin snippet to …"), not
 * transforms of text the user already has.
 */
private val WRITER_PROMPT_TYPES = listOf(TypeOfPrompt.Rewrite, TypeOfPrompt.Summary)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWriterSheet(
    state: AiWriterState,
    onSelectModel: (LlmModel) -> Unit,
    onRunPrompt: (Prompts) -> Unit,
    onReplace: (String) -> Unit,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit,
    onImportModel: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(onDismiss = onDismiss)

            if (!state.hasModels) {
                NoModelContent(onImportModel = onImportModel)
                return@Column
            }

            ModelSelector(
                models = state.models,
                selected = state.selectedModel,
                enabled = state.stage !is AiWriterState.Stage.Generating &&
                    state.stage !is AiWriterState.Stage.LoadingModel,
                onSelectModel = onSelectModel
            )

            SourceText(text = state.sourceText)

            HorizontalDivider()

            when (val stage = state.stage) {
                AiWriterState.Stage.Idle -> ActionGrid(
                    enabled = state.sourceText.isNotBlank(),
                    onRunPrompt = onRunPrompt
                )

                AiWriterState.Stage.LoadingModel -> LoadingModel(
                    modelName = state.selectedModel?.displayName.orEmpty()
                )

                is AiWriterState.Stage.Generating -> Generating(
                    partial = stage.partial,
                    onStop = onStop
                )

                is AiWriterState.Stage.Done -> Done(
                    result = stage.result,
                    canReplace = state.canReplace,
                    onReplace = onReplace,
                    onCopy = onCopy,
                    onRetry = onRetry,
                    onBack = onBack
                )

                is AiWriterState.Stage.Error -> ErrorContent(
                    message = stage.message,
                    onRetry = onRetry,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.ai_writer),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onDismiss) {
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
        TextButton(onClick = { expanded = true }, enabled = enabled) {
            Text(
                text = selected?.displayName ?: "Select a model",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(R.drawable.arrow_drop_down),
                contentDescription = null
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
private fun SourceText(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.ai_writer_selected_text),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = text.ifBlank { stringResource(R.string.ai_writer_no_text) },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionGrid(enabled: Boolean, onRunPrompt: (Prompts) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WRITER_PROMPT_TYPES.forEach { type ->
            val prompts = PredefinePrompts.listOfPrompts.filter { it.type == type }
            if (prompts.isEmpty()) return@forEach

            Text(
                text = type.displayString,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                prompts.forEach { prompt ->
                    AssistChip(
                        onClick = { onRunPrompt(prompt) },
                        enabled = enabled,
                        label = { Text(prompt.subType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingModel(modelName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Column {
            Text(
                text = stringResource(R.string.ai_writer_loading_model),
                style = MaterialTheme.typography.bodyMedium
            )
            if (modelName.isNotBlank()) {
                Text(
                    text = modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Generating(partial: String, onStop: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultText(text = partial)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            OutlinedButton(onClick = onStop) {
                Text(stringResource(R.string.ai_writer_stop))
            }
        }
    }
}

@Composable
private fun Done(
    result: String,
    canReplace: Boolean,
    onReplace: (String) -> Unit,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResultText(text = result)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canReplace) {
                Button(onClick = { onReplace(result) }) {
                    Text(stringResource(R.string.ai_writer_replace))
                }
            }
            OutlinedButton(onClick = { onCopy(result) }) {
                Icon(
                    painter = painterResource(R.drawable.content_copy),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.ai_writer_copy),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.ai_writer_retry))
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.ai_writer_back))
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.ai_writer_retry))
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.ai_writer_back))
            }
        }
    }
}

/**
 * Plain text, deliberately not [com.halilibo.richtext.ui.material3.Material3RichText] — the result
 * is headed back into a plain text field, so rendering markdown here would misrepresent it.
 */
@Composable
private fun ResultText(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
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
