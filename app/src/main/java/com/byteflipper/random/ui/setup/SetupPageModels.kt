package com.byteflipper.random.ui.setup

sealed class SetupPage {
    data object Welcome : SetupPage()
    data object NotificationsPermission : SetupPage()
    data object Finish : SetupPage()
}
