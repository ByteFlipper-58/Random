package com.byteflipper.random.ui.about

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.byteflipper.random.utils.ChromeCustomTabUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()

    AboutScaffold(onBack) { innerPadding ->
        AboutContent(modifier = modifier.padding(innerPadding)) { url ->
            ChromeCustomTabUtil.openUrl(
                context = context,
                url = url,
                primaryColor = primaryColorArgb
            )
        }
    }
}