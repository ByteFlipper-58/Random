package com.byteflipper.random.ui.lot

import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LotTopBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(stringResource(R.string.lot_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.arrow_back_24px), contentDescription = stringResource(R.string.back))
            }
        }
    )
}


