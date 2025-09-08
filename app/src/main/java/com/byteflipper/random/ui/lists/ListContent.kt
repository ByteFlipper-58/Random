package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.EditorList

@Composable
fun ListContent(
    modifier: Modifier = Modifier,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        EditorList(
            items = androidx.compose.runtime.snapshots.SnapshotStateList<String>().apply {
                clear()
                addAll(items)
            },
            onItemsChange = onItemsChange,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            minItems = 1
        )
    }
}


