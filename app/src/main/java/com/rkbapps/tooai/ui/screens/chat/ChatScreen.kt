package com.rkbapps.tooai.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.utils.roundTo2Decimals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    backStack: SnapshotStateList<Any>,viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.chatState.collectAsStateWithLifecycle()
    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState()
    var showModelSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopBar(
                title = {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(
                            color = Color.White.copy(alpha = 0.2f)
                        )
                            .clickable{
                                showModelSheet = true
                            }
                            .padding(8.dp)
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.llmModel?.displayName?:"Chat", style = MaterialTheme.typography.titleMedium,
                            overflow = TextOverflow.MiddleEllipsis
                        )
                        if (state.llmModel!=null){
                            Icon(
                                painter = painterResource(R.drawable.arrow_drop_down),
                                contentDescription = "ChangeModel"
                            )
                        }
                    }
                }
            ) {
                backStack.removeLastOrNull()
            }
        }
    ) {innerPadding->




        // Bottom Sheet for model selection
        if (showModelSheet) {
            ModalBottomSheet(
                onDismissRequest = { showModelSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
                ) {
                    Text(
                        "Available Models",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            llmModels,
                            key = { it.id }
                        ) { model ->
                            ModelListItem(
                                model = model,
                                isSelected = state.llmModel?.id == model.id,
                                onSelectModel = {
                                    state.sessionId?.let {sessionId->
                                        viewModel.loadSession(sessionId = sessionId, modelId=model.id)
                                        scope.launch {
                                            sheetState.hide()
                                            showModelSheet = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }







        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (state.modelInitializingStatus.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.weight(1f).padding(8.dp),
                reverseLayout = true
            ) {
                items(
                    state.messages.reversed(),
                    key = { it.id }
                ) { message ->
                    when(message.type){
                        ChatType.CHAT -> {
                            ChatMessageItem(
                                modifier = Modifier.padding(start = 30.dp),
                                message = message,
                                alignment = Alignment.End,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            )
                        }
                        ChatType.ASSISTANT -> {
                            ChatMessageItem(
                                modifier = Modifier.padding(end = 30.dp),
                                message = message,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                alignment = Alignment.Start
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message") },
                shape = RoundedCornerShape(100.dp),
                trailingIcon = {
                when{
                    state.modelInitializingStatus.isLoading && state.instance == null ->{
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    state.isChatRunning ->{
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    else->{
                            FilledIconButton(
                                onClick = {
                                    if (messageText.isNotBlank()) {
                                        viewModel.sendMessage(messageText)
                                        messageText = ""
                                    }
                                }
                            ) {
                                Icon(painter = painterResource(R.drawable.send),"Send message")
                            }
                        }
                    }

                }
            )
        }
    }

}

@Composable
fun ChatMessageItem(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    alignment: Alignment.Horizontal,
    color: Color,
) {
    var isStatsVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Card(
            onClick = {
                isExpanded = !isExpanded
                isStatsVisible = false
            },
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            if (message.isResponding){
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    strokeWidth = 3.dp
                )
            }else{
                RichText(
                    modifier = Modifier.background(color).padding(8.dp),
                ){
                    Markdown(content = message.message)
                }
            }
        }

        AnimatedVisibility(
            visible = message.statistics!=null && isExpanded
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                //show stats button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(color = MaterialTheme.colorScheme.primaryContainer)
                        .clickable {
                            isStatsVisible = !isStatsVisible
                        }
                        .padding(10.dp)
                ){
                    Text(
                        if (isStatsVisible)  "Hide stats"
                        else "Show stats"
                        , style = MaterialTheme.typography.labelSmall)
                }
                // copy button
                ChatActionButtons(
                    icon = R.drawable.content_copy,
                    title = "Copy"
                ) { }
                //share button
                ChatActionButtons(
                    icon = R.drawable.share,
                    title = "Share"
                ){

                }
            }
        }
        AnimatedVisibility(
            visible = isStatsVisible
        ) {
            Card{
                Row {
                    if (message.statistics?.timeToFirstToken!=null){
                        StatsItem(
                            title = "1st token",
                            value = message.statistics.timeToFirstToken.roundTo2Decimals().toString(),
                            unit = "sec"
                        )
                    }
                    if (message.statistics?.prefillSpeed!=null){
                        StatsItem(
                            title = "Prefill speed",
                            value = message.statistics.prefillSpeed.roundTo2Decimals().toString(),
                            unit = "tokens/s"
                        )
                    }
                    if (message.statistics?.totalLatency!=null){
                        StatsItem(
                            title = "Latency",
                            value = message.statistics.totalLatency.roundTo2Decimals().toString(),
                            unit = "sec"
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun StatsItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String
) {
    Column(
        modifier=modifier.padding(10.dp)
    ) {
        Text(title,style = MaterialTheme.typography.labelMedium)
        Text(value,style = MaterialTheme.typography.labelLarge)
        Text(unit,style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ChatActionButtons(
    modifier: Modifier = Modifier,
    icon:Int,
    title: String,
    onClick: () -> Unit
){
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
        , horizontalArrangement = Arrangement.spacedBy(5.dp)
    ){
        Icon(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.size(10.dp)
        )
        Text(title, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun ModelListItem(
    modifier: Modifier = Modifier,
    model: com.rkbapps.tooai.db.entity.LlmModel,
    isSelected: Boolean,
    onSelectModel: () -> Unit
) {
    val color = animateColorAsState(
        if (isSelected) { MaterialTheme.colorScheme.primaryContainer }
        else { MaterialTheme.colorScheme.secondaryContainer }
    )
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onSelectModel,
        colors = CardDefaults.cardColors(containerColor = color.value)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Max Tokens: ${model.maxTokens}",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "Temp: ${model.temperature}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
