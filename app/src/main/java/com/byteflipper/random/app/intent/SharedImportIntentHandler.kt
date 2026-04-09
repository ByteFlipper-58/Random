package com.byteflipper.random.app.intent

import android.content.Intent
import android.net.Uri
import com.byteflipper.random.ui.app.AppViewModel
import com.byteflipper.random.ui.app.PendingSharedImport

internal class SharedImportIntentHandler(
    private val appViewModel: AppViewModel
) {
    fun handle(intent: Intent?) {
        intent?.toPendingSharedImport()?.let(appViewModel::submitSharedImport)
    }
}

private fun Intent.toPendingSharedImport(): PendingSharedImport? {
    return when (action) {
        Intent.ACTION_SEND -> {
            val streamUri = getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                ?: firstUriFromClipData()
            val text = getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            streamUri.toPendingSharedImport(
                text = text,
                label = getStringExtra(Intent.EXTRA_TITLE) ?: getStringExtra(Intent.EXTRA_SUBJECT)
            )
        }

        Intent.ACTION_SEND_MULTIPLE -> {
            val streamUri = getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                ?.firstOrNull()
                ?: firstUriFromClipData()
            val text = getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            streamUri.toPendingSharedImport(
                text = text,
                label = getStringExtra(Intent.EXTRA_TITLE) ?: getStringExtra(Intent.EXTRA_SUBJECT)
            )
        }

        Intent.ACTION_VIEW -> {
            (data ?: firstUriFromClipData())?.toPendingSharedImport()
        }

        else -> null
    }
}

private fun Uri?.toPendingSharedImport(
    text: String? = null,
    label: String? = null
): PendingSharedImport? {
    if (this == null && text.isNullOrBlank()) return null
    return PendingSharedImport(
        uri = this,
        text = text,
        label = label
    )
}

private fun Intent.firstUriFromClipData(): Uri? {
    val clipData = clipData ?: return null
    return (0 until clipData.itemCount)
        .asSequence()
        .mapNotNull { index -> clipData.getItemAt(index).uri }
        .firstOrNull()
}
