package com.rkbapps.tooai.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rkbapps.tooai.R
import com.rkbapps.tooai.models.MenuItem
import com.rkbapps.tooai.models.docScanner
import com.rkbapps.tooai.models.imageSegmentation
import com.rkbapps.tooai.models.qrScanner
import com.rkbapps.tooai.models.textRecognition
import com.rkbapps.tooai.navigation.NavigationEntry


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(backStack: SnapshotStateList<Any>) {

    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation))

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.app_name)) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition.value,
                    iterations = LottieConstants.IterateForever
                )
            }

            MenuRows(item1 = {
                MenuItems(manuItem = docScanner) {
                    backStack.add(NavigationEntry.DocScanner)
                }
            }, item2 = {
                MenuItems(manuItem = qrScanner) {
                    backStack.add(NavigationEntry.BarcodeScan)
                }
            })
            MenuRows(item1 = {
                MenuItems(manuItem = imageSegmentation) {
                    backStack.add(NavigationEntry.ImageSegmentation)
                }
            }, item2 = {
                MenuItems(manuItem = textRecognition) {
                    backStack.add(NavigationEntry.TextRecognization)
                }
            })
            MenuItems(manuItem = textRecognition) {
                backStack.add(NavigationEntry.ChatAndModelManagement)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {

                Text(text = "Made with ❤\nBY RKB", textAlign = TextAlign.Center)

            }

        }
    }
}


@Composable
fun MenuItems(manuItem: MenuItem, onItemClick: () -> Unit) {
    OutlinedCard(
        onClick = {
            onItemClick()
        }, modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = manuItem.icon),
                contentDescription = manuItem.title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .height(30.dp)
                    .width(30.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = manuItem.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = manuItem.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MenuRows(item1: @Composable () -> Unit, item2: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item1()
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item2()
        }

    }

}














