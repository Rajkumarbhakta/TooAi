package com.rkbapps.tooai.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class NavigationEntry: NavKey {

    @Serializable
    data object BarcodeScan: NavigationEntry()

    @Serializable
    data object Home: NavigationEntry()

    @Serializable
    data object DocScanner: NavigationEntry()

    @Serializable
    data object ImageSegmentation: NavigationEntry()

    @Serializable
    data object TextRecognization: NavigationEntry()


}