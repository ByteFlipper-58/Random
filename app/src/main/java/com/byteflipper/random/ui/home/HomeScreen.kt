package com.byteflipper.random.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenNumbers: () -> Unit,
    onOpenList: () -> Unit,
    onOpenDice: () -> Unit,
    onOpenLot: () -> Unit,
    onOpenCoin: () -> Unit,
    onAddNumbersPreset: () -> Unit,
    onAddListPreset: () -> Unit,
) {
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
            onAddClick = onAddListPreset
        )
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.Casino, title = "Игральные кости", onClick = onOpenDice)
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.Gavel, title = "Жребий", onClick = onOpenLot)
        Spacer(Modifier.height(12.dp))
        MenuCard(icon = Icons.Outlined.MonetizationOn, title = "Монетка", onClick = onOpenCoin)
        }
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


