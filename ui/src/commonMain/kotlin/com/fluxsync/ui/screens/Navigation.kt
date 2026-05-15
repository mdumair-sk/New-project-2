package com.fluxsync.ui.screens

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class Screen {
    data object Discovery : Screen()
    data class Pairing(val peerId: String) : Screen()
    data class ActiveTransfer(val transferId: String) : Screen()
    data object Settings : Screen()
    data object DevConsole : Screen()
}

class AppNavigator {
    private val backstack = mutableListOf<Screen>(Screen.Discovery)
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Discovery)
    val currentScreen: StateFlow<Screen> = _currentScreen

    fun navigate(screen: Screen) {
        backstack.add(screen)
        _currentScreen.value = screen
    }

    fun back() {
        if (backstack.size > 1) {
            backstack.removeAt(backstack.lastIndex)
        }
        _currentScreen.value = backstack.last()
    }
}
