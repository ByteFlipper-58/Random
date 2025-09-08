package com.byteflipper.random.ui.lists

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListTopBar(
    onBack: () -> Unit,
    title: String,
    onShowSave: (() -> Unit)? = null,
    onShowRename: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            onShowSave?.let {
                IconButton(onClick = it) {
                    Icon(painterResource(R.drawable.save_24px), contentDescription = stringResource(R.string.save))
                }
            }
            onShowRename?.let {
                IconButton(onClick = it) {
                    Icon(painterResource(R.drawable.edit_24px), contentDescription = stringResource(R.string.rename))
                }
            }
        }
    )
}


