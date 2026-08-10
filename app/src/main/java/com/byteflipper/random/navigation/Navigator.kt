package com.byteflipper.random.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember

class Navigator internal constructor(
    private val state: NavigationState
) {
    private val teamSelectionResults = mutableStateMapOf<Teams, List<Long>>()

    fun navigate(key: AppNavKey) {
        if (state.backStack.lastOrNull() != key) {
            state.backStack.add(key)
        }
    }

    fun goBack(): Boolean {
        if (state.backStack.size <= 1) return false
        val top = state.backStack.lastOrNull()
        if (top is PeoplePicker) {
            teamSelectionResults[top.parent] = top.selectedMemberIds
        }
        state.backStack.removeLast()
        return true
    }

    fun replaceRoot(key: AppNavKey) {
        state.backStack.clear()
        state.backStack.add(key)
    }

    fun replaceTop(key: AppNavKey) {
        if (state.backStack.isEmpty()) {
            state.backStack.add(key)
        } else {
            state.backStack[state.backStack.lastIndex] = key
        }
    }

    fun openSavedList(id: Long) {
        while (state.backStack.size > 1 && state.backStack.lastOrNull() != Home) {
            state.backStack.removeLast()
        }
        navigate(ListEditor(id))
    }

    fun updatePeoplePickerSelection(picker: PeoplePicker, selectedMemberIds: List<Long>) {
        val updatedPicker = picker.copy(selectedMemberIds = selectedMemberIds.distinct())
        teamSelectionResults[picker.parent] = updatedPicker.selectedMemberIds
        replaceTop(updatedPicker)
    }

    fun pendingTeamSelection(parent: Teams): List<Long>? = teamSelectionResults[parent]

    fun consumeTeamSelection(parent: Teams): List<Long>? = teamSelectionResults.remove(parent)
}

@Composable
fun rememberNavigator(state: NavigationState): Navigator =
    remember(state) { Navigator(state) }
