package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndModelManagerScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: ChatAndModelManagerViewModel = hiltViewModel()
) {

    val context = LocalContext.current

    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()
    val chatSessions by viewModel.chatSessions.collectAsStateWithLifecycle()

    val tabs = listOf("Chats", "Models")
    val pagerState = rememberPagerState(
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()

    var showConfigurationDialog by remember { mutableStateOf<Pair<LlmModel?, Uri?>>(null to null) }
    var modelImportProgress by remember { mutableFloatStateOf(0f) }
    var deletableModel by remember { mutableStateOf<LlmModel?>(null) }

    var deletableChat by remember { mutableStateOf<ChatSession?>(null) }
    var updatableChat by remember { mutableStateOf<ChatSession?>(null) }


    var error by remember { mutableStateOf<Pair<String?, String>>(null to "") }

    val modelPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    val fileName = context.getFileName(uri = uri)
                    val nameAndSize = context.getFileNameAndSize(uri = uri)
                    if (fileName != null && !fileName.endsWith(".task") && !fileName.endsWith(".litertlm")) {
                        // unsupported file
                        error =
                            "Unsupported file type" to "$fileName is not supported. Only .task and .litertlm files are supported."
                    } else if (fileName == null || nameAndSize.second == 0L || fileName.lowercase()
                            .contains("-web")
                    ) {
                        // unsupported file
                        error =
                            "Unsupported file type" to "$fileName is not supported. Only .task and .litertlm files are supported."
                    } else {
                        //import model
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
                //canceled
                Log.d("File Picker", "File picking cancelled.")
            }

        }


    LaunchedEffect(modelImportProgress) {
        if (modelImportProgress == 1f) {
            delay(500)
            modelImportProgress = 0f
        }
    }

    Scaffold(
        topBar = {
            TopBar(title = "Ai Chat") {
                backStack.removeLastOrNull()
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                        // Single select.
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                    }
                    modelPickerLauncher.launch(intent)
                }
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painter = painterResource(R.drawable.add), contentDescription = "")
                    Text("Add Model")
                }
            }
        }
    ) { innerPadding ->

        if (modelImportProgress > 0f) {
            Dialog(
                onDismissRequest = {
                    modelImportProgress = 0f
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Importing Model")
                    LinearProgressIndicator(
                        progress = { modelImportProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    )
                }
            }
        }
        deletableModel?.let { model ->
            AlertDialog(
                onDismissRequest = { deletableModel = null },
                title = {
                    Text("Delete ${model.name}")
                },
                text = {
                    Text("Are you sure you want to delete ${model.name}?")
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteModel(model)
                            deletableModel = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            deletableModel = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        showConfigurationDialog.first?.let { model ->
            LlmModelConfigurationDialog(
                model = model,
                confirmButtonText = if (showConfigurationDialog.second == null) "Update" else "Import",
                onDismiss = { showConfigurationDialog = null to null }
            ) { updatedModel ->
                Log.d("Importing File", "File picked : $updatedModel")
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
                            Log.d("Importing File", "File picked : $update")
                            viewModel.addModel(update)
                            showConfigurationDialog = null to null
                        },
                        onProgress = { value ->
                            modelImportProgress = value
                        },
                        onError = { err ->
                            modelImportProgress = 0f
                            showConfigurationDialog = null to null
                            error = "Error Configuration" to err
                            Log.d("Importing File", "File Error : $err")
                        }
                    )
                } else {
                    viewModel.updateModel(model = updatedModel)
                    showConfigurationDialog = null to null
                }

            }
        }

        deletableChat?.let { chat->
            AlertDialog(
                onDismissRequest = { deletableChat = null },
                title = {
                    Text("Delete ${chat.title}")
                },
                text = {
                    Text("Are you sure you want to delete ${chat.title}?")
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.deleteChat(chat)
                            deletableChat = null
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            deletableChat = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        updatableChat?.let { chat->
            var name by remember { mutableStateOf(chat.title) }
            AlertDialog(
                onDismissRequest = { updatableChat = null },
                title = {
                    Text("Change name")
                },
                text = {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Chat name") }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isNotBlank() && name.isNotEmpty()) {
                                val chat = chat.copy(title = name)
                                viewModel.updateChat(chat)
                            }
                            updatableChat = null
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            updatableChat = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (error.second.isNotBlank() && error.second.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { error = null to "" },
                title = {
                    Text(error.first ?: "Error")
                },
                text = {
                    Text(error.second)
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            error = null to ""
                        }
                    ) {
                        Text("OK")
                    }
                },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                tabItems = tabs,
                selectedTabIndex = pagerState.currentPage
            ) { selectedTab ->
                scope.launch {
                    if (pagerState.currentPage != selectedTab)
                        pagerState.animateScrollToPage(page = selectedTab)
                }
            }

            HorizontalPager(
                state = pagerState
            ) {
                when (it) {
                    0 -> ChatList(
                        chats = chatSessions,
                        onDelete = { chat->
                            deletableChat = chat
                        },
                        onConfigClick = { chat->
                            updatableChat = chat
                        },
                        onChatItemClick = { chat ->
                            backStack.add(NavigationEntry.AiChat(id = chat.id, type = IdType.CHAT))
                        }
                    )
                    1 -> ModelList(
                        models = llmModels,
                        onDelete = { model ->
                            deletableModel = model
                        },
                        onConfigClick = { model ->
                            showConfigurationDialog = model to null
                        },
                        onModelItemClick = { model ->
                            backStack.add(NavigationEntry.AiChat(id = model.id.toString(), type = IdType.MODEL))
                        }
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
    onDelete: (LlmModel) -> Unit,
    onConfigClick: (LlmModel) -> Unit,
    onModelItemClick: (LlmModel) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    if (models.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                10.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No LLM model added")
            OutlinedButton(
                onClick = {
                    uriHandler.openUri("https://huggingface.co/litert-community/models")
                }
            ) {
                Text("Download a model")
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
            items(models, key = {
                it.id
            }) { llmModel ->
                LlmModelItemUi(
                    model = llmModel,
                    onDelete = onDelete,
                    onConfigClick = onConfigClick,
                    onClick = onModelItemClick
                )
            }
        }
    }
}

@Composable
fun ChatList(
    modifier: Modifier = Modifier,
    chats: List<ChatSession>,
    onDelete: (ChatSession) -> Unit,
    onConfigClick: (ChatSession) -> Unit,
    onChatItemClick: (ChatSession) -> Unit
) {

    if (chats.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No Chat found, select a model to start a chat")
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
            }
            items(chats, key = {
                it.id
            }) { chat ->
                ChatItem(
                    chat = chat,
                    onDelete = onDelete,
                    onConfigClick = onConfigClick,
                    onChatItemClick = onChatItemClick
                )
            }
        }
    }
}


@Composable
fun ChatItem(
    modifier: Modifier = Modifier,
    chat: ChatSession,
    onDelete: (ChatSession) -> Unit,
    onConfigClick: (ChatSession) -> Unit,
    onChatItemClick:(ChatSession)-> Unit
) {
    Card(
        modifier = modifier,
        onClick = { onChatItemClick(chat) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(chat.title, style = MaterialTheme.typography.bodyLarge)

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.access_time),
                    contentDescription = "Download Size"
                )
                Text(
                    chat.createdAt.toDateTimeString(
                        pattern = "dd MMM YYYY"
                    ), style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDelete(chat)
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.delete), "delete model")
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onConfigClick(chat)
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.tune), "tune model")
                        Text("Configure", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

        }
    }
}


@Composable
fun TabRow(
    modifier: Modifier = Modifier,
    tabItems: List<String>,
    selectedTabIndex: Int, onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.primaryContainer),
    ) {
        tabItems.forEachIndexed { index, string ->
            RowTabItem(
                modifier = Modifier.weight(1f),
                isSelected = index == selectedTabIndex,
                title = string,
                onClick = {
                    onTabSelected(index)
                }
            )
        }

    }
}

