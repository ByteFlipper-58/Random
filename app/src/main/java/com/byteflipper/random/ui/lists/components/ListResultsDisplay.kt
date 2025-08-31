package com.byteflipper.random.ui.lists.components

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
import com.byteflipper.random.utils.Constants

private fun getContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun ListResultsDisplay(
    results: List<String>,
    cardColor: Color
) {
    if (results.isNotEmpty()) {
        val adaptivePadding = (Constants.LIST_CARD_SIZE_DP * Constants.ADAPTIVE_PADDING_RATIO).dp
        val adaptiveSpacing = (Constants.LIST_CARD_SIZE_DP * Constants.ADAPTIVE_SPACING_RATIO).dp
        val titleFontSize = androidx.compose.ui.unit.TextUnit(
            Constants.LIST_CARD_SIZE_DP * Constants.ADAPTIVE_TITLE_FONT_RATIO,
            androidx.compose.ui.unit.TextUnitType.Sp
        )
        val itemFontSize = androidx.compose.ui.unit.TextUnit(
            Constants.LIST_CARD_SIZE_DP * Constants.ADAPTIVE_ITEM_FONT_RATIO,
            androidx.compose.ui.unit.TextUnitType.Sp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(adaptivePadding)
        ) {
            val titleTextColor = getContrastColor(cardColor).copy(alpha = 0.8f)
            val itemTextColor = getContrastColor(cardColor)

            Text(
                stringResource(R.string.results),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = titleFontSize,
                    textAlign = TextAlign.Center
                ),
                color = titleTextColor
            )

            Spacer(Modifier.height(adaptiveSpacing))

            results.forEach { item ->
                Text(
                    item,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = itemFontSize,
                        textAlign = TextAlign.Center
                    ),
                    color = itemTextColor
                )
            }
        }
    }
}
