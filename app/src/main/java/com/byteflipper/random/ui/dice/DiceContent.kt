package com.byteflipper.random.ui.dice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.byteflipper.random.R

@Composable
fun DiceCountSelector(
    diceCount: Int,
    onChange: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            listOf(1, 2).forEach { n ->
                val selected = n == diceCount
                FloatingActionButton(
                    onClick = { onChange(n) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(text = n.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            listOf(3, 4, 5).forEach { n ->
                val selected = n == diceCount
                FloatingActionButton(
                    onClick = { onChange(n) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(text = n.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            listOf(6, 7, 8).forEach { n ->
                val selected = n == diceCount
                FloatingActionButton(
                    onClick = { onChange(n) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(text = n.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            listOf(9, 10).forEach { n ->
                val selected = n == diceCount
                FloatingActionButton(
                    onClick = { onChange(n) },
                    modifier = Modifier.height(56.dp)
                ) {
                    Text(text = n.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun DiceContent(
    modifier: Modifier = Modifier,
    diceCount: Int,
    onDiceCountChange: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.dice_count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        DiceCountSelector(diceCount = diceCount, onChange = onDiceCountChange)
    }
}


