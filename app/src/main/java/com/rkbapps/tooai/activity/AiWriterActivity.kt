package com.rkbapps.tooai.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.ui.screens.ai_writer.AiWriterSheet
import com.rkbapps.tooai.ui.screens.ai_writer.AiWriterViewModel
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.utils.copyText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Entry point for the system-wide AI Writer.
 *
 * Registered for [android.content.Intent.ACTION_PROCESS_TEXT], which puts it in the text-selection toolbar of
 * every app on the device, and for [android.content.Intent.ACTION_SEND] as a share-sheet fallback for apps that
 * hide the selection menu.
 *
 * Runs under a translucent theme so the sheet floats over the calling app.
 *
 * Note: this must not declare `singleTask`/`singleInstance`/`noHistory` in the manifest —
 * PROCESS_TEXT depends on `startActivityForResult` semantics, and those flags silently discard
 * the replacement text.
 */
@AndroidEntryPoint
class AiWriterActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val viewModel: AiWriterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sourceText = when (intent?.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()

            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)

            else -> null
        }.orEmpty()

        // Writing the result back is only possible for PROCESS_TEXT on an editable field.
        val readOnly = intent?.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false) ?: true
        val canReplace = intent?.action == Intent.ACTION_PROCESS_TEXT && !readOnly


        setContent {
            val useSystemTheme by viewModel.isSystemTheme.collectAsStateWithLifecycle()
            val darkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) { viewModel.start(sourceText, canReplace) }

            TooAiTheme(
                darkTheme = if (useSystemTheme) isSystemInDarkTheme() else darkTheme
            ) {
                AiWriterSheet(
                    state = state,
                    onSelectModel = viewModel::selectModel,
                    onRunPrompt = viewModel::runPrompt,
                    onReplace = ::replaceAndFinish,
                    onCopy = { result -> copyText(result) },
                    onShare = ::shareResult,
                    onRegenerate = viewModel::regenerate,
                    onShowVariant = viewModel::showVariant,
                    onRetry = viewModel::retry,
                    onStop = viewModel::stop,
                    onPromptTextChange = viewModel::updatePromptText,
                    onToggleContext = viewModel::setUseSourceAsContext,
                    onGenerate = viewModel::runFreeform,
                    onImportModel = ::openModelManager,
                    onCurrentPageChange = viewModel::onCurrentPageChange,
                    onDismiss = ::finish
                )
            }
        }
    }

    /**
     * Every exit route lands here — the sheet's close button, swipe-to-dismiss, system back,
     * Replace, and the model-manager hand-off — so releasing the engine here means no path can
     * leave the model resident.
     */
    override fun finish() {
        viewModel.dismiss()
        super.finish()
    }

    /** Hands the text back to the calling app, which writes it over the user's selection. */
    private fun replaceAndFinish(result: String) {
        setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, result))
        finish()
    }

    private fun shareResult(result: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, result)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(send, null))
    }

    private fun openModelManager() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
    }
}