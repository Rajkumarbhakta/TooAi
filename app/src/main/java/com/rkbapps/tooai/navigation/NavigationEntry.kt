package com.rkbapps.tooai.navigation

import android.os.Parcelable
import androidx.navigation3.runtime.NavKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationEntry: NavKey, Parcelable {

    @Parcelize
    @Serializable
    data object BarcodeScan: NavigationEntry()

    @Parcelize
    @Serializable
    data object Home: NavigationEntry()

    @Parcelize
    @Serializable
    data object DocScanner: NavigationEntry()

    @Parcelize
    @Serializable
    data object ImageSegmentation: NavigationEntry()

    @Parcelize
    @Serializable
    data object TextRecognization: NavigationEntry()

    @Parcelize
    @Serializable
    data object ChatAndModelManagement: NavigationEntry()

    @Parcelize
    @Serializable
    data class AiChat(
        val id: String = "",
        val type: IdType = IdType.CHAT
    ): NavigationEntry()

}


enum class IdType{
    MODEL,
    CHAT
}