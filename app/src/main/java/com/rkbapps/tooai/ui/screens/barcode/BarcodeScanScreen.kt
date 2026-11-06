package com.rkbapps.tooai.ui.screens.barcode

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.rkbapps.tooai.R
import com.rkbapps.tooai.db.entity.QrScan
import com.rkbapps.tooai.ui.composabels.LargeCollapsingTopBar
import com.rkbapps.tooai.ui.composabels.PillSearchField
import com.rkbapps.tooai.ui.theme.GroupedListDefaults
import com.rkbapps.tooai.ui.theme.SurfaceStatusBar
import com.rkbapps.tooai.ui.theme.TooAiTheme
import com.rkbapps.tooai.ui.theme.ToolShapes
import com.rkbapps.tooai.ui.theme.pageContainerColor
import com.rkbapps.tooai.ui.theme.rowContainerColor
import com.rkbapps.tooai.ui.theme.toolPalette
import com.rkbapps.tooai.utils.copyText
import com.rkbapps.tooai.utils.dateGroupLabel
import com.rkbapps.tooai.utils.toDateTimeString

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BarcodeScanScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: BarcodeViewModel = hiltViewModel()
) {
    SurfaceStatusBar()

    val context = LocalContext.current
    val scans by viewModel.qrScanList.collectAsStateWithLifecycle()

    var selectedScanId by rememberSaveable { mutableStateOf<Long?>(null) }
    var scanToDelete by remember { mutableStateOf<QrScan?>(null) }
    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<QrCategory?>(null) }

    val newestFirst = remember(scans) { scans.sortedByDescending { it.timeMillis } }
    val categories = remember(scans) { QrCategory.entries.filter { c -> scans.any { c.matches(it) } } }
    val filteredScans = remember(newestFirst, searchQuery, category) {
        newestFirst.filter { scan ->
            (category?.matches(scan) ?: true) && (searchQuery.isBlank() ||
                scan.title().contains(searchQuery, ignoreCase = true) ||
                scan.rawVale.orEmpty().contains(searchQuery, ignoreCase = true))
        }
    }

    val startScan = {
        val options = GmsBarcodeScannerOptions.Builder()
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { result ->
                val qrScan = QrScan(
                    id = 0,
                    displayValue = result.displayValue,
                    rawVale = result.rawValue,
                    format = result.format,
                    valueType = result.valueType,
                    timeMillis = System.currentTimeMillis()
                )
                viewModel.addNewQrScan(qrScan) { id -> selectedScanId = id }
            }
            .addOnFailureListener {
                Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
                Log.e("BARCODESCANNER", "Barcode error", it)
            }
        Unit
    }

    scanToDelete?.let { scan ->
        AlertDialog(
            onDismissRequest = { scanToDelete = null },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete scan?") },
            text = { Text("\"${scan.title()}\" will be removed from your history.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQrScan(scan)
                        if (selectedScanId == scan.id) selectedScanId = null
                        scanToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { scanToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Looked up by id so a deleted scan closes its page.
    scans.find { it.id == selectedScanId }?.let { scan ->
        QrScanDetail(
            scan = scan,
            onClose = { selectedScanId = null },
            onDelete = { scanToDelete = scan }
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val accent = MaterialTheme.toolPalette.qr

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = "QR Scanner",
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = { backStack.removeLastOrNull() },
                subtitle = {
                    Text(if (scans.size == 1) "1 saved scan" else "${scans.size} saved scans")
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearching = !isSearching
                            searchQuery = ""
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = "Search history"
                        )
                    }
                }
            )
        },
        containerColor = pageContainerColor(),
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = startScan,
                containerColor = accent.container,
                contentColor = accent.onContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.qr_code_scanner),
                    contentDescription = "Scan a QR code",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedVisibility(visible = isSearching) {
                PillSearchField(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    query = searchQuery,
                    placeholder = "Search scans",
                    focusOnShow = true,
                    onQueryChange = { searchQuery = it }
                )
            }

            // A chip per kind that's actually in the history, so no filter can come up empty.
            if (categories.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryChip(label = "All", selected = category == null) { category = null }
                    categories.forEach { c ->
                        CategoryChip(label = c.label, selected = category == c) { category = c }
                    }
                }
            }

            if (filteredScans.isEmpty()) {
                ScansEmptyState(isSearching = searchQuery.isNotBlank(), onScan = startScan)
            } else {
                val groups = remember(filteredScans) {
                    filteredScans.groupBy { dateGroupLabel(it.timeMillis) }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)
                ) {
                    groups.forEach { (group, groupScans) ->
                        item(key = group) {
                            Text(
                                text = group,
                                modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleSmallEmphasized,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        itemsIndexed(groupScans, key = { _, scan -> scan.id }) { index, scan ->
                            QrScanItem(
                                shape = GroupedListDefaults.shape(index, groupScans.size),
                                scan = scan,
                                isToday = group == "Today",
                                onClick = { selectedScanId = scan.id },
                                onCopy = { context.copyText(scan.title()) },
                                onShare = { shareText(context, scan.rawVale ?: scan.title()) },
                                onDelete = { scanToDelete = scan }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(136.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.toolPalette.qr
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accent.container,
            selectedLabelColor = accent.onContainer,
            selectedLeadingIconColor = accent.onContainer
        )
    )
}

// Each kind of scan keeps one of the fixed tool accents, so it reads the same in every list.
@Composable
private fun qrTypeColors(valueType: Int?): Pair<Color, Color> {
    val palette = MaterialTheme.toolPalette
    val colors = when (valueType) {
        Barcode.TYPE_WIFI -> palette.qr
        Barcode.TYPE_URL -> palette.text
        Barcode.TYPE_PHONE, Barcode.TYPE_SMS -> palette.docs
        Barcode.TYPE_CONTACT_INFO -> palette.background
        Barcode.TYPE_EMAIL -> palette.aiChat
        else -> null
    }
    return colors?.let { it.container to it.onContainer }
        ?: (MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurfaceVariant)
}

private fun QrScan.subtitle(isToday: Boolean): String {
    val time = timeMillis.toDateTimeString(pattern = if (isToday) "h:mm a" else "d MMM")
    return listOfNotNull(
        qrType(valueType).label,
        parseWifi(rawVale)?.security?.label,
        time
    ).joinToString(" · ")
}

@Composable
private fun QrScanItem(
    shape: Shape,
    scan: QrScan,
    isToday: Boolean,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val (container, onContainer) = qrTypeColors(scan.valueType)
    var isMenuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = shape,
        color = rowContainerColor()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(container, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(qrType(scan.valueType).icon),
                    contentDescription = null,
                    tint = onContainer
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = scan.shortTitle(),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = scan.subtitle(isToday),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { isMenuOpen = true }) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    MenuItem("Copy", R.drawable.content_copy) {
                        isMenuOpen = false
                        onCopy()
                    }
                    MenuItem("Share", R.drawable.share) {
                        isMenuOpen = false
                        onShare()
                    }
                    MenuItem("Delete", R.drawable.delete, color = MaterialTheme.colorScheme.error) {
                        isMenuOpen = false
                        onDelete()
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    @DrawableRes icon: Int,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = color) },
        leadingIcon = { Icon(painter = painterResource(icon), contentDescription = null, tint = color) },
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScansEmptyState(isSearching: Boolean, onScan: () -> Unit) {
    val accent = MaterialTheme.toolPalette.qr
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(accent.container, ToolShapes.Qr.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.qr_code_scanner),
                contentDescription = null,
                tint = accent.onContainer,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isSearching) "No matching scans" else "No saved scans",
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isSearching) "Try a different search."
            else "Scan a QR code or barcode. Wi-Fi details, links and contacts are saved here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (!isSearching) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onScan,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.action,
                    contentColor = accent.onAction
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.qr_code_scanner),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text("Scan a code")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QrScanDetail(
    scan: QrScan,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val type = qrType(scan.valueType)
    val wifi = remember(scan) { parseWifi(scan.rawVale) }
    val primaryAction = remember(scan) { scan.primaryAction() }
    val (container, onContainer) = qrTypeColors(scan.valueType)
    var isPasswordVisible by rememberSaveable(scan.id) { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    BackHandler(onBack = onClose)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeCollapsingTopBar(
                title = type.title,
                scrollBehavior = scrollBehavior,
                onNavigationIconClick = onClose,
                subtitle = { Text(formatName(scan.format)) },
                actions = {
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = painterResource(R.drawable.delete),
                            contentDescription = "Delete scan",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        containerColor = pageContainerColor()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(container, ToolShapes.Qr.toShape()),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(type.icon),
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
                // Long values step down a size so the actions stay near the top.
                val heading = scan.shortTitle()
                SelectionContainer {
                    Text(
                        text = heading,
                        style = if (heading.length <= 32) {
                            MaterialTheme.typography.headlineMediumEmphasized
                        } else {
                            MaterialTheme.typography.titleLargeEmphasized
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            QrDetailActions(
                primaryAction = primaryAction,
                // Copies the password for Wi-Fi. A longer label squeezes Connect down to its icon.
                onCopy = { context.copyText(wifi?.password ?: scan.title()) },
                onShare = { shareText(context, scan.rawVale ?: scan.title()) }
            )

            Text(
                text = "Details",
                modifier = Modifier.padding(start = 12.dp, top = 24.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QrDetailFields(
                scan = scan,
                wifi = wifi,
                isPasswordVisible = isPasswordVisible,
                onTogglePassword = { isPasswordVisible = !isPasswordVisible },
                onCopyRaw = { context.copyText(scan.rawVale.orEmpty()) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QrDetailActions(
    primaryAction: QrAction?,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    val accent = MaterialTheme.toolPalette.qr
    val height = ButtonDefaults.MediumContainerHeight

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Copy is the main action when the scan has nothing more specific to do.
        val main = primaryAction ?: QrAction("Copy", R.drawable.content_copy) { onCopy() }
        Button(
            onClick = { main.run(context) },
            modifier = Modifier
                .weight(1f)
                .height(height),
            shapes = ButtonDefaults.shapesFor(height),
            contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.action,
                contentColor = accent.onAction
            )
        ) {
            ActionButtonContent(icon = main.icon, label = main.label, height = height)
        }
        // Copy drops its label next to a long one like "Add contact", which wouldn't fit otherwise.
        if (primaryAction != null && primaryAction.label.length > 8) {
            FilledTonalIconButton(
                onClick = onCopy,
                modifier = Modifier.size(height),
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = accent.container,
                    contentColor = accent.onContainer
                )
            ) {
                Icon(painter = painterResource(R.drawable.content_copy), contentDescription = "Copy")
            }
        } else if (primaryAction != null) {
            FilledTonalButton(
                onClick = onCopy,
                modifier = Modifier.height(height),
                shapes = ButtonDefaults.shapesFor(height),
                contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = accent.container,
                    contentColor = accent.onContainer
                )
            ) {
                ActionButtonContent(icon = R.drawable.content_copy, label = "Copy", height = height)
            }
        }
        OutlinedIconButton(
            onClick = onShare,
            modifier = Modifier.size(height),
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(painter = painterResource(R.drawable.share), contentDescription = "Share")
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionButtonContent(@DrawableRes icon: Int, label: String, height: Dp) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
    )
    Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(height)))
    Text(text = label, style = MaterialTheme.typography.titleMediumEmphasized, maxLines = 1)
}

private data class DetailField(
    val label: String,
    val value: String,
    val isMonospace: Boolean = false,
    val trailing: (@Composable () -> Unit)? = null
)

@Composable
private fun QrDetailFields(
    scan: QrScan,
    wifi: WifiDetails?,
    isPasswordVisible: Boolean,
    onTogglePassword: () -> Unit,
    onCopyRaw: () -> Unit
) {
    val raw = scan.rawVale.orEmpty()
    // Rows of one or two fields. The grouped corners follow the rows, not the fields.
    val rows = buildList {
        add(listOf(DetailField("Display value", scan.displayValue.orEmpty())))
        add(
            listOf(
                DetailField("Value type", getValueTypeLabel(scan.valueType ?: -1)),
                DetailField("Format", getFormatLabel(scan.format ?: -1))
            )
        )
        if (wifi != null) {
            val security = DetailField("Security", wifi.security.label)
            add(
                if (wifi.password == null) {
                    listOf(security)
                } else {
                    listOf(
                        security,
                        DetailField(
                            label = "Password",
                            value = if (isPasswordVisible) wifi.password else "••••••••",
                            trailing = {
                                IconButton(onClick = onTogglePassword, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        painter = painterResource(
                                            if (isPasswordVisible) R.drawable.visibility_off else R.drawable.visibility
                                        ),
                                        contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                    )
                }
            )
        }
        if (raw.isNotEmpty()) {
            add(
                listOf(
                    DetailField(
                        label = "Raw value",
                        value = if (wifi != null && !isPasswordVisible) maskWifiPassword(raw) else raw,
                        isMonospace = true,
                        trailing = {
                            FilledTonalIconButton(
                                onClick = onCopyRaw,
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.content_copy),
                                    contentDescription = "Copy raw value",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                )
            )
        }
        add(listOf(DetailField("Scanned", scan.timeMillis.toDateTimeString(pattern = "d MMM yyyy, h:mm a"))))
    }

    Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)) {
        rows.forEachIndexed { index, fields ->
            val shape = GroupedListDefaults.shape(index, rows.size)
            Row(horizontalArrangement = Arrangement.spacedBy(GroupedListDefaults.Gap)) {
                fields.forEach { field ->
                    DetailFieldCell(field = field, shape = shape, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DetailFieldCell(field: DetailField, shape: Shape, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(shape)
            .background(rowContainerColor())
            .padding(start = 16.dp, end = if (field.trailing != null) 8.dp else 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = field.label,
                style = MaterialTheme.typography.labelMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SelectionContainer {
                Text(
                    text = field.value.ifEmpty { "—" },
                    style = if (field.isMonospace) {
                        MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
            }
        }
        field.trailing?.invoke()
    }
}

@Preview
@Composable
fun QrScanItemPreview() {
    TooAiTheme {
        QrScanItem(
            shape = GroupedListDefaults.shape(0, 1),
            scan = QrScan(
                id = 1,
                displayValue = "HomeNet_5G",
                rawVale = "WIFI:T:WPA;S:HomeNet_5G;P:secret123;;",
                format = Barcode.FORMAT_QR_CODE,
                valueType = Barcode.TYPE_WIFI,
                timeMillis = System.currentTimeMillis()
            ),
            isToday = true,
            onClick = {},
            onCopy = {},
            onShare = {},
            onDelete = {}
        )
    }
}
