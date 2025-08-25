package com.byteflipper.random.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onOpenListById: (Long) -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onAddNumbersPreset: () -> Unit,
    onAddListPreset: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { ListPresetRepository.fromContext(context) }
    var presets by remember { mutableStateOf<List<ListPreset>>(emptyList()) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createName by rememberSaveable { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<ListPreset?>(null) }
    var renameName by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repo.observeAll().collectLatest { list -> presets = list }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Рандом") }, actions = {
            IconButton(onClick = { /* TODO: settings */ }) { Icon(Icons.Outlined.Settings, contentDescription = "Настройки") }
            IconButton(onClick = { /* TODO: menu */ }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Меню") }
        }) }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        MenuCard(
            icon = Icons.Outlined.FormatListNumbered,
            title = "Числа",
            onClick = onOpenNumbers,
            onAddClick = onAddNumbersPreset
        )
        Spacer(Modifier.height(12.dp))
        MenuCard(
            icon = Icons.Outlined.FormatListBulleted,
            title = "Список",
            onClick = onOpenList,
            onAddClick = { showCreateDialog = true }
        )
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.Casino, title = "Игральные кости", onClick = onOpenDice)
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.Gavel, title = "Жребий", onClick = onOpenLot)
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.MonetizationOn, title = "Монетка", onClick = onOpenCoin)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(presets, key = { it.id }) { p ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    onClick = { onOpenListById(p.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.FormatListBulleted, contentDescription = null)
                        Text(
                            text = p.name,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Переименовать") }, onClick = {
                                expanded = false
                                renameTarget = p
                                renameName = p.name
                            })
                            androidx.compose.material3.DropdownMenuItem(text = { Text("Удалить") }, onClick = {
                                expanded = false
                                scope.launch { repo.delete(p) }
                            })
                        }
                        IconButton(onClick = { expanded = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Меню")
                        }
                    }
                }
            }
        }
        }
    }

    if (showCreateDialog) {
        val nextNumber = (presets.size + 1)
        if (createName.isBlank()) createName = "Новый список $nextNumber"
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Новый список") },
            text = {
                Column {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        singleLine = true,
                        label = { Text("Название списка") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Будут добавлены элементы: Элемент 1, Элемент 2, Элемент 3")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = createName.trim().ifEmpty { "Новый список $nextNumber" }
                    scope.launch {
                        val id = repo.upsert(
                            ListPreset(
                                name = name,
                                items = listOf("Элемент 1", "Элемент 2", "Элемент 3")
                            )
                        )
                        showCreateDialog = false
                        createName = ""
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Переименовать список") },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    singleLine = true,
                    label = { Text("Новое название") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val t = renameTarget
                    val newName = renameName.trim()
                    if (t != null && newName.isNotEmpty()) {
                        scope.launch { repo.upsert(t.copy(name = newName)) }
                        renameTarget = null
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun MenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    onAddClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.titleMedium
            )
            if (onAddClick != null) {
                IconButton(onClick = onAddClick) {
                    Icon(imageVector = Icons.Outlined.Add, contentDescription = "Добавить пресет")
                }
            }
        }
    }
}


