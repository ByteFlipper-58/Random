package com.byteflipper.random.ui.presets.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.byteflipper.random.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PresetsFabControls(
    expanded: Boolean,
    hasPresets: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreatePreset: () -> Unit,
    onImportFile: () -> Unit,
    onImportClipboard: () -> Unit,
    onExportAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(expanded) {
        onExpandedChange(false)
    }

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange
            ) {
                val imageVector = remember {
                    derivedStateOf {
                        if (checkedProgress > 0.5f) {
                            true
                        } else {
                            false
                        }
                    }
                }

                val showClose = imageVector.value
                if (showClose) {
                    Icon(
                        painter = painterResource(id = R.drawable.close_24px),
                        contentDescription = stringResource(R.string.preset_actions)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.add_24px),
                        contentDescription = stringResource(R.string.preset_actions)
                    )
                }
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                onExpandedChange(false)
                onCreatePreset()
            },
            text = { Text(stringResource(R.string.create_preset)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.note_add_24px),
                    contentDescription = null
                )
            }
        )

        FloatingActionButtonMenuItem(
            onClick = {
                onExpandedChange(false)
                onImportFile()
            },
            text = { Text(stringResource(R.string.import_presets)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.upload_24px),
                    contentDescription = null
                )
            }
        )

        FloatingActionButtonMenuItem(
            onClick = {
                onExpandedChange(false)
                onImportClipboard()
            },
            text = { Text(stringResource(R.string.import_clipboard)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.content_paste_24px),
                    contentDescription = null
                )
            }
        )

        FloatingActionButtonMenuItem(
            onClick = {
                if (!hasPresets) return@FloatingActionButtonMenuItem
                onExpandedChange(false)
                onExportAll()
            },
            text = { Text(stringResource(R.string.export_all_presets)) },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.upload_24px),
                    contentDescription = null
                )
            }
        )
    }
}
