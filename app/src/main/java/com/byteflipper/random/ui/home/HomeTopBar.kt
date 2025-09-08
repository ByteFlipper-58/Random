package com.byteflipper.random.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onOpenAbout: () -> Unit, onOpenSettings: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.random),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        actions = {
            IconButton(onClick = onOpenAbout) {
                Icon(
                    painterResource(id = R.drawable.info_24px),
                    contentDescription = stringResource(R.string.about_app),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painterResource(id = R.drawable.settings_24px),
                    contentDescription = stringResource(R.string.settings),
                )
            }
        }
    )
}


