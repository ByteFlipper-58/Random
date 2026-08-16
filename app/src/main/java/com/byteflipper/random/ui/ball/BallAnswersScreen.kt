package com.byteflipper.random.ui.ball

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.byteflipper.random.R
import com.byteflipper.random.ui.ball.components.BallAnswerRow
import com.byteflipper.random.ui.common.MenuBackItem
import com.byteflipper.random.ui.common.PeopleSectionItem
import com.byteflipper.random.ui.common.PeopleSourceMenuItems
import com.byteflipper.random.ui.common.QuickTemplateMenuItems
import com.byteflipper.random.ui.common.SourceMenuSection
import com.byteflipper.random.ui.components.LocalHapticsManager
import com.byteflipper.random.ui.components.RoundedDropdownMenuShape
import com.byteflipper.random.ui.components.SizedFab
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Recoloring of the field's border when it switches between adding and editing. */
private const val EDIT_MODE_DURATION_MS = 320

/**
 * Width of the button row beside the field. A spring rather than a tween: with a tween the field
 * jerked on the way back from edit mode, because its growth was cut off exactly at the target width.
 */
private val FIELD_RESIZE_SPEC = spring<IntSize>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow
)

private val ICON_SCALE_SPEC = spring<Float>(
    dampingRatio = 0.65f,
    stiffness = Spring.StiffnessMediumLow
)

private val ICON_FADE_SPEC = tween<Float>(durationMillis = 220)

