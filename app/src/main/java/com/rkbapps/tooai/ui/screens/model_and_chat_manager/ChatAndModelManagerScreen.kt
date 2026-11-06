package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.StatFs
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.PillSearchField
import com.rkbapps.tooai.ui.screens.chat.removePromptIfApplied
import com.rkbapps.tooai.ui.theme.GroupedListDefaults
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.utils.ModelConfigs
import com.rkbapps.tooai.utils.dateGroupLabel
import com.rkbapps.tooai.utils.formatFileSize
import com.rkbapps.tooai.utils.getFileName
import com.rkbapps.tooai.utils.getFileNameAndSize
import com.rkbapps.tooai.utils.toDateTimeString
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndModelManagerScreen(
    backStack: SnapshotStateList<Any>,
    initialPage: Int = 0,
    viewModel: ChatAndModelManagerViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current

    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()
    val chatPreviews by viewModel.chatPreviews.collectAsStateWithLifecycle()

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

    val freeStorageBytes = remember(llmModels) {
        StatFs((context.getExternalFilesDir(null) ?: context.filesDir).path).availableBytes
    }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 2 })
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
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

    val launchModelPicker = {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }
        modelPickerLauncher.launch(intent)
    }

    val isConfiguringModel = showConfigurationDialog.first != null

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (isConfiguringModel) Modifier.clearAndSetSemantics {} else Modifier),
            topBar = {
                LargeCollapsingTopBar(
                    title = "Library",
                    scrollBehavior = scrollBehavior,
                    onNavigationIconClick = { backStack.removeLastOrNull() }
                )
            },
            containerColor = pageContainerColor(),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    modifier = Modifier.height(64.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        if (pagerState.currentPage == 0) {
                            if (llmModels.isNotEmpty()) {
                                scope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            } else {
                                launchModelPicker()
                            }
                        } else {
                            launchModelPicker()
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
                            Text(
                                text = if (page == 0) "New chat" else "Import model",
                                style = MaterialTheme.typography.titleMediumEmphasized
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->

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
                LibraryTabs(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedPage = pagerState.currentPage,
                    chatCount = chatSessions.size,
                    modelCount = llmModels.size
                ) { selectedTab ->
                    scope.launch {
                        if (pagerState.currentPage != selectedTab)
                            pagerState.animateScrollToPage(page = selectedTab)
                    }
                }

                PillSearchField(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    query = searchQuery,
                    placeholder = if (pagerState.currentPage == 0) "Search chats" else "Search models",
                    onQueryChange = { searchQuery = it }
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
                            previews = chatPreviews,
                            onDelete = { chat -> deletableChat = chat },
                            onRename = { chat -> updatableChat = chat },
                            onExport = { chat ->
                                viewModel.exportChat(chat) { transcript ->
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, chat.title)
                                        putExtra(Intent.EXTRA_TEXT, transcript)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Export chat"))
                                }
                            },
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
                            usedStorageBytes = llmModels.sumOf { it.sizeInBytes },
                            freeStorageBytes = freeStorageBytes,
                            modelCount = llmModels.size,
                            isSearching = searchQuery.isNotBlank(),
                            onDelete = { model -> deletableModel = model },
                            onConfigClick = { model -> showConfigurationDialog = model to null },
                            onModelItemClick = { model ->
                                backStack.add(NavigationEntry.AiChat(id = model.id.toString(), type = IdType.MODEL))
                            },
                            onImportModel = launchModelPicker,
                            onClearSearch = { searchQuery = "" }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isConfiguringModel,
            enter = slideInVertically(MaterialTheme.motionScheme.defaultSpatialSpec()) { it },
            exit = ExitTransition.None
        ) {
            showConfigurationDialog.first?.let { model ->
                val importUri = showConfigurationDialog.second
                LlmModelConfigurationScreen(
                    title = if (importUri == null) "Configure model" else "Import model",
                    confirmButtonText = if (importUri == null) "Update" else "Import model",
                    model = model,
                    importProgress = modelImportProgress,
                    fileSizeText = formatFileSize(model.sizeInBytes),
                    onChangeFile = if (importUri == null) null else launchModelPicker,
                    onDismiss = { showConfigurationDialog = null to null }
                ) { updatedModel ->
                    if (importUri != null) {
                        viewModel.importModel(
                            context = context,
                            fileSize = updatedModel.sizeInBytes,
                            // The display name can be edited freely, the file keeps its extension.
                            fileName = updatedModel.name,
                            uri = importUri,
                            onDone = { path ->
                                viewModel.addModel(
                                    updatedModel.copy(
                                        path = path,
                                        fileLocation = "${ModelConfigs.IMPORT_DIR}/${updatedModel.name}"
                                    )
                                )
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
        }
    }
}

@Composable
fun ModelList(
    modifier: Modifier = Modifier,
    models: List<LlmModel>,
    usedStorageBytes: Long,
    freeStorageBytes: Long,
    modelCount: Int,
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "storage") {
                ModelStorageCard(
                    modifier = Modifier.padding(top = 8.dp),
                    usedBytes = usedStorageBytes,
                    freeBytes = freeStorageBytes,
                    modelCount = modelCount
                )
            }
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
    previews: Map<String, String>,
    onDelete: (ChatSession) -> Unit,
    onRename: (ChatSession) -> Unit,
    onExport: (ChatSession) -> Unit,
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
        val groups = remember(chats) { chats.groupBy { dateGroupLabel(it.updatedAt) } }
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)
        ) {
            groups.forEach { (group, groupChats) ->
                item(key = group) {
                    Text(
                        text = group,
                        modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                itemsIndexed(groupChats, key = { _, chat -> chat.id }) { index, chat ->
                    ChatItem(
                        shape = GroupedListDefaults.shape(index, groupChats.size),
                        chat = chat,
                        associatedModel = modelMap[chat.modelId],
                        preview = previews[chat.id],
                        onDelete = onDelete,
                        onRename = onRename,
                        onExport = onExport,
                        onChatItemClick = onChatItemClick
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

private fun plainPreview(reply: String) =
    removePromptIfApplied(reply).replace(Regex("[*#`]"), "").replace(Regex("\\s+"), " ").trim()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ChatItem(
    modifier: Modifier = Modifier,
    shape: Shape,
    chat: ChatSession,
    associatedModel: LlmModel? = null,
    preview: String? = null,
    onDelete: (ChatSession) -> Unit,
    onRename: (ChatSession) -> Unit,
    onExport: (ChatSession) -> Unit,
    onChatItemClick: (ChatSession) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = if (isMenuOpen) MaterialTheme.colorScheme.primaryContainer else rowContainerColor(),
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "chat_row_bg"
    )
    val secondaryColor = if (isMenuOpen) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val time = chat.updatedAt.toDateTimeString(pattern = "dd MMM yyyy, hh:mm a")

    Surface(
        onClick = { onChatItemClick(chat) },
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                preview?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = plainPreview(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row {
                    associatedModel?.let {
                        Text(
                            text = it.displayName,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = " · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor
                        )
                    }
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryColor,
                        maxLines = 1
                    )
                }
            }

            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        tint = secondaryColor
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.edit), contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onRename(chat)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export as text") },
                        leadingIcon = {
                            Icon(painter = painterResource(R.drawable.share), contentDescription = null)
                        },
                        onClick = {
                            isMenuOpen = false
                            onExport(chat)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            onDelete(chat)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelStorageCard(
    modifier: Modifier = Modifier,
    usedBytes: Long,
    freeBytes: Long,
    modelCount: Int
) {
    val total = (usedBytes + freeBytes).coerceAtLeast(1L)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = rowContainerColor()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Model storage",
                    style = MaterialTheme.typography.titleSmallEmphasized
                )
                Text(
                    text = "${formatFileSize(usedBytes)} used · ${formatFileSize(freeBytes)} free",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearWavyProgressIndicator(
                progress = { usedBytes.toFloat() / total },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = if (modelCount == 1) "1 model on this device" else "$modelCount models on this device",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LlmModelItemUi(
    modifier: Modifier = Modifier,
    model: LlmModel,
    onDelete: (LlmModel) -> Unit,
    onConfigClick: (LlmModel) -> Unit,
    onClick: (LlmModel) -> Unit
) {
    val title = model.displayName.ifBlank { model.name }
    val fileDetails = listOfNotNull(
        model.name.takeIf { it != title },
        formatFileSize(model.sizeInBytes)
    ).joinToString(" · ")

    Surface(
        onClick = { onClick(model) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = rowContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.sparkles),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                    Text(
                        text = fileDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ParamBadge(label = "CPU")
                ParamBadge(label = "Temp ${model.temperature}")
                ParamBadge(label = "Top-k ${model.topK}")
                ParamBadge(label = "Top-p ${model.topP}")
                ParamBadge(label = "${model.maxTokens} tokens")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onClick(model) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = "Start chat",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        maxLines = 1
                    )
                }
                FilledTonalButton(
                    onClick = { onConfigClick(model) },
                    modifier = Modifier.heightIn(min = 44.dp),
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(
                        text = "Configure",
                        style = MaterialTheme.typography.labelLargeEmphasized,
                        maxLines = 1
                    )
                }
                OutlinedIconButton(
                    onClick = { onDelete(model) },
                    modifier = Modifier.size(44.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = "Delete Model",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParamBadge(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryTabs(
    modifier: Modifier = Modifier,
    selectedPage: Int,
    chatCount: Int,
    modelCount: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        LibraryTab(
            modifier = Modifier.weight(1f),
            title = "Chats",
            iconRes = R.drawable.chatbot,
            count = chatCount,
            selected = selectedPage == 0,
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
            onClick = { onTabSelected(0) }
        )
        LibraryTab(
            modifier = Modifier.weight(1f),
            title = "Models",
            iconRes = R.drawable.sparkles,
            count = modelCount,
            selected = selectedPage == 1,
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
            onClick = { onTabSelected(1) }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryTab(
    modifier: Modifier = Modifier,
    title: String,
    iconRes: Int,
    count: Int,
    selected: Boolean,
    shapes: ToggleButtonShapes,
    onClick: () -> Unit
) {
    ToggleButton(
        checked = selected,
        onCheckedChange = { onClick() },
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { role = Role.Tab },
        icon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = title,
            style = if (selected) {
                MaterialTheme.typography.labelLargeEmphasized
            } else {
                MaterialTheme.typography.labelLarge
            },
            maxLines = 1
        )
        if (!selected) {
            Surface(
                modifier = Modifier.padding(start = 8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "$count",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        }
    }
}

@Preview
@Composable
fun TabRowUiPreview() {
    TooAiTheme {
        LibraryTabs(
            selectedPage = 0,
            chatCount = 2,
            modelCount = 3,
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
            shape = GroupedListDefaults.shape(0, 1),
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
            onRename = {},
            onExport = {},
            onChatItemClick = {}
        )
    }
}

