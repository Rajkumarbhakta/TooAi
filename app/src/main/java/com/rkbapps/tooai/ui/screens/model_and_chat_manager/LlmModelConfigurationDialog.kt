package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.utils.ModelConfigs
import com.rkbapps.tooai.utils.roundTo2Decimals

@Composable
fun LlmModelConfigurationDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    model: LlmModel, onDone: (LlmModel) -> Unit,
) {
    var llmModelUpdated by remember { mutableStateOf(model) }


    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = modifier.fillMaxWidth().padding(10.dp)
                .clip(RoundedCornerShape(12.dp)).background(
                color = MaterialTheme.colorScheme.surface,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Configure Model", style = MaterialTheme.typography.headlineMedium)
                Text("Name : ${model.displayName}")
                Text("Model type : LLM")
                Spacer(modifier = Modifier.height(10.dp))
                Text("Max Token : ${llmModelUpdated.maxTokens}")
                Slider(
                    value = llmModelUpdated.maxTokens.toFloat(),
                    valueRange = ModelConfigs.MIN_MAX_TOKEN.toFloat()..ModelConfigs.MAX_MAX_TOKEN.toFloat(),
                    onValueChange = { maxTokens->
                        val update = llmModelUpdated.copy(
                            maxTokens = maxTokens.toInt()
                        )
                        llmModelUpdated = update
                    }
                )
                Text("Top K : ${llmModelUpdated.topK}")
                Slider(
                    value = llmModelUpdated.topK.toFloat(),
                    valueRange = ModelConfigs.MIN_TOP_K.toFloat()..ModelConfigs.MAX_TOP_K.toFloat(),
                    onValueChange = { topK->
                        val update = llmModelUpdated.copy(
                            topK = topK.toInt()
                        )
                        llmModelUpdated = update
                    }
                )
                Text("Top P : ${llmModelUpdated.topP}")
                Slider(
                    value = llmModelUpdated.topP.toFloat(),
                    valueRange = ModelConfigs.MIN_TOP_P.toFloat()..ModelConfigs.MAX_TOP_P.toFloat(),
                    onValueChange = { topP->
                        val update = llmModelUpdated.copy(
                            topP = topP.roundTo2Decimals()
                        )
                        llmModelUpdated = update
                    }
                )
                Text("Temperature : ${llmModelUpdated.temperature}")
                Slider(
                    value = llmModelUpdated.temperature.toFloat(),
                    valueRange = ModelConfigs.MIN_TEMPERATURE.toFloat()..ModelConfigs.MAX_TEMPERATURE.toFloat(),
                    onValueChange = { temperature->
                        val update = llmModelUpdated.copy(
                            temperature = temperature.roundTo2Decimals()
                        )
                        llmModelUpdated = update
                    }
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onDone(llmModelUpdated)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import")
                    }
                }
            }
        }
    }



}



@Preview
@Composable
fun LlmModelConfigurationDialogPreview(modifier: Modifier = Modifier) {

    TooAiTheme() {
        LlmModelConfigurationDialog(
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
            onDismiss = {}
        ) { }
    }



}












