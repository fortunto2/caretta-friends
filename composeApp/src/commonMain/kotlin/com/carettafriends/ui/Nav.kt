package com.carettafriends.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface Screen {
    data object Map : Screen
    data object Beaches : Screen
    data object Learn : Screen
    data object Profile : Screen
    data object AddNest : Screen
    data object Camera : Screen
    data class Community(val communityId: String) : Screen
    data class NestDetail(val nestId: String) : Screen
    data class Excavation(val nestId: String) : Screen
    data class BeachDetail(val beachId: String) : Screen
}

fun Screen.isTab(): Boolean =
    this is Screen.Map || this is Screen.Beaches || this is Screen.Learn || this is Screen.Profile

class Navigator {
    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Map))
        private set

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun go(s: Screen) { stack = stack + s }
    fun selectTab(s: Screen) { stack = listOf(s) }
    fun back() { if (stack.size > 1) stack = stack.dropLast(1) }
}
