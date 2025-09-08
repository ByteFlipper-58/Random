package com.byteflipper.random.ui.coin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.byteflipper.random.data.settings.SettingsRepository
import com.byteflipper.random.domain.coin.CoinSide
import com.byteflipper.random.domain.coin.usecase.TossCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class CoinViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val tossCoin: TossCoinUseCase
) : ViewModel() {

    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = runBlocking { settingsRepository.settingsFlow.first() }
    )

    private val _currentSide = MutableStateFlow(CoinSide.HEADS)
    val currentSide: StateFlow<CoinSide> = _currentSide.asStateFlow()

    fun toss(): CoinSide {
        val result = tossCoin()
        _currentSide.value = result
        return result
    }
}


