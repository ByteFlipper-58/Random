package com.byteflipper.random.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    onOpenMenu: () -> Unit,
    onOpenSearch: (() -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        actions = {
            if (onOpenSearch != null) {
                IconButton(onClick = onOpenSearch) {
                    Icon(
                        painter = painterResource(id = R.drawable.search_24px),
                        contentDescription = stringResource(R.string.search_presets),
                    )
                }
            }
            IconButton(onClick = onOpenMenu) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert_24px),
                    contentDescription = stringResource(R.string.menu),
                )
            }
        }
    )
}


