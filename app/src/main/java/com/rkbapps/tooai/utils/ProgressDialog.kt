package com.rkbapps.tooai.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ProgressDialog(
    isVisible: MutableState<Boolean>,
    title: String = "Processing...",
    progress: Float? = null,
    dismissible: Boolean = true
) {
    Dialog(onDismissRequest = {
        if (!dismissible) {
            isVisible.value = false
        }
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(10.dp)
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title)
        }
    }
}