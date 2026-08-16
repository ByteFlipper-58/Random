package com.byteflipper.random.domain.di

import android.content.Context
import com.byteflipper.random.domain.ball.data.BallAnswerProvider
import com.byteflipper.random.domain.ball.usecase.AskBallUseCase
import com.byteflipper.random.domain.numbers.usecase.GenerateNumbersUseCase
import com.byteflipper.random.domain.numbers.usecase.ValidateNumberInputsUseCase
import com.byteflipper.random.domain.coin.usecase.TossCoinUseCase
import com.byteflipper.random.domain.dice.usecase.RollDiceUseCase
import com.byteflipper.random.domain.lot.usecase.ValidateLotInputsUseCase
import com.byteflipper.random.domain.lot.usecase.GenerateMarkedIndicesUseCase
import com.byteflipper.random.domain.finger.usecase.GenerateFingerOutcomeUseCase
import com.byteflipper.random.domain.lists.usecase.GenerateListResultsUseCase
import com.byteflipper.random.domain.lists.usecase.SortListResultsUseCase
import com.byteflipper.random.domain.team.usecase.GenerateTeamsUseCase
import com.byteflipper.random.domain.team.usecase.ValidateTeamPresetInputsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Provides
    @Singleton
    fun provideGenerateTeamsUseCase(): GenerateTeamsUseCase = GenerateTeamsUseCase()

    @Provides
    @Singleton
    fun provideValidateTeamPresetInputsUseCase(): ValidateTeamPresetInputsUseCase = ValidateTeamPresetInputsUseCase()

    @Provides
    @Singleton
    fun provideGenerateFingerOutcomeUseCase(): GenerateFingerOutcomeUseCase = GenerateFingerOutcomeUseCase()

    @Provides
    @Singleton
    fun provideBallAnswerProvider(@ApplicationContext context: Context): BallAnswerProvider =
        BallAnswerProvider(context)

    @Provides
    @Singleton
    fun provideAskBallUseCase(): AskBallUseCase = AskBallUseCase()
}
