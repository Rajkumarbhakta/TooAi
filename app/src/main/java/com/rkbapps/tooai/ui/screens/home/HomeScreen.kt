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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
import com.rkbapps.tooai.models.menuItems
import com.rkbapps.tooai.models.qrScanner
import com.rkbapps.tooai.models.textRecognition
import com.rkbapps.tooai.navigation.NavigationEntry
import com.rkbapps.tooai.ui.composabels.TopBar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(backStack: SnapshotStateList<Any>) {

    val composition = rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.animation))

    Scaffold(topBar = {
        TopBar(
            stringResource(id = R.string.app_name),
            actions = {
                FilledIconButton(
                    onClick = {
                        backStack.add(NavigationEntry.Settings)
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.settings), contentDescription = "Settings")
                }
            }
        )
    }) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp)
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Box(
                modifier = Modifier.weight(1f).padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LottieAnimation(
                    composition = composition.value,
                    iterations = LottieConstants.IterateForever
                )
            }

            menuItems.forEach {  item ->
                MenuItems(manuItem = item) {
                    backStack.add(item.navigationEntry)
                }
            }
        }
    }
}


@Composable
fun MenuItems(manuItem: MenuItem, onItemClick: () -> Unit) {
    Card(
        onClick = onItemClick, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(id = manuItem.icon),
                contentDescription = manuItem.title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp)
            )
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = manuItem.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = manuItem.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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














