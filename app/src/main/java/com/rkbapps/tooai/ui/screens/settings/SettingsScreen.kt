package com.rkbapps.tooai.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rkbapps.tooai.R
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.theme.AccentColor
import com.rkbapps.tooai.ui.theme.GroupedListDefaults
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.isDark
import com.rkbapps.tooai.ui.theme.toolPalette
import kotlinx.coroutines.launch

private const val MODELS_PAGE = 1

private const val DISCLAIMER =
    "This app may not work properly on older Android versions or low-end devices. " +
            "It runs AI models locally, requiring decent processing power. " +
            "AI results may be inaccurate. The developer is not liable for device issues, incorrect AI responses, or any damages."

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: SettingsViewModel = hiltViewModel()
) {

    SurfaceStatusBar()

    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isSystemTheme by viewModel.isSystemTheme.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
    val isDynamicColor by viewModel.isDynamicColor.collectAsStateWithLifecycle()
    val accentColor by viewModel.accentColor.collectAsStateWithLifecycle()

    var showClearHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showDisclaimerDialog by rememberSaveable { mutableStateOf(false) }

    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pageColor = if (MaterialTheme.colorScheme.isDark) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "Settings",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { backStack.removeLastOrNull() },
                containerColor = pageColor
            )
        },
        containerColor = pageColor,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            val appearanceRows = if (supportsDynamicColor) 3 else 2
            SectionTitle("Appearance")
            SettingsGroup {
                ThemeRow(
                    shape = GroupedListDefaults.shape(0, appearanceRows),
                    isSystemTheme = isSystemTheme,
                    isDarkTheme = isDarkTheme,
                    onSelectSystem = { viewModel.updateIsSystemTheme(true) },
                    onSelectFixed = { dark ->
                        viewModel.updateTheme(dark)
                        viewModel.updateIsSystemTheme(false)
                    }
                )
                if (supportsDynamicColor) {
                    SwitchRow(
                        shape = GroupedListDefaults.shape(1, appearanceRows),
                        icon = R.drawable.sparkles,
                        title = "Dynamic color",
                        subtitle = "Use colors from your wallpaper",
                        checked = isDynamicColor,
                        onCheckedChange = { viewModel.updateDynamicColor(it) }
                    )
                }
                AccentRow(
                    shape = GroupedListDefaults.shape(appearanceRows - 1, appearanceRows),
                    selected = accentColor,
                    enabled = !supportsDynamicColor || !isDynamicColor,
                    onSelect = { viewModel.updateAccentColor(it) }
                )
            }

            SectionTitle("AI Chat")
            SettingsGroup {
                SettingsRow(
                    shape = GroupedListDefaults.shape(0, 2),
                    icon = R.drawable.tune,
                    title = "Manage models",
                    subtitle = "Import, configure or remove models",
                    onClick = {
                        backStack.add(NavigationEntry.ChatAndModelManagement(MODELS_PAGE))
                    }
                )
                ClearHistoryRow(
                    shape = GroupedListDefaults.shape(1, 2),
                    onClick = { showClearHistoryDialog = true }
                )
            }

            CoffeeCard(onClick = { uriHandler.openUri("https://coff.ee/rajkumarbhakta") })

            SectionTitle("About")
            SettingsGroup {
                SettingsRow(
                    shape = GroupedListDefaults.shape(0, 6),
                    icon = R.drawable.privacy_tip,
                    title = "Privacy policy",
                    onClick = { uriHandler.openUri("https://sites.google.com/view/tooai/home") }
                )
                SettingsRow(
                    shape = GroupedListDefaults.shape(1, 6),
                    icon = R.drawable.info,
                    title = "Disclaimer",
                    subtitle = "AI output can be inaccurate",
                    onClick = { showDisclaimerDialog = true }
                )
                SettingsRow(
                    shape = GroupedListDefaults.shape(2, 6),
                    icon = R.drawable.bug_report,
                    title = "Report an issue",
                    onClick = { uriHandler.openUri("https://github.com/Rajkumarbhakta/TooAi/issues") }
                )
                SettingsRow(
                    shape = GroupedListDefaults.shape(3, 6),
                    icon = R.drawable.github,
                    title = "Source code",
                    onClick = { uriHandler.openUri("https://github.com/Rajkumarbhakta/TooAi") }
                )
                SettingsRow(
                    shape = GroupedListDefaults.shape(4, 6),
                    icon = R.drawable.mail,
                    title = "Contact",
                    onClick = { uriHandler.openUri("mailto:contact@rkbapps.in") }
                )
                VersionRow(
                    shape = GroupedListDefaults.shape(5, 6),
                    version = viewModel.appVersion
                )
            }

            Text(
                text = "Made with ❤\nBY RKEXUS",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear all chat history?") },
            text = { Text("Every chat and its messages will be permanently deleted. Your models are kept.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        scope.launch {
                            viewModel.clearChatHistory().join()
                            snackbarHostState.showSnackbar("Chat history cleared")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDisclaimerDialog) {
        AlertDialog(
            onDismissRequest = { showDisclaimerDialog = false },
            title = { Text("Disclaimer") },
            text = { Text(DISCLAIMER) },
            confirmButton = {
                TextButton(onClick = { showDisclaimerDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmallEmphasized,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)
    ) {
        content()
    }
}

@Composable
private fun settingsRowColor() = if (MaterialTheme.colorScheme.isDark) {
    MaterialTheme.colorScheme.surfaceContainer
} else {
    MaterialTheme.colorScheme.surfaceContainerLowest
}

@Composable
private fun RowContainer(
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(settingsRowColor())
            .then(modifier)
    ) {
        content()
    }
}

@Composable
private fun IconBadge(
    icon: Int,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun RowText(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun SettingsRow(
    shape: Shape,
    icon: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    RowContainer(shape = shape, modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon)
            RowText(title, subtitle, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchRow(
    shape: Shape,
    icon: Int,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    RowContainer(
        shape = shape,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon)
            RowText(title, subtitle, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeRow(
    shape: Shape,
    isSystemTheme: Boolean,
    isDarkTheme: Boolean,
    onSelectSystem: () -> Unit,
    onSelectFixed: (dark: Boolean) -> Unit
) {
    RowContainer(shape = shape) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconBadge(R.drawable.brightness_auto)
                RowText(
                    title = "Theme",
                    subtitle = when {
                        isSystemTheme -> "Follows your system setting"
                        isDarkTheme -> "Always dark"
                        else -> "Always light"
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                ThemeOption(
                    label = "System",
                    icon = R.drawable.brightness_auto,
                    checked = isSystemTheme,
                    onClick = onSelectSystem,
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    label = "Light",
                    icon = R.drawable.light_mode,
                    checked = !isSystemTheme && !isDarkTheme,
                    onClick = { onSelectFixed(false) },
                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    label = "Dark",
                    icon = R.drawable.dark_mode,
                    checked = !isSystemTheme && isDarkTheme,
                    onClick = { onSelectFixed(true) },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeOption(
    label: String,
    icon: Int,
    checked: Boolean,
    onClick: () -> Unit,
    shapes: ToggleButtonShapes,
    modifier: Modifier = Modifier
) {
    ToggleButton(
        checked = checked,
        onCheckedChange = { onClick() },
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics { role = Role.RadioButton },
        icon = {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shapes = shapes,
        colors = ToggleButtonDefaults.colors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = label,
            style = if (checked) {
                MaterialTheme.typography.labelLargeEmphasized
            } else {
                MaterialTheme.typography.labelLarge
            },
            maxLines = 1
        )
    }
}

@Composable
private fun AccentRow(
    shape: Shape,
    selected: AccentColor,
    enabled: Boolean,
    onSelect: (AccentColor) -> Unit
) {
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    RowContainer(shape = shape) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowText(
                title = "Accent color",
                subtitle = "Used when dynamic color is off",
                modifier = Modifier.weight(1f),
                titleColor = if (enabled) MaterialTheme.colorScheme.onSurface else disabledColor,
                subtitleColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else disabledColor
            )
            AccentColor.entries.forEach { accent ->
                AccentSwatch(
                    accent = accent,
                    selected = accent == selected,
                    enabled = enabled,
                    onClick = { onSelect(accent) }
                )
            }
        }
    }
}

@Composable
private fun AccentSwatch(
    accent: AccentColor,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val name = accent.name.lowercase().replaceFirstChar { it.uppercase() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = name }
            .alpha(if (enabled) 1f else 0.55f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (selected) {
                        Modifier.border(
                            BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                            CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(if (selected) 4.dp else 0.dp)
                .background(accent.seed, CircleShape)
        )
    }
}

@Composable
private fun ClearHistoryRow(shape: Shape, onClick: () -> Unit) {
    RowContainer(shape = shape, modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(
                icon = R.drawable.delete,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Clear all chat history",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CoffeeCard(onClick: () -> Unit) {
    val accent = MaterialTheme.toolPalette.docs
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = accent.container,
        contentColor = accent.onContainer
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(accent.action, ToolShapes.Text.toShape()),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.coffee),
                    contentDescription = null,
                    tint = accent.onAction,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buy me a coffee",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
                Text(
                    text = "Keeps the app free, private and ad-free",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onClick,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.action,
                    contentColor = accent.onAction
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = "Support",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun VersionRow(shape: Shape, version: String) {
    RowContainer(shape = shape) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(R.drawable.info)
            RowText("Version", null, modifier = Modifier.weight(1f))
            Text(
                text = version,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
