package com.byteflipper.random.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigatorTest {
    @Test
    fun navigate_doesNotDuplicateTopDestination() {
        val state = stateOf(Home)
        val navigator = Navigator(state)

        navigator.navigate(Numbers)
        navigator.navigate(Numbers)

        assertEquals(listOf(Home, Numbers), state.backStack)
    }

    @Test
    fun replaceRoot_removesSetupFromHistory() {
        val state = stateOf(Setup)
        val navigator = Navigator(state)

        navigator.replaceRoot(Home)

        assertEquals(listOf(Home), state.backStack)
        assertFalse(navigator.goBack())
    }

    @Test
    fun openSavedList_replacesEditorHistoryAboveHome() {
        val state = stateOf(Home, ListEditor(1L))
        val navigator = Navigator(state)

        navigator.openSavedList(2L)

        assertEquals(listOf(Home, ListEditor(2L)), state.backStack)
    }

    @Test
    fun peoplePicker_updatesObservableResultAndReturnsItOnBack() {
        val teams = Teams(7L)
        val picker = PeoplePicker(parent = teams, selectedMemberIds = listOf(1L))
        val state = stateOf(Home, teams, picker)
        val navigator = Navigator(state)

        navigator.updatePeoplePickerSelection(picker, listOf(1L, 2L, 2L))

        assertEquals(listOf(1L, 2L), navigator.pendingTeamSelection(teams))
        assertTrue(navigator.goBack())
        assertEquals(listOf(Home, teams), state.backStack)
        assertEquals(listOf(1L, 2L), navigator.consumeTeamSelection(teams))
        assertNull(navigator.consumeTeamSelection(teams))
    }

    private fun stateOf(vararg keys: AppNavKey): NavigationState =
        NavigationState(keys.mapTo(mutableListOf<NavKey>()) { it })
}
