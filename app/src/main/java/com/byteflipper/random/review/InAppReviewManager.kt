package com.byteflipper.random.review

import android.app.Activity
import com.byteflipper.random.BuildConfig
import com.byteflipper.random.data.settings.ReviewPromptState
import com.byteflipper.random.data.settings.SettingsRepository
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppReviewManager @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    companion object {
        private const val MIN_INSTALL_AGE_MS: Long = 7L * 24 * 60 * 60 * 1000
        private const val MIN_SESSION_COUNT: Int = 3
        private const val MIN_SUCCESSFUL_ACTIONS: Int = 12
        private const val MIN_DAYS_BETWEEN_REQUESTS_MS: Long = 90L * 24 * 60 * 60 * 1000
    }

    private val reviewFlowInProgress = AtomicBoolean(false)

    suspend fun onSessionStarted(nowMs: Long = System.currentTimeMillis()) {
        settingsRepository.recordReviewSessionStart(nowMs)
    }

    suspend fun onSuccessfulAction(activity: Activity, nowMs: Long = System.currentTimeMillis()): Boolean {
        val state = settingsRepository.recordReviewSuccessfulAction(nowMs)
        if (!shouldRequestReview(state, nowMs)) {
            return false
        }

        if (!activity.canLaunchReviewFlow()) {
            return false
        }

        if (!reviewFlowInProgress.compareAndSet(false, true)) {
            return false
        }

        settingsRepository.markReviewPromptRequested(
            requestedAtMs = nowMs,
            requestedVersionCode = BuildConfig.VERSION_CODE
        )

        return try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val reviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(activity, reviewInfo).await()
            true
        } catch (_: Exception) {
            true
        } finally {
            reviewFlowInProgress.set(false)
        }
    }

    private fun shouldRequestReview(state: ReviewPromptState, nowMs: Long): Boolean {
        if (state.firstSeenAtMs == 0L) return false
        if (nowMs - state.firstSeenAtMs < MIN_INSTALL_AGE_MS) return false
        if (state.sessionCount < MIN_SESSION_COUNT) return false
        if (state.successfulActionCount < MIN_SUCCESSFUL_ACTIONS) return false
        if (state.lastReviewRequestVersionCode == BuildConfig.VERSION_CODE) return false
        if (state.lastReviewRequestAtMs != 0L && nowMs - state.lastReviewRequestAtMs < MIN_DAYS_BETWEEN_REQUESTS_MS) {
            return false
        }
        return true
    }

    private fun Activity.canLaunchReviewFlow(): Boolean {
        return !isFinishing && !isDestroyed
    }
}
