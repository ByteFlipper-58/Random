package com.byteflipper.random.ui.about

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.R
import com.byteflipper.random.utils.ChromeCustomTabUtil
import com.byteflipper.random.ui.components.AnimatedActionItem
import com.byteflipper.random.ui.components.AppInfoCard
import com.byteflipper.random.ui.components.ExpandableSection
import com.byteflipper.random.ui.components.InfoCard
import com.byteflipper.random.ui.components.VersionInfoCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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