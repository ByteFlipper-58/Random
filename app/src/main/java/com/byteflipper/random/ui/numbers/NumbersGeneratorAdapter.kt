package com.byteflipper.random.ui.numbers

import com.byteflipper.random.ui.common.createGeneratorExecutionAdapter

internal fun NumbersViewModel.toGeneratorExecutionAdapter() =
    createGeneratorExecutionAdapter(
        host = this,
        beforeSpin = {
            randomizeCardColor()
        },
        onFirstOpen = {
            setOverlayVisible(true)
        },
        onReveal = { targetIsFront ->
            val newNumbers = generate()
            if (targetIsFront) {
                onEvent(NumbersUiEvent.SetFrontValues(newNumbers))
                onEvent(NumbersUiEvent.SetBackValues(emptyList()))
            } else {
                onEvent(NumbersUiEvent.SetBackValues(newNumbers))
                onEvent(NumbersUiEvent.SetFrontValues(emptyList()))
            }
        }
    )
