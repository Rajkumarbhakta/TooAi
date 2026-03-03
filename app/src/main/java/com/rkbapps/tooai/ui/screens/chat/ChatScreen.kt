package com.rkbapps.tooai.ui.screens.chat

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.utils.PredefinePrompts
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.TypeOfPrompt
import com.rkbapps.tooai.utils.copyText
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

    // prompt sheet
    var showPromptSheet by remember { mutableStateOf(false) }
    val promptSheetState = rememberModalBottomSheetState()


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
                            .clickable { showModelSheet = true }
                            .padding(8.dp)
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.llmModel?.displayName?:"Chat", style = MaterialTheme.typography.titleMedium,
                            overflow = TextOverflow.MiddleEllipsis,
                            maxLines = 1
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
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .padding(BottomAppBarDefaults.windowInsets.asPaddingValues())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp, max = 120.dp)
                            .padding(bottom = 10.dp),
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Ask tooai") },
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

                        },
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp,)
                    ) {
                        if (state.currentPromptType==null){
                            Icon(
                                painter = painterResource(R.drawable.tune),"",
                                modifier = Modifier.clickable{showPromptSheet = true}
                            )
                        }else{
                            state.currentPromptType?.let { prompt->
                                PromptChip(prompt = prompt, showClose = true) {
                                    viewModel.selectAPredefinePrompt(null)
                                }
                            }
                        }
                    }
                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
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
        //prompt bottom sheet
        if (showPromptSheet){
            ModalBottomSheet(
                onDismissRequest = { showPromptSheet = false },
                sheetState = promptSheetState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = innerPadding.calculateTopPadding()),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        PredefinePrompts.listOfPrompts,
                        key = { it.subType }
                    ) { prompt ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color =
                                        if (state.currentPromptType == prompt) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer
                                )
                                .clickable {
                                    viewModel.selectAPredefinePrompt(prompt)
                                    scope.launch {
                                        promptSheetState.hide()
                                        showPromptSheet = false
                                    }
                                }
                                .padding(16.dp)
                        ){
                            Text("${prompt.type.displayString} : ${prompt.subType}")
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
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                reverseLayout = true
            ) {
                item {
                    Spacer(modifier = Modifier.size(8.dp))
                }
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
    val context = LocalContext.current
    var isStatsVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    val prompt =  getPromptTypeIfApplied(message.message)
    val isPromptApplied = prompt!=null


    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Card(
            onClick = {
                if (message.type == ChatType.ASSISTANT){
                    isExpanded = !isExpanded
                }
                isStatsVisible = false
            },
            colors = CardDefaults.cardColors(containerColor = color)
        ) {
            if (isPromptApplied){
                PromptChip(
                    prompt = Prompts(type = prompt, subType = "", prompt = ""),
                    showClose = false
                ) { }
            }

            if (message.isResponding){
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp).size(24.dp),
                    strokeWidth = 3.dp
                )
            }else{
                RichText(
                    modifier = Modifier
                        .background(color)
                        .padding(8.dp),
                ){
                    if (isPromptApplied){
                        val updatedMessage = removePromptIfApplied(message.message)
                        Markdown(content = updatedMessage)
                    }else{
                        Markdown(content = message.message)
                    }
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
                ) {
                    context.copyText(message.message)
                }
                //share button
                ChatActionButtons(
                    icon = R.drawable.share,
                    title = "Share"
                ){
                    Intent(Intent.ACTION_SEND).apply {
                        putExtra(Intent.EXTRA_TEXT, message.message)
                        type = "text/plain"
                    }.also {
                        context.startActivity(it)
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = isStatsVisible
        ) {
            Card{
                Row {
                    if (message.statistics?.tokenUsed!=null){
                        StatsItem(
                            title = "Token\nused",
                            value = message.statistics.tokenUsed.toString(),
                            unit = ""
                        )
                    }

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

fun getPromptTypeIfApplied(message:String): TypeOfPrompt?{
    when{
        message.startsWith(PredefinePrompts.REWRITE_FORMAL) ||
                message.startsWith(PredefinePrompts.REWRITE_FRIENDLY) ||
                message.startsWith(PredefinePrompts.REWRITE_FRIENDLY) ->{
            return TypeOfPrompt.Rewrite
        }
        message.startsWith(PredefinePrompts.SUMMARY_BULLET_POINT) ||
        message.startsWith(PredefinePrompts.SUMMARY_CONCISE) ||
        message.startsWith(PredefinePrompts.SUMMARY_SHORT_PARAGRAPH) ->{
            return TypeOfPrompt.Summary
        }
        message.startsWith(PredefinePrompts.CODE_SNIPPET_CPP) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_JAVA) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_KOTLIN) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_PYTHON) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_SWIFT) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_JAVA_SCRIPT) ||
                message.startsWith(PredefinePrompts.CODE_SNIPPET_JAVA_SCRIPT) -> {
                    return TypeOfPrompt.CodeSnippet
                }
        else ->{
            return  null
        }
    }
}

fun removePromptIfApplied(message: String): String{
    val isApplied = PredefinePrompts.listOfPrompts.any { message.startsWith(it.prompt,ignoreCase = true) }
    return if (isApplied){
        message.removePrefix(PredefinePrompts.listOfPrompts.first { message.startsWith(it.prompt,ignoreCase = true) }.prompt)
    }else{
        message
    }
}

@Composable
fun PromptChip(
    modifier: Modifier = Modifier,
    prompt: Prompts,
    showClose: Boolean,
    onClick: () -> Unit
    ) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ){
        Text(prompt.type.name + (if(prompt.subType.isNotBlank())" : ${prompt.subType}" else ""), color = MaterialTheme.colorScheme.onPrimary)
        if (showClose)
        Icon(painter = painterResource(R.drawable.close),"", tint = MaterialTheme.colorScheme.onPrimary)
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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onSelectModel,
        colors = CardDefaults.cardColors(containerColor = color.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
