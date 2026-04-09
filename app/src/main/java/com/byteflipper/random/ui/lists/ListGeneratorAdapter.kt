package com.byteflipper.random.ui.lists

import com.byteflipper.random.ui.common.createGeneratorExecutionAdapter

internal fun ListViewModel.toGeneratorExecutionAdapter() =
    createGeneratorExecutionAdapter(
        host = this,
        onFirstOpen = {
            randomizeCardColor()
            setOverlayVisible(true)
        },
        onReveal = { _ ->
            generateAndUpdateResults()
        }
    )
