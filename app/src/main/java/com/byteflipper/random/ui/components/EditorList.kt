package com.byteflipper.random.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Компонент редактора списка с поведением текстового редактора
 *
 * @param items Список строк для редактирования
 * @param onItemsChange Коллбек при изменении списка элементов
 * @param modifier Модификатор для компонента
 * @param minItems Минимальное количество элементов в списке (по умолчанию 1)
 */
@Composable
fun EditorList(
    items: SnapshotStateList<String>,
    onItemsChange: (List<String>) -> Unit = {},
    modifier: Modifier = Modifier,
    minItems: Int = 1
) {
    // Состояние фокуса
    val focusRequesters = remember { mutableStateListOf<FocusRequester>() }
    val pendingFocusIndex = remember { mutableStateOf<Int?>(null) }

    // Синхронизация FocusRequester с количеством элементов
    LaunchedEffect(items.size) {
        // Синхронизируем размер списка focusRequesters с размером items
        when {
            focusRequesters.size < items.size -> {
                // Добавляем недостающие FocusRequester
                repeat(items.size - focusRequesters.size) {
                    focusRequesters.add(FocusRequester())
                }
            }
            focusRequesters.size > items.size -> {
                // Удаляем лишние FocusRequester
                repeat(focusRequesters.size - items.size) {
                    focusRequesters.removeAt(focusRequesters.lastIndex)
                }
            }
        }
    }

    // Обработка отложенного фокуса
    LaunchedEffect(items.size, pendingFocusIndex.value) {
        val index = pendingFocusIndex.value
        if (index != null && index in 0 until focusRequesters.size) {
            focusRequesters[index].requestFocus()
            pendingFocusIndex.value = null
        }
    }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(items.size) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = items[index],
                    onValueChange = { newValue ->
                        items[index] = newValue
                        onItemsChange(items.toList())
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(
                            key(index) {
                                focusRequesters.getOrNull(index) ?: remember { FocusRequester() }
                            }
                        )
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown) {
                                when (event.key) {
                                    Key.Backspace -> {
                                        // Удаляем строку только если она пустая и не единственная
                                        if (items[index].isEmpty() && items.size > minItems) {
                                            val newIndex = (index - 1).coerceAtLeast(0)
                                            items.removeAt(index)
                                            pendingFocusIndex.value = newIndex
                                            onItemsChange(items.toList())
                                            true
                                        } else {
                                            false
                                        }
                                    }
                                    else -> false
                                }
                            } else {
                                false
                            }
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
                            if (index == items.lastIndex) {
                                // В последнем поле - добавляем новую строку
                                if (items[index].isNotBlank()) {
                                    items.add("")
                                    pendingFocusIndex.value = items.lastIndex
                                    onItemsChange(items.toList())
                                }
                            } else {
                                // Переходим к следующему полю
                                focusRequesters.getOrNull(index + 1)?.requestFocus()
                            }
                        }
                    )
                )
            }
        }
    }
}
