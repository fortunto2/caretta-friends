package com.carettafriends

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.carettafriends.data.CarettaRepository
import com.carettafriends.ui.Navigator
import com.carettafriends.ui.PlatformBackHandler
import com.carettafriends.ui.Screen
import com.carettafriends.ui.components.BottomBar
import com.carettafriends.ui.components.EmptyHint
import com.carettafriends.ui.components.NavItem
import com.carettafriends.ui.isTab
import com.carettafriends.ui.screens.AddNestScreen
import com.carettafriends.ui.screens.BeachesScreen
import com.carettafriends.ui.screens.CameraScreen
import com.carettafriends.ui.screens.CommunityScreen
import com.carettafriends.ui.screens.ExcavationScreen
import com.carettafriends.ui.screens.LearnScreen
import com.carettafriends.ui.screens.MapScreen
import com.carettafriends.ui.screens.NestDetailScreen
import com.carettafriends.ui.screens.ProfileScreen
import com.carettafriends.ui.theme.CarettaTheme
import com.carettafriends.ui.theme.caretta

private val navItems = listOf(
    NavItem("map", "🗺️", "Map"),
    NavItem("beaches", "🏖️", "Beaches"),
    NavItem("learn", "📖", "Learn"),
    NavItem("profile", "🐢", "Profile"),
)

private fun tabKey(s: Screen): String = when (s) {
    is Screen.Beaches -> "beaches"
    is Screen.Learn -> "learn"
    is Screen.Profile -> "profile"
    else -> "map"
}

private fun tabFromKey(k: String): Screen = when (k) {
    "beaches" -> Screen.Beaches
    "learn" -> Screen.Learn
    "profile" -> Screen.Profile
    else -> Screen.Map
}

@Composable
fun CarettaApp() {
    CarettaTheme {
        val repo = remember { CarettaRepository() }
        val nav = remember { Navigator() }
        val state by repo.state.collectAsState()
        val current = nav.current

        PlatformBackHandler(enabled = nav.canGoBack) { nav.back() }

        Surface(modifier = Modifier.fillMaxSize(), color = caretta.sand) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (current) {
                        is Screen.Map -> MapScreen(state, { nav.go(Screen.AddNest) }, { nav.go(Screen.NestDetail(it)) })
                        is Screen.Beaches -> BeachesScreen(state) { nav.go(Screen.NestDetail(it)) }
                        is Screen.Learn -> LearnScreen(state, repo)
                        is Screen.Profile -> ProfileScreen(state) { nav.go(Screen.Community) }
                        is Screen.AddNest -> AddNestScreen(repo, state, { nav.back() }, { nav.go(Screen.Camera) })
                        is Screen.Camera -> CameraScreen({ nav.back() }, { nav.back() })
                        is Screen.Community -> CommunityScreen(state) { nav.back() }
                        is Screen.NestDetail -> {
                            val n = state.nest(current.nestId)
                            if (n != null) {
                                NestDetailScreen(n, repo, { nav.back() }, { nav.go(Screen.Excavation(n.id)) })
                            } else {
                                EmptyHint("🐢", "Nest not found")
                            }
                        }
                        is Screen.Excavation -> {
                            val n = state.nest(current.nestId)
                            if (n != null) ExcavationScreen(n, repo) { nav.back() } else EmptyHint("🐢", "Nest not found")
                        }
                    }
                }
                if (current.isTab()) {
                    BottomBar(current = tabKey(current), items = navItems, onSelect = { nav.selectTab(tabFromKey(it)) })
                }
            }
        }
    }
}
