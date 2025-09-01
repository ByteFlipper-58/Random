package com.byteflipper.random.ui.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
)
@Composable
fun SetupBottomBar(
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    pagerState: PagerState,
    onNextClicked: () -> Unit,
    onFinishClicked: () -> Unit,
    isFinishButtonEnabled: Boolean
) {
    val morphAnimationSpec = tween<Float>(durationMillis = 600, easing = FastOutSlowInEasing)
    val rotationAnimationSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)

    val targetShapeValues = when (pagerState.currentPage % 3) {
        0 -> listOf(50f, 50f, 50f, 50f)
        1 -> listOf(26f, 26f, 26f, 26f)
        else -> listOf(18f, 50f, 18f, 50f)
    }

    val animatedTopStart by animateFloatAsState(targetShapeValues[0], morphAnimationSpec, label = "TopStart")
    val animatedTopEnd by animateFloatAsState(targetShapeValues[1], morphAnimationSpec, label = "TopEnd")
    val animatedBottomStart by animateFloatAsState(targetShapeValues[2], morphAnimationSpec, label = "BottomStart")
    val animatedBottomEnd by animateFloatAsState(targetShapeValues[3], morphAnimationSpec, label = "BottomEnd")

    val animatedRotation by animateFloatAsState(
        targetValue = pagerState.currentPage * 360f,
        animationSpec = rotationAnimationSpec,
        label = "Rotation"
    )

    Surface(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), clip = true),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTR = 36.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTL = 36.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBR = 36.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBL = 36.dp,
            smoothnessAsPercentTR = 60
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "StepTextAnimation"
                ) { targetPage ->
                    if (targetPage == 0) {
                        Text(
                            text = "Let's Go!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(
                            text = "Step ${targetPage} of ${pagerState.pageCount - 1}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == pagerState.pageCount - 1
                val containerColor = if (isLastPage && !isFinishButtonEnabled) {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
                val contentColor = if (isLastPage && !isFinishButtonEnabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                FloatingActionButton (
                    onClick = if (isLastPage) onFinishClicked else onNextClicked,
                    shape = AbsoluteSmoothCornerShape(
                        cornerRadiusTL = animatedTopStart.toInt().dp,
                        smoothnessAsPercentTR = 60,
                        cornerRadiusTR = animatedTopEnd.toInt().dp,
                        smoothnessAsPercentTL = 60,
                        cornerRadiusBL = animatedBottomStart.toInt().dp,
                        smoothnessAsPercentBL = 60,
                        cornerRadiusBR = animatedBottomEnd.toInt().dp,
                        smoothnessAsPercentBR = 60,
                    ),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp),
                    containerColor = containerColor,
                    contentColor = contentColor,
                    modifier = Modifier
                        .rotate(animatedRotation)
                        .padding(end = 0.dp)
                ) {
                    AnimatedContent(
                        modifier = Modifier.rotate(-animatedRotation),
                        targetState = pagerState.currentPage < pagerState.pageCount - 1,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = fadeIn(animationSpec = tween(220, delayMillis = 90)) + scaleIn(
                                    initialScale = 0.9f,
                                    animationSpec = tween(220, delayMillis = 90)
                                ),
                                initialContentExit = fadeOut(animationSpec = tween(90)) + scaleOut(
                                    targetScale = 0.9f,
                                    animationSpec = tween(90)
                                )
                            ).using(SizeTransform(clip = false))
                        },
                        label = "AnimatedFabIcon"
                    ) { isNextPage ->
                        if (isNextPage) {
                            Icon(Icons.Rounded.ArrowForward, contentDescription = "Next")
                        } else {
                            if (isFinishButtonEnabled) {
                                Icon(Icons.Rounded.Check, contentDescription = "Finish")
                            } else {
                                Icon(Icons.Rounded.Close, contentDescription = "Finish")
                            }
                        }
                    }
                }
            }
        }
    }
}