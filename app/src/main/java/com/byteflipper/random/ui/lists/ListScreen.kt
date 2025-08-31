package com.byteflipper.random.ui.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.byteflipper.random.R
import com.byteflipper.random.data.preset.ListPreset
import com.byteflipper.random.data.preset.ListPresetRepository
import com.byteflipper.random.data.settings.Settings
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.ui.components.EditorList
import com.byteflipper.random.ui.components.FlipCardControls
import com.byteflipper.random.ui.components.FlipCardOverlay
import com.byteflipper.random.ui.components.GeneratorConfigDialog
import com.byteflipper.random.ui.components.SizedFab
import com.byteflipper.random.ui.components.rememberFlipCardState
import com.byteflipper.random.ui.theme.getRainbowColors
import kotlinx.coroutines.launch
import kotlin.math.min

// Функция для получения контрастного цвета текста на основе цвета фона
private fun getContrastColor(backgroundColor: Color): Color {
    // Вычисляем яркость цвета фона (формула luminance)
    val luminance = backgroundColor.luminance()

    // Если фон светлый (luminance > 0.5), используем черный текст
    // Если фон темный (luminance <= 0.5), используем белый текст
    return if (luminance > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}

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
                        IconButton(onClick = {
                            saveName = defaultName
                            showSaveDialog = true
                        }) { Icon(painterResource(R.drawable.save_24px), contentDescription = stringResource(R.string.save_settings)) }
                    } else {
                        IconButton(onClick = {
                            renameName = selectedPreset?.name ?: ""
                            showRenameDialog = true
                        }) { Icon(painterResource(R.drawable.edit_24px), contentDescription = stringResource(R.string.rename)) }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
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
                    EditorList(
                        items = editorItems,
                        onItemsChange = { saveCurrent() },
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        minItems = 1
                    )
                } else {
                    Text(stringResource(R.string.loading), style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Получить цвета радуги и выбрать случайный для карточки
            val rainbowColors = getRainbowColors()
            val cardColor = remember(results) { rainbowColors.random() }

            // Адаптивный размер карточки для списков
            val listCardSize = 320.dp

            // Flip overlay
            FlipCardOverlay(
                state = flipState,
                anchorInRoot = fabCenterInRoot,
                onClosed = { results = emptyList() },
                // Используем один и тот же цвет для обеих сторон карточки
                frontContainerColor = cardColor,
                backContainerColor = cardColor,
                cardSize = listCardSize,
                frontContent = {
                    if (results.isNotEmpty()) {
                        // Адаптивные отступы и размеры - увеличены для лучшей видимости
                        val adaptivePadding = (listCardSize.value * 0.04f).coerceIn(12f, 28f).dp
                        val adaptiveSpacing = (listCardSize.value * 0.03f).coerceIn(6f, 16f).dp
                        val titleFontSize = (listCardSize.value * 0.045f).coerceIn(18f, 32f).sp
                        val itemFontSize = (listCardSize.value * 0.035f).coerceIn(20f, 36f).sp

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(adaptivePadding)) {
                            // Адаптируем цвет текста под цвет фона карточки
                            val titleTextColor = getContrastColor(cardColor).copy(alpha = 0.8f)
                            val itemTextColor = getContrastColor(cardColor)
                            Text(stringResource(R.string.results), style = MaterialTheme.typography.labelMedium.copy(fontSize = titleFontSize), color = titleTextColor)
                            Spacer(Modifier.height(adaptiveSpacing))
                            results.forEach { s ->
                                Text(s, style = MaterialTheme.typography.titleMedium.copy(fontSize = itemFontSize), color = itemTextColor)
                            }
                        }
                    }
                },
                backContent = {
                    if (results.isNotEmpty()) {
                        // Адаптивные отступы и размеры - увеличены для лучшей видимости
                        val adaptivePadding = (listCardSize.value * 0.04f).coerceIn(12f, 28f).dp
                        val adaptiveSpacing = (listCardSize.value * 0.03f).coerceIn(6f, 16f).dp
                        val titleFontSize = (listCardSize.value * 0.045f).coerceIn(18f, 32f).sp
                        val itemFontSize = (listCardSize.value * 0.035f).coerceIn(20f, 36f).sp

                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(adaptivePadding)) {
                            // Адаптируем цвет текста под цвет фона карточки
                            val titleTextColor = getContrastColor(cardColor).copy(alpha = 0.8f)
                            val itemTextColor = getContrastColor(cardColor)
                            Text(stringResource(R.string.results), style = MaterialTheme.typography.labelMedium.copy(fontSize = titleFontSize), color = titleTextColor)
                            Spacer(Modifier.height(adaptiveSpacing))
                            results.forEach { s ->
                                Text(s, style = MaterialTheme.typography.titleMedium.copy(fontSize = itemFontSize), color = itemTextColor)
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

            // Rename dialog (only for saved presets)
            if (showRenameDialog && presetId != null) {
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
                                val current = selectedPreset
                                if (current != null) {
                                    val updated = current.copy(name = newName)
                                    selectedPreset = updated
                                    scope.launch { repo.upsert(updated) }
                                    showRenameDialog = false
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


}


