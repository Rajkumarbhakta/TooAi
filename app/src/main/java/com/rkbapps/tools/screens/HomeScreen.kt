package com.rkbapps.tools.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import com.rkbapps.tools.models.MenuItem
import com.rkbapps.tools.models.docScanner
import com.rkbapps.tools.models.imageSegementation
import com.rkbapps.tools.models.qrScanner
import com.rkbapps.tools.models.textRecognition

class HomeScreen() : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(text = "Tools") }, colors = TopAppBarDefaults.topAppBarColors(
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
                MenuRows(item1 = {
                    MenuItems(manuItem = docScanner) {
                        navigator!!.push(DocScannerScreen())
                    }
                }, item2 = {
                    MenuItems(manuItem = qrScanner) {
                        navigator!!.push(BarcodeScanScreen())
                    }
                })

//                Spacer(modifier = Modifier.height(8.dp))

                MenuRows(item1 = {
                    MenuItems(manuItem = imageSegementation) {
                        navigator!!.push(ImageSegmentationScreen())
                    }
                }, item2 = {
                    MenuItems(manuItem = textRecognition) {
                        navigator!!.push(TextReorganizationScreen())
                    }
                })

            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MenuItems(manuItem: MenuItem, onItemClick: () -> Unit) {
        OutlinedCard(
            onClick = {
                onItemClick()
            }, modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(8.dp)
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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


}














