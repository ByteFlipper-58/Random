package com.byteflipper.random.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.byteflipper.random.R
import com.byteflipper.random.ui.theme.ExpTitleTypography
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupScreen(
    setupViewModel: SetupViewModel = hiltViewModel(),
    onSetupComplete: () -> Unit
) {
    val pages = remember {
        buildList {
            add(SetupPage.Welcome)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(SetupPage.NotificationsPermission)
            }
            add(SetupPage.Finish)
        }
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            SetupBottomBar(
                pagerState = pagerState,
                onNextClicked = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onFinishClicked = {
                    setupViewModel.setSetupComplete()
                    onSetupComplete()
                }
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { pageIndex ->
            val page = pages[pageIndex]
            val pageOffset = pagerState.currentPageOffsetFraction

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                        translationX = size.width * pageOffset
                    },
                contentAlignment = Alignment.Center
            ) {
                when (page) {
                    SetupPage.Welcome -> WelcomePage()
                    SetupPage.NotificationsPermission -> NotificationsPermissionPage()
                    SetupPage.Finish -> FinishPage()
                }
            }
        }
    }
}

sealed class SetupPage {
    object Welcome : SetupPage()
    object NotificationsPermission : SetupPage()
    object Finish : SetupPage()
}

@Composable
fun WelcomePage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 12.dp),
            text = stringResource(R.string.setup_welcome_title),
            style = ExpTitleTypography.displayLarge.copy(
                fontSize = 42.sp,
                lineHeight = 1.1.em
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(20.dp))
        ){
            MaterialYouVectorDrawable(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(R.drawable.logo)
            )
            SineWaveLine(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(32.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.surface,
                alpha = 0.95f,
                strokeWidth = 16.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(22.dp)
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp)
            ){

            }
            SineWaveLine(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(32.dp)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 4.dp),
                animate = true,
                color = MaterialTheme.colorScheme.primary,
                alpha = 0.95f,
                strokeWidth = 4.dp,
                amplitude = 4.dp,
                waves = 7.6f,
                phase = 0f
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_welcome_description), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FinishPage() {
    val finishIcons = persistentListOf(
        R.drawable.check_circle_24px,
        R.drawable.favorite_24px,
        R.drawable.celebration_24px,
        R.drawable.star_shine_24px,
        R.drawable.explosion_24px
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.setup_finish_title), style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionIconCollage(
            modifier = Modifier.height(230.dp),
            icons = finishIcons
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.setup_finish_description), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun NotificationsPermissionPage() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
    }

    val notificationIcons = persistentListOf(
        R.drawable.circle_notifications_24px,
        R.drawable.campaign_24px,
        R.drawable.bolt_24px,
        R.drawable.news_24px,
        R.drawable.release_alert_24px
    )

    PermissionPageLayout(
        title = stringResource(R.string.setup_notifications_title),
        granted = isGranted,
        description = stringResource(R.string.setup_notifications_description),
        buttonText = if (isGranted) {
            stringResource(R.string.setup_permission_granted)
        } else {
            stringResource(R.string.setup_enable_notifications)
        },
        icons = notificationIcons,
        onGrantClicked = {
            if (!isGranted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.setup_notifications_optional),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PermissionPageLayout(
    title: String,
    granted: Boolean = false,
    description: String,
    buttonText: String,
    icons: ImmutableList<Int>,
    onGrantClicked: () -> Unit,
    content: @Composable () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        PermissionIconCollage(icons = icons)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onGrantClicked,
            enabled = !granted,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            Text(text = buttonText)
        }
        content()
    }
}
