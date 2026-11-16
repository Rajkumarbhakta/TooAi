package com.rkbapps.tooai.ui.composabels

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import com.rkbapps.tooai.R

@Composable
fun PillSearchField(
    modifier: Modifier = Modifier,
    query: String,
    placeholder: String,
    // For fields shown by a search button, so one tap is enough to start typing.
    focusOnShow: Boolean = false,
    onQueryChange: (String) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val focusRequester = remember { FocusRequester() }
    if (focusOnShow) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.search),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}
