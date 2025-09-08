package com.byteflipper.random.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.R
import com.byteflipper.random.ui.components.AnimatedActionItem
import com.byteflipper.random.ui.components.AppInfoCard
import com.byteflipper.random.ui.components.ExpandableSection
import com.byteflipper.random.ui.components.InfoCard
import com.byteflipper.random.ui.components.VersionInfoCard

@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    onOpenUrl: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val primaryColorArgb = MaterialTheme.colorScheme.primary.toArgb()
    val otherAppsUrl = stringResource(R.string.app_name)

    Column(
        modifier = modifier
            .fillMaxSize()
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
                AnimatedActionItem(
                    title = stringResource(R.string.rate_the_app_title),
                    subtitle = stringResource(R.string.rate_the_app_subtitle),
                    icon = painterResource(R.drawable.rate_review_24px),
                    onClick = { onOpenUrl("https://play.google.com/store/apps/details?id=com.byteflipper.random") }
                )

                AnimatedActionItem(
                    title = stringResource(R.string.other_apps_title),
                    subtitle = stringResource(R.string.other_apps_subtitle),
                    icon = painterResource(R.drawable.apps_24px),
                    onClick = { onOpenUrl(otherAppsUrl) },
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
                    icon = painterResource(R.drawable.web_24px),
                    onClick = { onOpenUrl("https://byteflipper.web.app") }
                )

                AnimatedActionItem(
                    title = stringResource(R.string.vk_title),
                    subtitle = stringResource(R.string.vk_subtitle),
                    icon = painterResource(R.drawable.vk_24),
                    onClick = { onOpenUrl("https://vk.com/byteflipper") }
                )

                AnimatedActionItem(
                    title = stringResource(R.string.telegram_title),
                    subtitle = stringResource(R.string.telegram_subtitle),
                    icon = painterResource(R.drawable.telegram_24),
                    onClick = { onOpenUrl("https://t.me/byteflipper") },
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
                    icon = painterResource(R.drawable.github_24),
                    onClick = { onOpenUrl("https://github.com/ByteFlipper-58") }
                )

                AnimatedActionItem(
                    title = stringResource(R.string.source_code_title),
                    subtitle = stringResource(R.string.source_code_subtitle),
                    icon = painterResource(R.drawable.code_24px),
                    onClick = { onOpenUrl("https://github.com/ByteFlipper-58/random") },
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


