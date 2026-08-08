package com.byteflipper.random.ui.teams

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.byteflipper.random.R

import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsTopBar(
    onBack: () -> Unit,
    onOpenPreset: () -> Unit,
    hasPresets: Boolean,
    onManagePeople: () -> Unit,
    onSavePreset: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = stringResource(R.string.teams),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    painter = painterResource(R.drawable.more_vert_24px),
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = RoundedDropdownMenuShape
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.save_team_preset)) },
                    onClick = {
                        menuExpanded = false
                        onSavePreset()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.save_24px),
                            contentDescription = null
                        )
                    }
                )
                if (hasPresets) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.open_team_preset)) },
                        onClick = {
                            menuExpanded = false
                            onOpenPreset()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.folder_open_24px),
                                contentDescription = null
                            )
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.manage_people)) },
                    onClick = {
                        menuExpanded = false
                        onManagePeople()
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.manage_accounts_24px),
                            contentDescription = null
                        )
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}
