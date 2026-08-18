package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.ChatSession
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.navigation.IdType
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.utils.ModelConfigs
import com.rkbapps.tooai.utils.getFileName
import com.rkbapps.tooai.utils.getFileNameAndSize
import com.rkbapps.tooai.utils.toDateTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndModelManagerScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: ChatAndModelManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val filteredModels = remember(llmModels, searchQuery) {
        if (searchQuery.isBlank()) llmModels
        else llmModels.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredChats = remember(chatSessions, searchQuery) {
        if (searchQuery.isBlank()) chatSessions
        else chatSessions.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val modelMap = remember(llmModels) {
        llmModels.associateBy { it.id }
    }

    val tabs = listOf("Chats (${chatSessions.size})", "Models (${llmModels.size})")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    var showConfigurationDialog by remember { mutableStateOf<Pair<LlmModel?, Uri?>>(null to null) }
    var modelImportProgress by remember { mutableFloatStateOf(0f) }
    var deletableModel by remember { mutableStateOf<LlmModel?>(null) }
    var deletableChat by remember { mutableStateOf<ChatSession?>(null) }
    var updatableChat by remember { mutableStateOf<ChatSession?>(null) }
    var error by remember { mutableStateOf<Pair<String?, String>>(null to "") }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val fileName = context.getFileName(uri = uri)
                val nameAndSize = context.getFileNameAndSize(uri = uri)
                if (fileName != null && !fileName.endsWith(".task") && !fileName.endsWith(".litertlm")) {
                    error = "Unsupported File Type" to "$fileName is not supported. Only .task and .litertlm files are supported."
                } else if (fileName == null || nameAndSize.second == 0L || fileName.lowercase().contains("-web")) {
                    error = "Unsupported File Type" to "$fileName is not supported. Only .task and .litertlm files are supported."
                } else {
                    val model = LlmModel(
                        name = fileName,
                        displayName = nameAndSize.first,
                        sizeInBytes = nameAndSize.second,
                        path = "",
                        fileLocation = "",
                        maxTokens = ModelConfigs.DEFAULT_MAX_TOKEN,
                        topK = ModelConfigs.DEFAULT_TOP_K,
                        topP = ModelConfigs.DEFAULT_TOP_P,
                        temperature = ModelConfigs.DEFAULT_TEMPERATURE,
                        createdAt = System.currentTimeMillis()
                    )
                    showConfigurationDialog = model to uri
                    Log.d("File Picker", "File picked : $model")
                }
            }
        } else {
            Log.d("File Picker", "File picking cancelled.")
        }
    }

    LaunchedEffect(modelImportProgress) {
        if (modelImportProgress == 1f) {
            delay(500.milliseconds)
            modelImportProgress = 0f
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Models & Chats") {
                backStack.removeLastOrNull()
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        if (llmModels.isNotEmpty()) {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        } else {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                            }
                            modelPickerLauncher.launch(intent)
                        }
                    } else {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                        }
                        modelPickerLauncher.launch(intent)
                    }
                },
                icon = {
                    AnimatedContent(
                        targetState = pagerState.currentPage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "fab_icon"
                    ) { page ->
                        if (page == 0) {
                            Icon(
                                painter = painterResource(R.drawable.chatbot),
                                contentDescription = "New Chat"
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = "Import Model"
                            )
                        }
                    }
                },
                text = {
                    AnimatedContent(
                        targetState = pagerState.currentPage,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "fab_text"
                    ) { page ->
                        Text(if (page == 0) "New Chat" else "Import Model")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (modelImportProgress > 0f) {
            Dialog(onDismissRequest = { modelImportProgress = 0f }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Importing Model...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { modelImportProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = "${(modelImportProgress * 100).toInt()}% completed",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Delete Model Dialog
        deletableModel?.let { model ->
            AlertDialog(
                onDismissRequest = { deletableModel = null },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete ${model.displayName}?") },
                text = { Text("Are you sure you want to delete ${model.displayName}? This model file will be permanently removed.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteModel(model)
                            deletableModel = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletableModel = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Model Configuration Dialog
        showConfigurationDialog.first?.let { model ->
            LlmModelConfigurationDialog(
                model = model,
                confirmButtonText = if (showConfigurationDialog.second == null) "Update" else "Import",
                onDismiss = { showConfigurationDialog = null to null }
            ) { updatedModel ->
                if (showConfigurationDialog.second != null) {
                    viewModel.importModel(
                        context = context,
                        fileSize = updatedModel.sizeInBytes,
                        fileName = updatedModel.displayName,
                        uri = showConfigurationDialog.second!!,
                        onDone = { path ->
                            val update = model.copy(
                                path = path,
                                fileLocation = "${ModelConfigs.IMPORT_DIR}/${model.displayName}"
                            )
                            viewModel.addModel(update)
                            showConfigurationDialog = null to null
                        },
                        onProgress = { value -> modelImportProgress = value },
                        onError = { err ->
                            modelImportProgress = 0f
                            showConfigurationDialog = null to null
                            error = "Error Configuration" to err
                        }
                    )
                } else {
                    viewModel.updateModel(model = updatedModel)
                    showConfigurationDialog = null to null
                }
            }
        }

        // Delete Chat Dialog
        deletableChat?.let { chat ->
            AlertDialog(
                onDismissRequest = { deletableChat = null },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete Chat?") },
                text = { Text("Are you sure you want to delete \"${chat.title}\"?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteChat(chat)
                            deletableChat = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deletableChat = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Update Chat Name Dialog
        updatableChat?.let { chat ->
            var name by remember { mutableStateOf(chat.title) }
            AlertDialog(
                onDismissRequest = { updatableChat = null },
                title = { Text("Rename Chat") },
                text = {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Chat Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                viewModel.updateChat(chat.copy(title = name))
                            }
                            updatableChat = null
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { updatableChat = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error Dialog
        if (error.second.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { error = null to "" },
                title = { Text(error.first ?: "Error") },
                text = { Text(error.second) },
                confirmButton = {
                    Button(onClick = { error = null to "" }) {
                        Text("OK")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            SegmentedTabRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                tabItems = tabs,
                selectedTabIndex = pagerState.currentPage
            ) { selectedTab ->
                scope.launch {
                    if (pagerState.currentPage != selectedTab)
                        pagerState.animateScrollToPage(page = selectedTab)
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = if (pagerState.currentPage == 0) "Search chats..." else "Search models...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.search),
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Clear search",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> ChatList(
                        chats = filteredChats,
                        isSearching = searchQuery.isNotBlank(),
                        modelMap = modelMap,
                        onDelete = { chat -> deletableChat = chat },
                        onConfigClick = { chat -> updatableChat = chat },
                        onChatItemClick = { chat ->
                            backStack.add(NavigationEntry.AiChat(id = chat.id, type = IdType.CHAT))
                        },
                        onBrowseModels = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        onClearSearch = { searchQuery = "" }
                    )
                    1 -> ModelList(
                        models = filteredModels,
                        isSearching = searchQuery.isNotBlank(),
                        onDelete = { model -> deletableModel = model },
                        onConfigClick = { model -> showConfigurationDialog = model to null },
                        onModelItemClick = { model ->
                            backStack.add(NavigationEntry.AiChat(id = model.id.toString(), type = IdType.MODEL))
                        },
                        onImportModel = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                            }
                            modelPickerLauncher.launch(intent)
                        },
                        onClearSearch = { searchQuery = "" }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelList(
    modifier: Modifier = Modifier,
    models: List<LlmModel>,
    isSearching: Boolean,
    onDelete: (LlmModel) -> Unit,
    onConfigClick: (LlmModel) -> Unit,
    onModelItemClick: (LlmModel) -> Unit,
    onImportModel: () -> Unit,
    onClearSearch: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    if (models.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.download),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "No matching models" else "No AI Models Installed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearching) "Try searching with a different model name or keyword."
                else "Import local .task or .litertlm model files or download community models.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (isSearching) {
                OutlinedButton(onClick = onClearSearch) {
                    Text("Clear Search")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onImportModel) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painter = painterResource(R.drawable.add), contentDescription = null)
                            Text("Import Model")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            try{
                                uriHandler.openUri("https://huggingface.co/litert-community/models")
                            }catch (e: Exception){}
                        }
                    ) {
                        Text("Download Models")
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }
            items(models, key = { it.id }) { llmModel ->
                LlmModelItemUi(
                    model = llmModel,
                    onDelete = onDelete,
                    onConfigClick = onConfigClick,
                    onClick = onModelItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ChatList(
    modifier: Modifier = Modifier,
    chats: List<ChatSession>,
    isSearching: Boolean,
    modelMap: Map<Long, LlmModel>,
    onDelete: (ChatSession) -> Unit,
    onConfigClick: (ChatSession) -> Unit,
    onChatItemClick: (ChatSession) -> Unit,
    onBrowseModels: () -> Unit,
    onClearSearch: () -> Unit
) {
    if (chats.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chatbot),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearching) "No matching chats" else "No Chat History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isSearching) "No conversations found matching your search term."
                else "Select an installed LLM model to start a new AI conversation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            if (isSearching) {
                OutlinedButton(onClick = onClearSearch) {
                    Text("Clear Search")
                }
            } else {
                Button(onClick = onBrowseModels) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.tune), contentDescription = null)
                        Text("Browse Models & Chat")
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(6.dp)) }
            items(chats, key = { it.id }) { chat ->
                ChatItem(
                    chat = chat,
                    associatedModel = modelMap[chat.modelId],
                    onDelete = onDelete,
                    onConfigClick = onConfigClick,
                    onChatItemClick = onChatItemClick
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ChatItem(
    modifier: Modifier = Modifier,
    chat: ChatSession,
    associatedModel: LlmModel? = null,
    onDelete: (ChatSession) -> Unit,
    onConfigClick: (ChatSession) -> Unit,
    onChatItemClick: (ChatSession) -> Unit
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onChatItemClick(chat) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.chatbot),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.access_time),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = chat.createdAt.toDateTimeString(pattern = "dd MMM yyyy, hh:mm a"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (associatedModel != null) {
                AssistChip(
                    onClick = { onChatItemClick(chat) },
                    label = {
                        Text(
                            text = "Model: ${associatedModel.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.sparkles),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onConfigClick(chat) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tune),
                            contentDescription = "Rename Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDelete(chat) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete Chat",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                FilledTonalButton(
                    onClick = { onChatItemClick(chat) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Chat")
                        Icon(
                            painter = painterResource(R.drawable.arrow_forward),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LlmModelItemUi(
    modifier: Modifier = Modifier,
    model: LlmModel,
    onDelete: (LlmModel) -> Unit,
    onConfigClick: (LlmModel) -> Unit,
    onClick: (LlmModel) -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick(model) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sparkles),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.displayName.ifBlank { model.name },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = formatFileSize(model.sizeInBytes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParamBadge(label = "Tokens: ${model.maxTokens}")
                ParamBadge(label = "Temp: ${model.temperature}")
                ParamBadge(label = "Top K: ${model.topK}")
                ParamBadge(label = "Top P: ${model.topP}")
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { onConfigClick(model) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.tune),
                            contentDescription = "Configure Model",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDelete(model) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete Model",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Button(
                    onClick = { onClick(model) },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.chatbot),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Start Chat")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParamBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun SegmentedTabRow(
    modifier: Modifier = Modifier,
    tabItems: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        tabItems.forEachIndexed { index, title ->
            RowTabItem(
                modifier = Modifier.weight(1f),
                isSelected = index == selectedTabIndex,
                title = title,
                iconRes = if (index == 0) R.drawable.chatbot else R.drawable.sparkles,
                onClick = { onTabSelected(index) }
            )
        }
    }
}

@Composable
fun RowTabItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    val color by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tab_content"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(color = color)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
        else -> "$bytes B"
    }
}

@Preview
@Composable
fun TabRowUiPreview() {
    TooAiTheme {
        SegmentedTabRow(
            tabItems = listOf("Chats (2)", "Models (3)"),
            selectedTabIndex = 0,
            onTabSelected = {}
        )
    }
}

@Preview
@Composable
fun LlmModelItemUiPreview() {
    TooAiTheme {
        LlmModelItemUi(
            model = LlmModel(
                name = "Qwen3-0.6B.litertlm",
                displayName = "Qwen 3 (0.6B)",
                sizeInBytes = 614236160,
                path = "",
                fileLocation = "",
                maxTokens = ModelConfigs.DEFAULT_MAX_TOKEN,
                topK = ModelConfigs.DEFAULT_TOP_K,
                topP = ModelConfigs.DEFAULT_TOP_P,
                temperature = ModelConfigs.DEFAULT_TEMPERATURE,
                createdAt = 170000
            ),
            onDelete = {},
            onConfigClick = {},
            onClick = {}
        )
    }
}

@Preview
@Composable
fun ChatItemPreview() {
    TooAiTheme {
        ChatItem(
            chat = ChatSession(
                title = "Chat with Qwen 3",
                modelId = 1,
                createdAt = System.currentTimeMillis()
            ),
            associatedModel = LlmModel(
                name = "Qwen3-0.6B.litertlm",
                displayName = "Qwen 3 (0.6B)",
                sizeInBytes = 614236160,
                path = "",
                fileLocation = "",
                maxTokens = 1024,
                topK = 64,
                topP = 0.9,
                temperature = 1.0,
                createdAt = System.currentTimeMillis()
            ),
            onDelete = {},
            onConfigClick = {},
            onChatItemClick = {}
        )
    }
}