@Composable
fun RowTabItem(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    title: String,
    onClick: () -> Unit
) {
    val color = animateColorAsState(
        if (isSelected)
            MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.primaryContainer
    )
    val textColor = animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onPrimaryContainer
    )

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .background(
                color = color.value,
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = textColor.value)
    }
}


@Composable
fun LlmModelItemUi(
    modifier: Modifier = Modifier,
    model: LlmModel,
    onDelete: (LlmModel) -> Unit,
    onConfigClick: (LlmModel) -> Unit,
    onClick: (LlmModel) -> Unit
) {
    val fileSIze = model.sizeInBytes / 1e+6

    Card(
        modifier = modifier,
        onClick = {
            onClick(model)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = model.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(R.drawable.download), "Download Size")
                Text("${fileSIze.toInt()} MB", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onDelete(model)
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.delete), "delete model")
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onConfigClick(model)
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.tune), "tune model")
                        Text("Configure", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }


    }

}


@Preview
@Composable
fun TabRowUiPreview() {
    TooAiTheme{
        TabRow(
            tabItems = listOf("Chats", "Models"),
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
            onDelete = { _ -> },
            onConfigClick = {}
        ) {}
    }

}

@Preview
@Composable
fun ChatItemPreview() {
    TooAiTheme {
        ChatItem(
            chat = ChatSession(
                title = "Chat with my favourite model",
                modelId = 1,
                createdAt = System.currentTimeMillis()
            ),
            onDelete = {},
            onConfigClick = {},
            onChatItemClick = {}
        )
    }
}
