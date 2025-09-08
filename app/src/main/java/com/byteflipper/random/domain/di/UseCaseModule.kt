package com.byteflipper.random.domain.di

import com.byteflipper.random.domain.numbers.usecase.GenerateNumbersUseCase
import com.byteflipper.random.domain.numbers.usecase.ValidateNumberInputsUseCase
import com.byteflipper.random.domain.coin.usecase.TossCoinUseCase
import com.byteflipper.random.domain.dice.usecase.RollDiceUseCase
import com.byteflipper.random.domain.lot.usecase.ValidateLotInputsUseCase
import com.byteflipper.random.domain.lot.usecase.GenerateMarkedIndicesUseCase
import com.byteflipper.random.domain.lists.usecase.GenerateListResultsUseCase
import com.byteflipper.random.domain.lists.usecase.SortListResultsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideValidateNumberInputsUseCase(): ValidateNumberInputsUseCase = ValidateNumberInputsUseCase()

    @Provides
    @Singleton
    fun provideGenerateNumbersUseCase(): GenerateNumbersUseCase = GenerateNumbersUseCase()

    @Provides
    @Singleton
    fun provideTossCoinUseCase(): TossCoinUseCase = TossCoinUseCase()

    @Provides
    @Singleton
    fun provideRollDiceUseCase(): RollDiceUseCase = RollDiceUseCase()

    @Provides
    @Singleton
    fun provideValidateLotInputsUseCase(): ValidateLotInputsUseCase = ValidateLotInputsUseCase()

    @Provides
    @Singleton
    fun provideGenerateMarkedIndicesUseCase(): GenerateMarkedIndicesUseCase = GenerateMarkedIndicesUseCase()

    @Provides
    @Singleton
    fun provideGenerateListResultsUseCase(): GenerateListResultsUseCase = GenerateListResultsUseCase()

    @Provides
    @Singleton
    fun provideSortListResultsUseCase(): SortListResultsUseCase = SortListResultsUseCase()
}


