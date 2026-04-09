package com.byteflipper.random.ui.setup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SetupPagerContent(
    pages: List<SetupPage>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier
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
