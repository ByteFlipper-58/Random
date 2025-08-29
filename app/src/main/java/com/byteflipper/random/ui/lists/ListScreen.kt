package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.ui.components.FlipCardControls
import com.byteflipper.random.ui.components.FlipCardOverlay
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.rememberFlipCardState
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.data.settings.Settings
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.min
private fun Set<String>.indicesOf(baseSize: Int): Set<Int> {
    // Just a bounded placeholder set for the dialog. We don't need exact numbers UI for lists.
    return if (this.isEmpty()) emptySet() else (0 until min(this.size, baseSize)).toSet()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(onBack: () -> Unit, presetId: Long? = null, onOpenListById: (Long) -> Unit = {}) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = remember { ListPresetRepository.fromContext(context) }

    // Получение строк из ресурсов
    val listString = stringResource(R.string.list)
    val item1String = stringResource(R.string.item_1)
    val item2String = stringResource(R.string.item_2)
    val item3String = stringResource(R.string.item_3)

    // UI state
    var selectedPreset by remember { mutableStateOf<ListPreset?>(null) }
    var presets by remember { mutableStateOf<List<ListPreset>>(emptyList()) }
    var showConfigDialog by rememberSaveable { mutableStateOf(false) }
    var useDelay by rememberSaveable { mutableStateOf(true) }
    var delayText by rememberSaveable { mutableStateOf("3000") }
    var countText by rememberSaveable { mutableStateOf("1") }
    var allowRepetitions by rememberSaveable { mutableStateOf(true) }

    // Editor state
    val editorItems = remember { mutableStateListOf<String>() }
    var newItem by rememberSaveable { mutableStateOf("") }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameName by rememberSaveable { mutableStateOf("") }
    var defaultName by rememberSaveable { mutableStateOf(listString) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var saveName by rememberSaveable { mutableStateOf("") }
    var openAfterSave by rememberSaveable { mutableStateOf(true) }
    var usedItems by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Focus state
    val focusRequesters = remember { mutableStateListOf<FocusRequester>() }
    var pendingFocusIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val focusManager = LocalFocusManager.current

    var results by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // FAB geometry
    var fabCenterInRoot by remember { mutableStateOf(Offset.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }

    // Flip card
    val flipState = rememberFlipCardState()
    val flipCtrl = FlipCardControls(flipState)

    // Load default or specific preset by id, and prepare editor state
    LaunchedEffect(presetId) {
        if (presetId == null) {
            // Load from SharedPreferences and keep changes local (not repository)
            val sp = context.getSharedPreferences("random_prefs", android.content.Context.MODE_PRIVATE)
            defaultName = sp.getString("default_list_name", null) ?: listString
            val raw = sp.getString("default_list_items", null)
            val items = if (raw == null) listOf(item1String, item2String, item3String) else raw.split('\u0001')
            editorItems.clear(); editorItems.addAll(items)
            if (editorItems.isEmpty()) editorItems.add("")
        } else {
            repo.getById(presetId)?.let { p ->
                selectedPreset = p
                presets = listOf(p)
                editorItems.clear(); editorItems.addAll(p.items)
                if (editorItems.isEmpty()) editorItems.add("")
            }
        }
    }

    fun saveCurrent() {
        if (presetId == null) {
            // Persist to SharedPreferences only
            val sp = context.getSharedPreferences("random_prefs", android.content.Context.MODE_PRIVATE)
            val joined = editorItems.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\u0001")
            sp.edit().putString("default_list_items", joined).apply()
        } else {
            val current = selectedPreset ?: return
            val updated = current.copy(
                items = editorItems.map { it.trim() }.filter { it.isNotEmpty() }
            )
            selectedPreset = updated
            scope.launch { repo.upsert(updated) }
        }
    }

    fun generate(): List<String> {
        val base: List<String> = if (presetId == null) {
            editorItems.map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            val p = selectedPreset ?: return emptyList()
            p.items.map { it.trim() }.filter { it.isNotEmpty() }
        }
        val n = countText.toIntOrNull()?.coerceAtLeast(1) ?: 1
        return if (allowRepetitions) {
            if (base.isEmpty()) emptyList() else List(n) { base.random() }
        } else {
            val pool = base.map { it.trim() }.filter { it.isNotEmpty() && it !in usedItems }.distinct()
            if (pool.isEmpty()) emptyList() else pool.shuffled().take(min(n, pool.size))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (presetId == null) defaultName else (selectedPreset?.name ?: listString)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = null) } },
                actions = {
                    if (presetId == null) {
                        TextButton(onClick = {
                            saveName = defaultName
                            showSaveDialog = true
                        }) { Text(stringResource(R.string.save_settings)) }
                        IconButton(onClick = {
                            renameName = defaultName
                            showRenameDialog = true
                        }) { Icon(painterResource(R.drawable.edit_24px), contentDescription = stringResource(R.string.rename)) }
                    } else {
                        IconButton(onClick = {
                            renameName = selectedPreset?.name ?: ""
                            showRenameDialog = true
                        }) { Icon(painterResource(R.drawable.edit_24px), contentDescription = stringResource(R.string.rename)) }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showConfigDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) { Icon(painterResource(R.drawable.settings_24px), contentDescription = null) }

                Box(modifier = Modifier.onGloballyPositioned { c ->
                    fabSize = c.size
                    val pos = c.positionInRoot()
                    fabCenterInRoot = Offset(pos.x + fabSize.width / 2f, pos.y + fabSize.height / 2f)
                }) {
                    val settingsRepo = remember { SettingsRepository.fromContext(context) }
                    val settings: Settings by settingsRepo.settingsFlow.collectAsState(initial = Settings())
                    SizedFab(
                        size = settings.fabSize,
                        onClick = {
                        val base = if (presetId == null) {
                            editorItems.map { it.trim() }.filter { it.isNotEmpty() }
                        } else {
                            selectedPreset?.items?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                        }
                        if (base.isEmpty()) {
                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.list_empty)) }
                            return@SizedFab
                        }
                        if (!allowRepetitions) {
                            val pool = base.map { it.trim() }.filter { it.isNotEmpty() && it !in usedItems }.distinct()
                            if (pool.isEmpty()) {
                                scope.launch {
                                    val res = snackbarHostState.showSnackbar(
                                        message = context.getString(R.string.all_options_used),
                                        actionLabel = context.getString(R.string.reset)
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        usedItems = emptySet()
                                    }
                                }
                                return@SizedFab
                            }
                        }
                        val ms = if (useDelay) delayText.toIntOrNull() ?: 3000 else 1000
                        if (!flipCtrl.isVisible()) flipCtrl.open()
                        flipCtrl.spinAndReveal(
                            effectiveDelayMs = ms,
                            onReveal = { isFront ->
                                val out = generate()
                                results = out
                                if (!allowRepetitions) {
                                    val toAdd = out.map { it.trim() }.filter { it.isNotEmpty() }
                                    if (toAdd.isNotEmpty()) usedItems = usedItems + toAdd
                                }
                            }
                        )
                    },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) { Icon(painterResource(R.drawable.autorenew_24px), contentDescription = null) }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
            val blur = (8f * flipCtrl.scrimProgress.value).dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .blur(blur),
                verticalArrangement = Arrangement.Top
            ) {
                if (presetId == null || selectedPreset != null) {
                    // Keep focusRequesters in sync with item count
                    if (focusRequesters.size < editorItems.size) {
                        repeat(editorItems.size - focusRequesters.size) { focusRequesters.add(FocusRequester()) }
                    } else if (focusRequesters.size > editorItems.size) {
                        repeat(focusRequesters.size - editorItems.size) { focusRequesters.removeLast() }
                    }

                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(editorItems.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                BasicTextField(
                                    value = editorItems[index],
                                    onValueChange = { newVal ->
                                        editorItems[index] = newVal
                                        saveCurrent()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequesters[index])
                                        .onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.key == Key.Backspace && editorItems[index].isEmpty()) {
                                                if (editorItems.size > 1) {
                                                    editorItems.removeAt(index)
                                                    val newIndex = (index - 1).coerceAtLeast(0)
                                                    pendingFocusIndex = newIndex
                                                    saveCurrent()
                                                }
                                                true
                                            } else false
                                        },
                                    textStyle = MaterialTheme.typography.displayLarge.copy(
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 32.sp
                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(
                                        onNext = {
                                            if (index == editorItems.lastIndex) {
                                                if (editorItems[index].isNotBlank()) {
                                                    editorItems.add("")
                                                    pendingFocusIndex = index + 1
                                                    saveCurrent()
                                                }
                                            } else {
                                                focusRequesters[index + 1].requestFocus()
                                            }
                                        }
                                    )
                                )
                            }
                        }
                    }
                } else {
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Flip overlay
            FlipCardOverlay(
                state = flipState,
                anchorInRoot = fabCenterInRoot,
                onClosed = { results = emptyList() },
                frontContent = {
                    if (results.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.results), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            results.forEach { s ->
                                Text(s, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                },
                backContent = {
                    if (results.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.results), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(6.dp))
                            results.forEach { s ->
                                Text(s, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            )

            // Config dialog (reuse)
            GeneratorConfigDialog(
                visible = showConfigDialog,
                onDismissRequest = { showConfigDialog = false },
                countText = countText,
                onCountChange = { countText = it },
                allowRepetitions = allowRepetitions,
                onAllowRepetitionsChange = { allowRepetitions = it },
                usedNumbers = usedItems.indicesOf(baseSize = 1_000_000),
                availableRange = null,
                onResetUsedNumbers = { usedItems = emptySet() },
                useDelay = useDelay,
                onUseDelayChange = { useDelay = it },
                delayText = delayText,
                onDelayChange = { delayText = it }
            )

            // Rename dialog
            if (showRenameDialog) {
                AlertDialog(
                    onDismissRequest = { showRenameDialog = false },
                    title = { Text(stringResource(R.string.rename_list)) },
                    text = {
                        OutlinedTextField(
                            value = renameName,
                            onValueChange = { renameName = it },
                            singleLine = true,
                            label = { Text(stringResource(R.string.new_name)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val newName = renameName.trim()
                            if (newName.isNotEmpty()) {
                                if (presetId == null) {
                                    defaultName = newName
                                    val sp = context.getSharedPreferences("random_prefs", android.content.Context.MODE_PRIVATE)
                                    sp.edit().putString("default_list_name", newName).apply()
                                    showRenameDialog = false
                                } else {
                                    val current = selectedPreset
                                    if (current != null) {
                                        val updated = current.copy(name = newName)
                                        selectedPreset = updated
                                        scope.launch { repo.upsert(updated) }
                                        showRenameDialog = false
                                    }
                                }
                            }
                        }) { Text(stringResource(R.string.save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }

            // Save as new preset dialog (for default screen)
            if (showSaveDialog) {
                AlertDialog(
                    onDismissRequest = { showSaveDialog = false },
                    title = { Text(stringResource(R.string.save_settings)) },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = saveName,
                                onValueChange = { saveName = it },
                                singleLine = true,
                                label = { Text(stringResource(R.string.list_name)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = openAfterSave, onCheckedChange = { openAfterSave = it })
                                Text(stringResource(R.string.open_after_save), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = saveName.trim()
                            val items = editorItems.map { it.trim() }.filter { it.isNotEmpty() }
                            if (name.isNotEmpty() && items.isNotEmpty()) {
                                scope.launch {
                                    val newId = repo.upsert(ListPreset(name = name, items = items))
                                    if (openAfterSave) onOpenListById(newId)
                                }
                                showSaveDialog = false
                            }
                        }) { Text(stringResource(R.string.save)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }

    // Focus new item if needed
    LaunchedEffect(editorItems.size, pendingFocusIndex) {
        val i = pendingFocusIndex
        if (i != null && i in 0 until focusRequesters.size) {
            focusRequesters[i].requestFocus()
            pendingFocusIndex = null
        }
    }
}


