package com.byteflipper.random.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppNavKey : NavKey

@Serializable
data object Setup : AppNavKey

@Serializable
data object Home : AppNavKey

@Serializable
data object Numbers : AppNavKey

@Serializable
data class ListEditor(val id: Long? = null) : AppNavKey

@Serializable
data object Dice : AppNavKey

@Serializable
data object Lot : AppNavKey

@Serializable
data object Coin : AppNavKey

@Serializable
data object Wheel : AppNavKey

@Serializable
data object Finger : AppNavKey

@Serializable
data object Ball : AppNavKey

/** The ball's answers editor: which set it follows, and what the answers say. */
@Serializable
data object BallAnswers : AppNavKey

@Serializable
data class Teams(val id: Long? = null) : AppNavKey

@Serializable
data class PeoplePicker(
    val parent: Teams,
    val selectedMemberIds: List<Long>
) : AppNavKey

@Serializable
data object People : AppNavKey

@Serializable
data object Settings : AppNavKey

@Serializable
data object SettingsGeneral : AppNavKey

@Serializable
data object SettingsAppearance : AppNavKey

@Serializable
data object About : AppNavKey
