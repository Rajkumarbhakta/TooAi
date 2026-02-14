package com.rkbapps.tooai.ui.screens.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.RichText
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.utils.roundTo2Decimals

@Composable
fun ChatScreen(
    backStack: SnapshotStateList<Any>,
    modelId:Long,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.chatState.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(modelId) {
        viewModel.initializeModel(modelId)
    }

    Scaffold(
        topBar = {
            TopBar(
                state.llmModel?.name ?: "Chat"
            ) {
                backStack.removeLastOrNull()
            }
        }
    ) {innerPadding->
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
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(
                    state.messages.reversed(),
                    key = {
                        it.id
                    }
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
            Card() {
                Row() {
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
)
{
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
