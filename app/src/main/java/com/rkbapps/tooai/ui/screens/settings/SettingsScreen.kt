package com.rkbapps.tooai.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.rkbapps.tooai.R
import com.rkbapps.tooai.ui.composabels.TopBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    backStack: SnapshotStateList<Any>,
    viewModel: SettingsViewModel = hiltViewModel()
) {

    val uriHandler = LocalUriHandler.current

    val isSystemTheme by viewModel.isSystemTheme.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()




    Scaffold(
        topBar = {
            TopBar(title = "Settings") {
                backStack.removeLastOrNull()
            }
        }
    ) { innerPadding ->


        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item(key = 1) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                    ) {

                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text("v${viewModel.appVersion}")

                        Row(modifier = Modifier.padding(vertical = 10.dp)) {
                            FilledIconButton(
                                onClick = {
                                    uriHandler.openUri("https://github.com/Rajkumarbhakta/TooAi")
                                }
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.github),
                                    contentDescription = ""
                                )
                            }
                            FilledIconButton(
                                onClick = {
                                    uriHandler.openUri("mailto:contact@rkbapps.in")
                                },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.onPrimary,
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.mail),
                                    contentDescription = ""
                                )
                            }

                        }

                        HorizontalDivider()

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {
                                uriHandler.openUri("https://github.com/Rajkumarbhakta/TooAi/issues")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.bug_report),
                                    ""
                                )
                                Text("Raise a issue")
                            }
                        }

                        Button(
                            onClick = {
                                uriHandler.openUri("https://coff.ee/rajkumarbhakta")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary,
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.coffee),
                                    ""
                                )
                                Text("Buy me a Coffee")
                            }
                        }

                    }
                }
            }

            item(key = 2) {
                TextWithSwitch(
                    text = "Follow System Theme",
                    subText = "Use light or dark theme based on your system settings.",
                    checked = isSystemTheme,
                    icon = ImageVector.vectorResource(R.drawable.brightness_auto)
                ) {
                    viewModel.updateIsSystemTheme(it)
                }
            }
            item(key = 3) {
                AnimatedVisibility(!isSystemTheme) {
                    TextWithSwitch(
                        text = "Dark Theme",
                        checked = isDarkTheme,
                        icon = if (isDarkTheme) ImageVector.vectorResource(R.drawable.dark_mode)
                        else ImageVector.vectorResource(R.drawable.light_mode)
                    ) {
                        viewModel.updateTheme(it)
                    }
                }
            }

            item(key = 4) {
                TextWithArrow(
                    text = "Privacy Policy",
                    subText = "Read the privacy policy",
                    icon = ImageVector.vectorResource(R.drawable.privacy_tip)
                ) {
                    uriHandler.openUri("https://sites.google.com/view/tooai/home")
                }
            }
            item(key = 5) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "This app may not work properly on older Android versions or low-end devices. " +
                                "It runs AI models locally, requiring decent processing power. " +
                                "AI results may be inaccurate. The developer is not liable for device issues, incorrect AI responses, or any damages.",

                        modifier = Modifier.padding(10.dp),
                        textAlign = TextAlign.Justify
                    )
                }
            }
            item (key = 7){
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(text = "Made with ❤\nBY RKEXUS", textAlign = TextAlign.Center)
                }
            }
        }

    }


}


@Composable
fun TextWithSwitch(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    subText: String? = null,
    checked: Boolean = false,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            "country",
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text,

                style = MaterialTheme.typography.titleLarge,
            )
            subText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}


@Composable
fun TextWithArrow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    subText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            "country",
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text,

                style = MaterialTheme.typography.titleLarge,
            )
            subText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        IconButton(
            onClick = onClick
        ) {
            Icon(painter = painterResource(R.drawable.arrow_forward), "")
        }
    }
}