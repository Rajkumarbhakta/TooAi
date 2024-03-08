package com.rkbapps.tools.models

import com.rkbapps.tools.R

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: Int
)

val docScanner = MenuItem(
    title = "Doc Scanner",
    subtitle = "Scan document with ease.",
    icon = R.drawable.document_scanner
)

val qrScanner = MenuItem(
    title = "QR Scanner",
    subtitle = "Scan barcode and QR codes.",
    icon = R.drawable.qr_code_scanner
)

val imageSegmentation = MenuItem(
    title = "Image Segmentation",
    subtitle = "Remove image background with one click.",
    icon = R.drawable.image_segmentation
)

val textRecognition = MenuItem(
    title = "Text Recognition",
    subtitle = "Extract text from image.",
    icon = R.drawable.text_recognation
)
