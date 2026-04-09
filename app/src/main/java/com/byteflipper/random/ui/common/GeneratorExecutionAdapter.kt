package com.byteflipper.random.ui.common

import android.app.Activity
import android.content.Context
import com.byteflipper.random.utils.findActivity

internal interface GeneratorExecutionAdapter {
    val effectiveDelayMs: Int

    fun beforeSpin()

    fun onFirstOpen()

    fun onReveal(targetIsFront: Boolean)

    fun onSpinCompleted(activity: Activity?)
}

internal fun FlipGenerateController.runGeneratorExecution(
    context: Context,
    adapter: GeneratorExecutionAdapter
) {
    runGenerateSpin(
        effectiveDelayMs = adapter.effectiveDelayMs,
        beforeSpin = adapter::beforeSpin,
        onFirstOpen = adapter::onFirstOpen,
        onReveal = adapter::onReveal,
        onSpinCompleted = {
            adapter.onSpinCompleted(context.findActivity())
        }
    )
}

internal fun createGeneratorExecutionAdapter(
    host: GeneratorHostViewModel,
    beforeSpin: GeneratorHostViewModel.() -> Unit = {},
    onFirstOpen: GeneratorHostViewModel.() -> Unit = {},
    onReveal: GeneratorHostViewModel.(Boolean) -> Unit
): GeneratorExecutionAdapter {
    return object : GeneratorExecutionAdapter {
        override val effectiveDelayMs: Int
            get() = host.getEffectiveDelayMs()

        override fun beforeSpin() {
            host.beforeSpin()
        }

        override fun onFirstOpen() {
            host.onFirstOpen()
        }

        override fun onReveal(targetIsFront: Boolean) {
            host.onReveal(targetIsFront)
        }

        override fun onSpinCompleted(activity: Activity?) {
            host.notifyHapticPressIfEnabled()
            activity?.let(host::checkAd)
        }
    }
}
