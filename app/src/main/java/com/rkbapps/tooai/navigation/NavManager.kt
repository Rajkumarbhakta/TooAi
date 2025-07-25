package com.rkbapps.tooai.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import com.rkbapps.tooai.ui.screens.BarcodeScanScreen
import com.rkbapps.tooai.ui.screens.DocScannerScreen
import com.rkbapps.tooai.ui.screens.HomeScreen
import com.rkbapps.tooai.ui.screens.ImageSegmentationScreen
import com.rkbapps.tooai.ui.screens.TextReorganizationScreen

@Composable
fun NavManager(
    backStack: SnapshotStateList<Any>,
) {
    NavDisplay(
        entryDecorators = listOf(
            rememberSceneSetupNavEntryDecorator(),
            rememberSavedStateNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        entryProvider = entryProvider {

            entry<NavigationEntry.Home> {
                HomeScreen(backStack)
            }
            entry<NavigationEntry.BarcodeScan> {
                BarcodeScanScreen(backStack)
            }

            entry<NavigationEntry.DocScanner> {
                DocScannerScreen(backStack)
            }

            entry<NavigationEntry.ImageSegmentation> {
                ImageSegmentationScreen(backStack)
            }

            entry<NavigationEntry.TextRecognization> {
                TextReorganizationScreen(backStack)
            }


        }
    )


}