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
    val scrollState = rememberScrollState()
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_app)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppInfoCard(
                appName = stringResource(R.string.app_name),
                developerName = "ByteFlipper",
                appIcon = painterResource(id = R.drawable.logo)
            )

            ExpandableSection(
                title = stringResource(R.string.support_feedback_category_title),
                icon = painterResource(id = R.drawable.bug_report_24px),
                expandedContentDescription = stringResource(R.string.expandable_section_expand),
                collapsedContentDescription = stringResource(R.string.expandable_section_collapse)
            ) {
                Column {
                    /*AnimatedActionItem(
                        title = stringResource(R.string.bug_report_title),
                        subtitle = stringResource(R.string.bug_report_subtitle),
                        icon = painterResource(id = R.drawable.bug_report_24px),
                        onClick = { } //TODO
                    )*/

                    AnimatedActionItem(
                        title = stringResource(R.string.rate_the_app_title),
                        subtitle = stringResource(R.string.rate_the_app_subtitle),
                        icon = painterResource(id = R.drawable.rate_review_24px),
                        onClick = { } //TODO
                    )

                    AnimatedActionItem(
                        title = stringResource(R.string.other_apps_title),
                        subtitle = stringResource(R.string.other_apps_subtitle),
                        icon = painterResource(id = R.drawable.apps_24px),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = context.getString(R.string.app_name),
                                primaryColor = primaryColorArgb
                            )
                        },
                        showDivider = false
                    )
                }
            }

            ExpandableSection(
                title = stringResource(R.string.connect_with_us_category_title),
                icon = painterResource(id = R.drawable.web_24px),
                expandedContentDescription = stringResource(R.string.expandable_section_expand),
                collapsedContentDescription = stringResource(R.string.expandable_section_collapse)
            ) {
                Column {
                    AnimatedActionItem(
                        title = stringResource(R.string.website_title),
                        subtitle = stringResource(R.string.website_subtitle),
                        icon = painterResource(id = R.drawable.web_24px),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = "https://byteflipper.web.app",
                                primaryColor = primaryColorArgb
                            )
                        }
                    )

                    AnimatedActionItem(
                        title = stringResource(R.string.vk_title),
                        subtitle = stringResource(R.string.vk_subtitle),
                        icon = painterResource(id = R.drawable.vk_24),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = "https://vk.com/byteflipper",
                                primaryColor = primaryColorArgb
                            )
                        }
                    )

                    AnimatedActionItem(
                        title = stringResource(R.string.telegram_title),
                        subtitle = stringResource(R.string.telegram_subtitle),
                        icon = painterResource(id = R.drawable.telegram_24),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = "https://t.me/byteflipper",
                                primaryColor = primaryColorArgb
                            )
                        },
                        showDivider = false
                    )
                }
            }

            ExpandableSection(
                title = stringResource(R.string.development_category_title),
                icon = painterResource(id = R.drawable.code_24px),
                expandedContentDescription = stringResource(R.string.expandable_section_expand),
                collapsedContentDescription = stringResource(R.string.expandable_section_collapse)
            ) {
                Column {
                    AnimatedActionItem(
                        title = stringResource(R.string.github_title),
                        subtitle = stringResource(R.string.github_subtitle),
                        icon = painterResource(id = R.drawable.github_24),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = "https://github.com/ByteFlipper-58",
                                primaryColor = primaryColorArgb
                            )
                        }
                    )

                    AnimatedActionItem(
                        title = stringResource(R.string.source_code_title),
                        subtitle = stringResource(R.string.source_code_subtitle),
                        icon = painterResource(id = R.drawable.code_24px),
                        onClick = {
                            ChromeCustomTabUtil.openUrl(
                                context = context,
                                url = "https://github.com/ByteFlipper-58/FFSensitivities2",
                                primaryColor = primaryColorArgb
                            )
                        },
                        showDivider = false
                    )
                }
            }

            VersionInfoCard(
                versionTitle = stringResource(R.string.version),
                versionInfo = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                icon = painterResource(id = R.drawable.update_24px)
            )

            InfoCard(
                text = stringResource(R.string.made_with_love)
            )
        }
    }
}