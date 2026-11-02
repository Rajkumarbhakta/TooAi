package com.rkbapps.tooai.models

import android.os.Build
import com.rkbapps.tooai.R
import com.rkbapps.tooai.navigation.NavigationEntry

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: Int,
    val navigationEntry: NavigationEntry
)

val docScanner = MenuItem(
    title = "Doc Scanner",
    subtitle = "Scan pages into clean PDFs",
    icon = R.drawable.document_scanner,
    navigationEntry = NavigationEntry.DocScanner
)

val qrScanner = MenuItem(
    title = "QR Scanner",
    subtitle = "Read any code, keep a history",
    icon = R.drawable.qr_code_scanner,
    navigationEntry = NavigationEntry.BarcodeScan
)

val imageSegmentation = MenuItem(
    title = "Background Remover",
    subtitle = "Cut out any subject in a tap",
    icon = R.drawable.image_segmentation,
    navigationEntry = NavigationEntry.ImageSegmentation
)

val textRecognition = MenuItem(
    title = "Text Recognition",
    subtitle = "Turn photos into editable text",
    icon = R.drawable.text_recognation,
    navigationEntry = NavigationEntry.TextRecognization
)
val aiChat = MenuItem(
    title = "Ai Chat",
    subtitle = "Chat with local LLM models.",
    icon = R.drawable.chatbot,
    navigationEntry = NavigationEntry.ChatAndModelManagement()
)

val menuItems =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            aiChat,
            docScanner,
            qrScanner,
            imageSegmentation,
            textRecognition,
        )
    }else{
        listOf(
            docScanner,
            qrScanner,
            imageSegmentation,
            textRecognition,
        )
    }


