package com.rkbapps.tooai.ui.composabels

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.rkbapps.tooai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    onNavigationIconClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        navigationIcon = {
            onNavigationIconClick?.let {
                IconButton(onClick = { onNavigationIconClick.invoke() }) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "navigation up",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    )
}