package com.rkbapps.tooai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.rkbapps.tooai.navigation.NavManager
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.theme.TooAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TooAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val backStack = remember { mutableStateListOf<Any>(NavigationEntry.Home) }
                    NavManager(backStack)
                }
            }
        }
        @OptIn(ExperimentalApi::class)
        ExperimentalFlags.enableBenchmark = true
    }
}