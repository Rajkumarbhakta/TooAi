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
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.toolPalette
import com.rkbapps.tooai.utils.PredefinePrompts
import com.rkbapps.tooai.utils.Prompts
import com.rkbapps.tooai.utils.TypeOfPrompt
import com.rkbapps.tooai.utils.copyText
import com.rkbapps.tooai.utils.roundTo2Decimals
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    backStack: SnapshotStateList<Any>,viewModel: ChatViewModel = hiltViewModel(),
) {
    SurfaceStatusBar()

    val state by viewModel.chatState.collectAsStateWithLifecycle()
    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }

    // Bottom sheet state
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    var showModelSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // prompt sheet
    var showPromptSheet by remember { mutableStateOf(false) }
    val promptSheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val modelName = state.llmModel?.displayName

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            ChatTopBar(
                title = state.sessionTitle ?: "Chat",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { backStack.removeLastOrNull() },
                modelChip = modelName?.let { name ->
                    {
                        ModelChip(
                            name = name,
                            isReady = state.instance != null,
                            enabled = !state.isChatRunning,
                            onClick = { showModelSheet = true }
                        )
                    }
                },
                newChatEnabled = !state.isChatRunning && state.messages.isNotEmpty(),
                onNewChat = viewModel::startNewChat
            )
        },
        bottomBar = {
            ChatComposer(
                messageText = messageText,
                onMessageChange = { messageText = it },
                placeholder = modelName?.let { "Message $it" } ?: "Message",
                isLoadingModel = state.modelInitializingStatus.isLoading && state.instance == null,
                isGenerating = state.isChatRunning,
                selectedPrompt = state.currentPromptType,
                onClearPrompt = { viewModel.selectAPredefinePrompt(null) },
                onShowPrompts = { showPromptSheet = true },
                onSend = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                onStop = viewModel::stopGeneration
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) {innerPadding->

        // Bottom Sheet for model selection
        if (showModelSheet) {
            ChatModelSelectionSheet(
                sheetState = sheetState,
                llmModels = llmModels,
                selectedModel = state.llmModel,
                onSheetDismiss = { showModelSheet = false  },
            ){model ->
                state.sessionId?.let { sessionId ->
                    viewModel.loadSession(
                        sessionId = sessionId,
                        modelId = model.id
                    )
                    scope.launch {
                        sheetState.hide()
                        showModelSheet = false
                    }
                }
            }
        }
        //prompt bottom sheet
        if (showPromptSheet){
            ChatPromptSuggestionSheet(
                sheetState = promptSheetState,
                topPadding = innerPadding.calculateTopPadding(),
                selectedPrompt = state.currentPromptType,
                onSheetDismiss = {
                    showPromptSheet = false
                }
            ){prompt ->
                viewModel.selectAPredefinePrompt(prompt)
                scope.launch {
                    promptSheetState.hide()
                    showPromptSheet = false
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (state.modelInitializingStatus.isLoading && state.messages.isNotEmpty()) {
                LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (state.messages.isEmpty()) {
                ChatEmptyState(
                    modifier = Modifier.weight(1f),
                    isLoading = state.modelInitializingStatus.isLoading,
                    error = state.modelInitializingStatus.error,
                    modelName = modelName
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Bottom)
                ) {
                    items(
                        state.messages.reversed(),
                        key = { it.id }
                    ) { message ->
                        when(message.type){
                            ChatType.CHAT -> UserMessageItem(message = message)
                            ChatType.ASSISTANT -> AssistantMessageItem(
                                message = message,
                                modelName = modelName,
                                isGenerating = state.isChatRunning &&
                                        message.id == state.messages.last().id
                            )
                        }
                    }
                }
            }
        }
    }

}

// A chat needs the vertical space, so it keeps a single-row bar rather than the large
// collapsing one the other migrated screens use.
@Composable
private fun ChatTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClick: () -> Unit,
    modelChip: (@Composable () -> Unit)?,
    newChatEnabled: Boolean,
    onNewChat: () -> Unit
) {
    val titleContent: @Composable () -> Unit = {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = onNavigationIconClick) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "navigation up"
            )
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        FilledTonalIconButton(
            onClick = onNewChat,
            enabled = newChatEnabled,
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(painter = painterResource(R.drawable.add), "New chat")
        }
    }
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    if (modelChip != null) {
        TopAppBar(
            title = titleContent,
            subtitle = modelChip,
            navigationIcon = navigationIcon,
            actions = actions,
            expandedHeight = 80.dp,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    } else {
        TopAppBar(
            title = titleContent,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
private fun ModelChip(
    name: String,
    isReady: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (isReady) {
                            MaterialTheme.toolPalette.qr.action
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
            )
            Text(
                text = "$name · CPU",
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMediumEmphasized,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
            Icon(
                painter = painterResource(R.drawable.arrow_drop_down),
                contentDescription = "Change model",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatEmptyState(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    error: String?,
    modelName: String?
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            error != null -> Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            isLoading -> {
                ContainedLoadingIndicator(modifier = Modifier.size(64.dp))
                Text(
                    text = "Warming up the model…",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = ToolShapes.AiChat.toShape()
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chatbot),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Text(
                    text = "Start a conversation",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
                modelName?.let {
                    Text(
                        text = "$it runs entirely on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatComposer(
    messageText: String,
    onMessageChange: (String) -> Unit,
    placeholder: String,
    isLoadingModel: Boolean,
    isGenerating: Boolean,
    selectedPrompt: Prompts?,
    onClearPrompt: () -> Unit,
    onShowPrompts: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val fieldColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedPrompt?.let { prompt ->
                PromptChip(prompt = prompt, showClose = true, onClick = onClearPrompt)
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp, max = 120.dp),
                    value = messageText,
                    onValueChange = onMessageChange,
                    placeholder = {
                        Text(text = placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    shape = RoundedCornerShape(28.dp),
                    trailingIcon = {
                        // Always available, so the sheet can be reopened to swap the active
                        // prompt instead of having to clear it first.
                        IconButton(onClick = onShowPrompts) {
                            Icon(
                                painter = painterResource(R.drawable.tune),
                                contentDescription = "Prompt suggestions"
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = fieldColor,
                        unfocusedContainerColor = fieldColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                val canSend = !isLoadingModel && !isGenerating && messageText.isNotBlank()
                FilledIconButton(
                    onClick = if (isGenerating) onStop else onSend,
                    enabled = isGenerating || canSend,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    when {
                        isLoadingModel -> LoadingIndicator(modifier = Modifier.size(36.dp))
                        isGenerating -> Icon(
                            painter = painterResource(R.drawable.stop),
                            contentDescription = "Stop generating"
                        )
                        else -> Icon(
                            painter = painterResource(R.drawable.send),
                            contentDescription = "Send message"
                        )
                    }
                }
            }
            Text(
                text = "Runs offline on this device · AI can make mistakes",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun UserMessageItem(message: ChatMessage) {
    val prompt = getPromptTypeIfApplied(message.message)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        prompt?.let {
            PromptChip(
                prompt = Prompts(type = it, subType = "", prompt = ""),
                showClose = false
            ) { }
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomEnd = 6.dp,
                bottomStart = 24.dp
            ),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = removePromptIfApplied(message.message),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AssistantMessageItem(
    message: ChatMessage,
    modelName: String?,
    isGenerating: Boolean
) {
    if (isGenerating && message.message.isEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ContainedLoadingIndicator(modifier = Modifier.size(48.dp))
            Text(
                text = "Thinking…",
                style = MaterialTheme.typography.titleSmallEmphasized
            )
        }
        return
    }

    val context = LocalContext.current
    var isStatsVisible by remember { mutableStateOf(false) }
    val statistics = message.statistics
    val statsSummary = statistics?.let {
        listOfNotNull(
            it.decodeSpeed?.let { speed -> "${speed.roundTo2Decimals()} tok/s" },
            it.tokenUsed?.let { tokens -> "$tokens tokens" }
        ).joinToString(" · ")
    }.orEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modelName?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(24.dp)
                        .background(
                           color = MaterialTheme.colorScheme.primary,
                           shape = ToolShapes.AiChat.toShape()
                        ) ,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        RichText {
            Markdown(content = message.message)
        }

        if (!isGenerating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = statsSummary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = statsSummary.isNotEmpty()) {
                            isStatsVisible = !isStatsVisible
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { context.copyText(message.message) }) {
                    Icon(
                        painter = painterResource(R.drawable.content_copy),
                        contentDescription = "Copy",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, message.message)
                            type = "text/plain"
                        }.also {
                            context.startActivity(it)
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isStatsVisible
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row {
                    if (statistics?.tokenUsed!=null){
                        StatsItem(
                            title = "Token\nused",
                            value = statistics.tokenUsed.toString(),
                            unit = ""
                        )
                    }

                    if (statistics?.timeToFirstToken!=null){
                        StatsItem(
                            title = "1st token",
                            value = statistics.timeToFirstToken.roundTo2Decimals().toString(),
                            unit = "sec"
                        )
                    }
                    if (statistics?.prefillSpeed!=null){
                        StatsItem(
                            title = "Prefill speed",
                            value = statistics.prefillSpeed.roundTo2Decimals().toString(),
                            unit = "tokens/s"
                        )
                    }
                    if (statistics?.totalLatency!=null){
                        StatsItem(
                            title = "Latency",
                            value = statistics.totalLatency.roundTo2Decimals().toString(),
                            unit = "sec"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Derived from [PredefinePrompts.listOfPrompts] rather than a hand-written list of prefixes, so it
 * cannot drift as prompts are added. Matches [removePromptIfApplied] exactly — the two must agree,
 * or a message keeps its raw prompt prefix on screen.
 */
fun getPromptTypeIfApplied(message: String): TypeOfPrompt? =
    PredefinePrompts.listOfPrompts
        .firstOrNull { message.startsWith(it.prompt, ignoreCase = true) }
        ?.type

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
    val label = prompt.type.displayString + (if(prompt.subType.isNotBlank())" : ${prompt.subType}" else "")
    Surface(
        modifier = modifier,
        onClick = onClick,
        enabled = showClose,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ){
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelLargeEmphasized)
            if (showClose)
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Remove prompt",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatPromptSuggestionSheet(
    sheetState: SheetState,
    topPadding: Dp,
    selectedPrompt: Prompts?,
    onSheetDismiss: () -> Unit,
    onPromptSelection:(prompt: Prompts?)-> Unit,
) {
    val groupedPrompts = remember { PredefinePrompts.listOfPrompts.groupBy { it.type } }

    ModalBottomSheet(
        onDismissRequest = onSheetDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Prompt suggestions",
                        style = MaterialTheme.typography.headlineSmallEmphasized
                    )
                    Text(
                        "Added in front of your next message",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = selectedPrompt != null) {
                    TextButton(onClick = { onPromptSelection(null) }) {
                        Text("Clear")
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedPrompts.forEach { (type, prompts) ->
                    item(key = "header-${type.name}") {
                        Text(
                            text = type.displayString,
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)
                        )
                    }
                    items(
                        prompts,
                        // subType alone is not unique across types, so qualify it.
                        key = { "${type.name}-${it.subType}" }
                    ) { prompt ->
                        PromptSuggestionItem(
                            prompt = prompt,
                            isSelected = selectedPrompt == prompt,
                            onClick = { onPromptSelection(prompt) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptSuggestionItem(
    modifier: Modifier = Modifier,
    prompt: Prompts,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = containerColor,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prompt.subType,
                    style = MaterialTheme.typography.titleSmall
                )
//                Text(
//                    text = prompt.previewText(),
//                    style = MaterialTheme.typography.bodySmall,
//                    color = LocalContentColor.current.copy(alpha = 0.7f),
//                    maxLines = 2,
//                    overflow = TextOverflow.Ellipsis
//                )
            }
            // onClick = null so the whole card stays the single tap target.
            RadioButton(selected = isSelected, onClick = null)
        }
    }
}

/**
 * The stored prompts are prefixes ("... using a formal tone: ", "Write a Kotlin code snippet to ").
 * Show them as readable descriptions instead of raw templates.
 */
private fun Prompts.previewText(): String {
    val trimmed = prompt.trim()
    return if (trimmed.endsWith(":")) trimmed.dropLast(1).trim() else "$trimmed…"
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
@OptIn(ExperimentalMaterial3Api::class)
fun ChatModelSelectionSheet(
    sheetState: SheetState,
    llmModels:  List<LlmModel>,
    selectedModel: LlmModel?,
    onSheetDismiss:()-> Unit,
    onModelSelect:(model: LlmModel)-> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onSheetDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Available models",
                style = MaterialTheme.typography.headlineSmallEmphasized,
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
                        isSelected = selectedModel?.id == model.id,
                        onSelectModel = {
                            onModelSelect(model)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelListItem(
    modifier: Modifier = Modifier,
    model: LlmModel,
    isSelected: Boolean,
    onSelectModel: () -> Unit
) {
    val color = animateColorAsState(
        if (isSelected) { MaterialTheme.colorScheme.primaryContainer }
        else { MaterialTheme.colorScheme.surfaceContainerHighest }
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onSelectModel,
        shape = MaterialTheme.shapes.large,
        color = color.value
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
                        style = MaterialTheme.typography.titleMediumEmphasized
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
