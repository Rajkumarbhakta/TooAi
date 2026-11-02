package com.rkbapps.tooai.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import com.rkbapps.tooai.R
import com.rkbapps.tooai.models.MenuItem
import com.rkbapps.tooai.models.aiChat
import com.rkbapps.tooai.models.docScanner
import com.rkbapps.tooai.models.menuItems
import com.rkbapps.tooai.models.qrScanner
import com.rkbapps.tooai.models.textRecognition
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.ToolColors
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.isDark
import com.rkbapps.tooai.ui.theme.toolPalette

private const val CHATS_PAGE = 0
private const val MODELS_PAGE = 1

@Composable
fun HomeScreen(backStack: SnapshotStateList<Any>) {

    SurfaceStatusBar()

    Scaffold { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            HomeHeader(
                onSettingsClick = { backStack.add(NavigationEntry.Settings) }
            )

            if (aiChat in menuItems) {
                AiChatHero(
                    onNewChat = {
                        backStack.add(NavigationEntry.ChatAndModelManagement(MODELS_PAGE))
                    },
                    onHistory = {
                        backStack.add(NavigationEntry.ChatAndModelManagement(CHATS_PAGE))
                    }
                )
            }

            VisionToolsHeader()

            VisionToolsGrid(
                tools = menuItems - aiChat,
                onToolClick = { backStack.add(it.navigationEntry) }
            )
        }
    }
}

@Composable
private fun HomeHeader(onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OfflineBadge()
            FilledIconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.settings),
                    contentDescription = "Settings"
                )
            }
        }
        Text(
            text = stringResource(id = R.string.app_name),
            style = MaterialTheme.typography.displaySmallEmphasized
        )
    }
}

@Composable
private fun OfflineBadge() {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .padding(start = 10.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.privacy_tip),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "On-device · Works offline",
                style = MaterialTheme.typography.labelLargeEmphasized
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AiChatHero(onNewChat: () -> Unit, onHistory: () -> Unit) {
    val accent = MaterialTheme.toolPalette.aiChat
    val isDark = MaterialTheme.colorScheme.isDark
    val heroColor = if (isDark) accent.container else accent.action
    val primaryButtonColors = ButtonDefaults.buttonColors(
        containerColor = if (isDark) accent.action else Color.White,
        contentColor = if (isDark) accent.onAction else accent.onContainer
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLargeIncreased,
        color = heroColor,
        contentColor = Color.White
    ) {
        Box(modifier = Modifier.clipToBounds()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 18.dp, y = (-14).dp)
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.12f), ToolShapes.AiChat.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chatbot),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(end = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LOCAL LLM",
                        style = MaterialTheme.typography.labelMediumEmphasized,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "AI Chat",
                        style = MaterialTheme.typography.headlineLargeEmphasized
                    )
                    Text(
                        text = "Talk to models that run entirely on your phone. Nothing leaves the device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onNewChat,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 56.dp),
                        shapes = ButtonDefaults.shapes(),
                        colors = primaryButtonColors
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.add),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = "New chat",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1
                        )
                    }
                    OutlinedButton(
                        onClick = onHistory,
                        modifier = Modifier.heightIn(min = 56.dp),
                        shapes = ButtonDefaults.shapes(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.access_time),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisionToolsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Vision tools",
            style = MaterialTheme.typography.titleLargeEmphasized
        )
        Text(
            text = "Powered by ML Kit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VisionToolsGrid(tools: List<MenuItem>, onToolClick: (MenuItem) -> Unit) {
    BoxWithConstraints(modifier = Modifier.padding(horizontal = 16.dp)) {
        val columns = if (maxWidth >= 600.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tools.chunked(columns).forEach { rowTools ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowTools.forEach { tool ->
                        ToolTile(
                            tool = tool,
                            onClick = { onToolClick(tool) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToolTile(tool: MenuItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val (accent, shape) = toolIdentity(tool)

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 168.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .size(60.dp)
                    .background(accent.container, shape.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = tool.icon),
                    contentDescription = null,
                    tint = accent.onContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = tool.title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun toolIdentity(tool: MenuItem): Pair<ToolColors, RoundedPolygon> {
    val palette = MaterialTheme.toolPalette
    return when (tool) {
        docScanner -> palette.docs to ToolShapes.Docs
        qrScanner -> palette.qr to ToolShapes.Qr
        textRecognition -> palette.text to ToolShapes.Text
        else -> palette.background to ToolShapes.Background
    }
}
