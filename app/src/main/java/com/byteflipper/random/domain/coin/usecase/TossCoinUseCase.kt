package com.byteflipper.random.domain.coin.usecase

import com.byteflipper.random.domain.coin.CoinSide
import kotlin.random.Random

class TossCoinUseCase {
    operator fun invoke(): CoinSide {
        return if (Random.nextBoolean()) CoinSide.HEADS else CoinSide.TAILS
    }
}


