package com.byteflipper.random.ui.numbers

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumbersTopBar(onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.number),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}