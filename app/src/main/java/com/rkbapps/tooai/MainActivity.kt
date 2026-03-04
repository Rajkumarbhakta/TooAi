package com.rkbapps.tooai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.rkbapps.tooai.db.PreferenceManager
import com.rkbapps.tooai.navigation.NavManager
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.theme.TooAiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object{
        val  backStack = mutableStateListOf<Any>(NavigationEntry.Home)
    }

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isSystemTheme = preferenceManager.getBooleanPreference(PreferenceManager.Companion.IS_USE_SYSTEM_THEME, true)
            .stateIn(
                lifecycleScope,
                SharingStarted.Companion.Lazily,
                true
            )

        val isDarkTheme = preferenceManager.getBooleanPreference(PreferenceManager.Companion.IS_DARK_THEME, false)
            .stateIn(
                lifecycleScope,
                SharingStarted.Companion.Lazily,
                false
            )

        setContent {

            val isSystemTheme by isSystemTheme.collectAsStateWithLifecycle()
            val darkTheme by isDarkTheme.collectAsStateWithLifecycle()

            TooAiTheme(
                darkTheme = if (isSystemTheme) isSystemInDarkTheme() else darkTheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavManager(backStack)
                }
            }
        }
        @OptIn(ExperimentalApi::class)
        ExperimentalFlags.enableBenchmark = true
    }
}