/**
 * Where the answers come from, and what they say.
 *
 * The classic set and the saved presets are *sources*: picking one takes effect at once and the ball
 * keeps following it. Everything else — a team's names, a quick template, anything typed into the
 * field at the top — is a copy that lands in the list below and is only stored when saved.
 *
 * One field serves both adding and editing, the way the wheel's editor does: tapping a row loads it
 * into the field, the add button becomes a tick, and a bin appears beside it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BallAnswersScreen(onBack: () -> Unit) {
    val viewModel: BallAnswersViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val hapticsManager = LocalHapticsManager.current

    var isBackHandled by remember { mutableStateOf(false) }
    val safeBack = {
        if (!isBackHandled) {
            isBackHandled = true
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                BallAnswersUiEffect.Saved -> safeBack()
                is BallAnswersUiEffect.ShowMessage -> snackbarHostState.showSnackbar(
                    message = effect.formatArg
                        ?.let { context.getString(effect.messageRes, it) }
                        ?: context.getString(effect.messageRes)
                )
            }
        }
    }

    var newAnswer by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingText by rememberSaveable { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var menuSection by remember { mutableStateOf(SourceMenuSection.Root) }

    // Derived rather than remembered, so a reseeded list (a preset, a reset) drops the edit on its
    // own instead of leaving the field pointing at a row that is gone.
    val editingIndex = uiState.draft.indexOfFirst { row -> row.id == editingId }
    val isEditing = editingIndex >= 0
    val canRemove = uiState.draft.size > BALL_MIN_ANSWERS

    val cancelEdit = {
        editingId = null
        editingText = ""
    }

    val primaryAction = {
        if (isEditing) {
            val id = editingId
            if (id != null && editingText.isNotBlank()) {
                viewModel.updateAnswer(id, editingText.trim())
                cancelEdit()
            }
        } else {
            if (viewModel.addAnswer(newAnswer)) newAnswer = ""
        }
    }

    val closeMenu = {
        showMenu = false
        menuSection = SourceMenuSection.Root
    }

    // Which face carries which answer is the order of this list, so it is worth being able to change.
    // Only the rows below are wrapped in a reorderable item, so the field and the counter above them
    // cannot be picked up or dropped onto.
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        viewModel.moveAnswer(from.key as Long, to.key as Long)
        // A tick per swap, the way the wheel ticks as sectors pass the pointer.
        if (settings.hapticsEnabled) hapticsManager?.performTick(settings.hapticsIntensity)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.ball_answers_editor)) },
                navigationIcon = {
                    IconButton(onClick = safeBack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_24px),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert_24px),
                                contentDescription = stringResource(R.string.menu)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = closeMenu,
                            shape = RoundedDropdownMenuShape
                        ) {
                            when (menuSection) {
                                SourceMenuSection.People -> {
                                    MenuBackItem(
                                        onClick = { menuSection = SourceMenuSection.Presets }
                                    )

                                    PeopleSourceMenuItems(minItems = BALL_MIN_ANSWERS) { _, names ->
                                        closeMenu()
                                        // A copy: the ball has no people source of its own.
                                        cancelEdit()
                                        viewModel.applyItems(names)
                                    }
                                }

                                SourceMenuSection.Templates -> {
                                    MenuBackItem(onClick = { menuSection = SourceMenuSection.Root })

                                    QuickTemplateMenuItems { template ->
                                        closeMenu()
                                        cancelEdit()
                                        viewModel.applyItems(template.items)
                                    }
                                }

                                SourceMenuSection.Presets -> {
                                    MenuBackItem(onClick = { menuSection = SourceMenuSection.Root })

                                    PeopleSectionItem(
                                        onClick = { menuSection = SourceMenuSection.People }
                                    )

                                    if (presets.isNotEmpty()) {
                                        HorizontalDivider()
                                    }

                                    presets.forEach { preset ->
                                        val fits = preset.items.size >= BALL_MIN_ANSWERS

                                        DropdownMenuItem(
                                            enabled = fits,
                                            text = {
                                                Column {
                                                    Text(
                                                        text = preset.name,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = if (fits) {
                                                            stringResource(
                                                                R.string.ball_answers_count,
                                                                preset.items.size
                                                            )
                                                        } else {
                                                            stringResource(
                                                                R.string.ball_answers_min,
                                                                BALL_MIN_ANSWERS
                                                            )
                                                        },
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme
                                                            .onSurfaceVariant
                                                    )
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    painter = painterResource(
                                                        R.drawable.list_alt_24px
                                                    ),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            onClick = {
                                                closeMenu()
                                                cancelEdit()
                                                viewModel.usePreset(preset)
                                            }
                                        )
                                    }
                                }

                                SourceMenuSection.Root -> {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.presets)) },
                                        onClick = { menuSection = SourceMenuSection.Presets },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(
                                                    R.drawable.folder_open_24px
                                                ),
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.wheel_templates)) },
                                        onClick = { menuSection = SourceMenuSection.Templates },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(R.drawable.list_alt_24px),
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    HorizontalDivider()

                                    // The reset is not "undo my edits" but "give me the toy's own
                                    // answers back", which is why it names them.
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.reset),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.ball_answers_source_classic
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme
                                                        .onSurfaceVariant
                                                )
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                painter = painterResource(
                                                    R.drawable.restart_alt_24px
                                                ),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            closeMenu()
                                            cancelEdit()
                                            viewModel.resetToClassic()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Nothing worth storing means nothing to press: the list is already what the ball uses.
            if (uiState.canSave) {
                SizedFab(
                    size = settings.fabSize,
                    onClick = viewModel::save,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check_24px),
                        contentDescription = stringResource(R.string.save)
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 100.dp
            )
        ) {
            item(key = "input") {
                // One field for both modes: it stays in place and shrinks because the row of buttons
                // to its right changes width with an animation.
                val focusedBorderColor by animateColorAsState(
                    targetValue = if (isEditing) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    animationSpec = tween(EDIT_MODE_DURATION_MS),
                    label = "field_focused_border"
                )
                val unfocusedBorderColor by animateColorAsState(
                    targetValue = if (isEditing) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    animationSpec = tween(EDIT_MODE_DURATION_MS),
                    label = "field_unfocused_border"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = if (isEditing) editingText else newAnswer,
                        onValueChange = { value ->
                            if (value.length <= BALL_MAX_ANSWER_LENGTH) {
                                if (isEditing) editingText = value else newAnswer = value
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = if (isEditing) {
                                    stringResource(R.string.ball_answers_face, editingIndex + 1)
                                } else {
                                    stringResource(R.string.ball_answers_new)
                                }
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        enabled = isEditing || !uiState.atLimit,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { primaryAction() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = focusedBorderColor,
                            unfocusedBorderColor = unfocusedBorderColor
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        // The row animates its width and the field takes whatever is left through
                        // weight(1f), so it shrinks smoothly when the bin appears.
                        modifier = Modifier.animateContentSize(animationSpec = FIELD_RESIZE_SPEC)
                    ) {
                        FilledTonalIconButton(
                            onClick = primaryAction,
                            enabled = if (isEditing) {
                                editingText.isNotBlank()
                            } else {
                                newAnswer.isNotBlank() && !uiState.atLimit
                            }
                        ) {
                            AnimatedContent(
                                targetState = isEditing,
                                transitionSpec = {
                                    (
                                        fadeIn(ICON_FADE_SPEC) +
                                            scaleIn(ICON_SCALE_SPEC, initialScale = 0.7f)
                                        ).togetherWith(
                                        fadeOut(ICON_FADE_SPEC) +
                                            scaleOut(ICON_SCALE_SPEC, targetScale = 0.7f)
                                    )
                                },
                                label = "primary_action_icon"
                            ) { editing ->
                                Icon(
                                    painter = painterResource(
                                        if (editing) R.drawable.check_24px else R.drawable.add_24px
                                    ),
                                    contentDescription = stringResource(
                                        if (editing) R.string.confirm else R.string.add
                                    )
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isEditing,
                            enter = fadeIn(ICON_FADE_SPEC) +
                                scaleIn(ICON_SCALE_SPEC, initialScale = 0.6f),
                            exit = fadeOut(ICON_FADE_SPEC) +
                                scaleOut(ICON_SCALE_SPEC, targetScale = 0.6f)
                        ) {
                            // At the minimum the button stays put and only dims: a disappearing
                            // button would read as a feature that went missing.
                            IconButton(
                                onClick = {
                                    editingId?.let { id -> viewModel.removeAnswer(id) }
                                    cancelEdit()
                                },
                                enabled = canRemove
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_24px),
                                    contentDescription = stringResource(R.string.delete),
                                    tint = if (canRemove) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                            .copy(alpha = 0.38f)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            item(key = "source") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = stringResource(
                            R.string.ball_answers_count_of,
                            uiState.draft.size,
                            BALL_MAX_ANSWERS
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(uiState.draft, key = { _, row -> row.id }) { index, row ->
                ReorderableItem(state = reorderState, key = row.id) { dragging ->
                    Column {
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        BallAnswerRow(
                            faceNumber = index + 1,
                            text = row.text,
                            canDelete = canRemove,
                            selected = editingId == row.id,
                            dragging = dragging,
                            onClick = {
                                // Tapping the row in the field again puts the field back to adding,
                                // since the bin took the place of a cancel button.
                                if (editingId == row.id) {
                                    cancelEdit()
                                } else {
                                    editingId = row.id
                                    editingText = row.text
                                }
                            },
                            onDelete = {
                                if (editingId == row.id) cancelEdit()
                                viewModel.removeAnswer(row.id)
                            },
                            // The whole row is the handle: there is nothing to aim at, and a long
                            // press is what tells it apart from a tap and from the list scrolling.
                            modifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    if (settings.hapticsEnabled) {
                                        hapticsManager?.performPress(settings.hapticsIntensity)
                                    }
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
