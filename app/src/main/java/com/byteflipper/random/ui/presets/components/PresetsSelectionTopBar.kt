package com.byteflipper.random.ui.presets.components

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetsSelectionTopBar(
    selectedCount: Int,
    hasSelection: Boolean,
    canMerge: Boolean,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onMerge: () -> Unit
) {
    var moreExpanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(stringResource(R.string.selected_presets_count, selectedCount))
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back_24px),
                    contentDescription = stringResource(R.string.close)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onShare,
                enabled = hasSelection
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share_24px),
                    contentDescription = stringResource(R.string.share_selected)
                )
            }
            IconButton(
                onClick = onExport,
                enabled = hasSelection
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.upload_24px),
                    contentDescription = stringResource(R.string.export_selected)
                )
            }
            IconButton(
                onClick = { moreExpanded = true },
                enabled = hasSelection
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_vert_24px),
                    contentDescription = stringResource(R.string.menu)
                )
            }
            DropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = { moreExpanded = false },
                shape = RoundedDropdownMenuShape
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.merge_presets)) },
                    enabled = canMerge,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.apps_24px),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        moreExpanded = false
                        onMerge()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.delete_selected)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.delete_24px),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        moreExpanded = false
                        onDelete()
                    }
                )
            }
        }
    )
}
