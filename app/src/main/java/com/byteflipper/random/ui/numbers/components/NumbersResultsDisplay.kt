package com.byteflipper.random.ui.numbers.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.byteflipper.random.R

private fun getContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun NumbersResultsDisplay(
    results: List<Int>,
    cardColor: Color
) {
    if (results.isNotEmpty()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            val textColor = getContrastColor(cardColor)

            Text(
                stringResource(R.string.results),
                style = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center
                ),
                color = textColor.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(16.dp))

            // Отображаем числа в сетке или списке
            val numbersText = results.joinToString(", ")

            Text(
                numbersText,
                style = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.Center
                ),
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
