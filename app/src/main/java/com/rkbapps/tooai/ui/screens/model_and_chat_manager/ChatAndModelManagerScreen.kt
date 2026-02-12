package com.rkbapps.tooai.ui.screens.model_and_chat_manager

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.LlmModel
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.composabels.TopBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.utils.ModelConfigs
import com.rkbapps.tooai.utils.getFileName
import com.rkbapps.tooai.utils.getFileNameAndSize
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAndModelManagerScreen(backStack: SnapshotStateList<Any>) {

    val context = LocalContext.current

    val viewModel: ChatAndModelManagerViewModel = hiltViewModel()
    val status by viewModel.status.collectAsState()
    val llmModels by viewModel.llmModels.collectAsStateWithLifecycle()

    var showUnSupportedFileDialog by remember { mutableStateOf(false) }
    var showConfigurationDialog by remember { mutableStateOf<Pair<LlmModel?, Uri?>>(null to null) }
    var modelImportProgress by remember { mutableFloatStateOf(0f) }


    val modelPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK){
            result.data?.data?.let { uri ->
                val fileName = context.getFileName(uri = uri)
                val nameAndSize = context.getFileNameAndSize(uri = uri)
                if (fileName != null && !fileName.endsWith(".task") && !fileName.endsWith(".litertlm") ){
                    // unsupported file
                    showUnSupportedFileDialog = true
                }else if(fileName==null || nameAndSize.second == 0L || fileName.lowercase().contains("-web")){
                    // unsupported file
                    showUnSupportedFileDialog = true
                }else{
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
        }else{
            //canceled
            Log.d("File Picker", "File picking cancelled.")
        }

    }


    LaunchedEffect(modelImportProgress) {
        if (modelImportProgress==1f){
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
    ) { innerPadding->


        if (showUnSupportedFileDialog){
            AlertDialog(
                onDismissRequest = { showUnSupportedFileDialog=false },
                text = {
                    Text("Select a LiteRT-LM model")
                },
                title = {
                    Text("Unsupported file type")
                },
                confirmButton = {
                    Button(onClick = { showUnSupportedFileDialog=false }) {
                        Text("Ok")
                    }
                }
            )
        }
        if (modelImportProgress>0f){
            Dialog(
                onDismissRequest = {
                    modelImportProgress = 0f
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color = MaterialTheme.colorScheme.surface)
                        .padding(10.dp)
                    ,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text("Importing Model")
                    LinearProgressIndicator(
                        progress = { modelImportProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        showConfigurationDialog.first?.let { model ->
            LlmModelConfigurationDialog(
                model = model,
                onDismiss = { showConfigurationDialog = null to null }
            ) { updatedModel ->
                Log.d("Importing File", "File picked : $updatedModel")
                if (showConfigurationDialog.second!=null){
                    viewModel.importModel(
                        context = context,
                        fileSize = updatedModel.sizeInBytes,
                        fileName = updatedModel.displayName,
                        uri = showConfigurationDialog.second!!,
                        onDone = { path->
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
                        onError = { error->
                            modelImportProgress = 0f
                            showConfigurationDialog = null to null
                            Log.d("Importing File", "File Error : $error")
                        }
                    )
                }else{
                    viewModel.updateModel(model = model)
                }

            }
        }

        if (llmModels.isEmpty()){
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp, alignment = Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No LLM model added")
            }
        }else{
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = innerPadding,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
                items(llmModels, key = {
                    it.id
                }){llmModel->
                    LlmModelItemUi(model = llmModel){
                        backStack.add(NavigationEntry.AiChat(llmModel.id))
                    }
                }
            }
        }

    }


}


@Composable
fun LlmModelItemUi(
    modifier: Modifier = Modifier,
    model: LlmModel,
    onClick:(LlmModel)-> Unit
) {
    val fileSIze = model.sizeInBytes/1e+6

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
                Icon(painter = painterResource(R.drawable.download),"Download Size")
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
                    onClick = {}
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.delete),"delete model")
                        Text("Delete", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {

                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(R.drawable.tune),"tune model")
                        Text("Configure", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }


    }

}


@Preview
@Composable
fun LlmModelItemUiPreview(modifier: Modifier = Modifier) {
    TooAiTheme() {
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
            )
        ){}
    }
    
}


