package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.byteflipper.random.ui.components.EditorList

@Composable
fun ListContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    items: List<String>,
    onItemsChange: (List<String>) -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        EditorList(
            items = SnapshotStateList<String>().apply {
                clear()
                addAll(items)
            },
            onItemsChange = onItemsChange,
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            contentPadding = contentPadding,
            minItems = 1
        )
    }
}
