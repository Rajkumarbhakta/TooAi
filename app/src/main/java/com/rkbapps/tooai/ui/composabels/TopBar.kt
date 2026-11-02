package com.rkbapps.tooai.ui.composabels

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.rkbapps.tooai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: String,
    actions: @Composable (RowScope.() -> Unit) = {},
    onNavigationIconClick: (() -> Unit)? = null,
) {
    TopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        actions = actions,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title:  @Composable (() -> Unit),
    onNavigationIconClick: (() -> Unit)? = null
) {
    TopAppBar(
        title = title,
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

// Standard bar for migrated screens. Pass the scroll behavior's nestedScrollConnection to the
// Scaffold so it collapses with the content.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LargeCollapsingTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onNavigationIconClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    LargeFlexibleTopAppBar(
        title = { Text(text = title, fontWeight = FontWeight.ExtraBold) },
        navigationIcon = {
            IconButton(onClick = onNavigationIconClick) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "navigation up"
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        scrollBehavior = scrollBehavior
    )
}
