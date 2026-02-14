package com.rkbapps.tooai.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.rkbapps.tooai.ui.screens.barcode.BarcodeScanScreen
import com.rkbapps.tooai.ui.screens.chat.ChatScreen
import com.rkbapps.tooai.ui.screens.doc_scanner.DocScannerScreen
import com.rkbapps.tooai.ui.screens.home.HomeScreen
import com.rkbapps.tooai.ui.screens.image_segmentation.ImageSegmentationScreen
import com.rkbapps.tooai.ui.screens.model_and_chat_manager.ChatAndModelManagerScreen
import com.rkbapps.tooai.ui.screens.text_recognitation.TextReorganizationScreen

@Composable
fun NavManager(
    backStack: SnapshotStateList<Any>,
) {
    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
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
        predictivePopTransitionSpec = {
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

            entry<NavigationEntry.ChatAndModelManagement> {
                ChatAndModelManagerScreen(backStack)
            }

            entry<NavigationEntry.AiChat>{
                val modelId = it.modelId
                ChatScreen(backStack = backStack, modelId = modelId)
            }


        }
    )
}