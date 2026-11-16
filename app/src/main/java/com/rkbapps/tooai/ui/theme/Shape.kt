package com.rkbapps.tooai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.unit.dp

// One shape per tool. Apply with toShape().
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object ToolShapes {
    val AiChat = MaterialShapes.Sunny
    val Docs = MaterialShapes.Square
    val Qr = MaterialShapes.Clover4Leaf
    val Background = MaterialShapes.Cookie6Sided
    val Text = MaterialShapes.Cookie9Sided
    val Loading = MaterialShapes.Burst
}

// Grouped lists: 24 outer / 6 inner corners with 3dp gaps.
object GroupedListDefaults {
    val Gap = 3.dp

    private val Outer = 24.dp
    private val Inner = 6.dp

    fun shape(index: Int, count: Int) = RoundedCornerShape(
        topStart = if (index == 0) Outer else Inner,
        topEnd = if (index == 0) Outer else Inner,
        bottomStart = if (index == count - 1) Outer else Inner,
        bottomEnd = if (index == count - 1) Outer else Inner,
    )
}
