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
import com.carettafriends.ui.screens.AddNestScreen
import com.carettafriends.ui.screens.BeachDetailScreen
import com.carettafriends.ui.screens.BeachesScreen
import com.carettafriends.ui.screens.CameraScreen
import com.carettafriends.ui.screens.CommunityScreen
import com.carettafriends.ui.screens.ExcavationScreen
import com.carettafriends.ui.screens.LearnScreen
import com.carettafriends.ui.screens.MapScreen
import com.carettafriends.ui.screens.MemberProfileScreen
import com.carettafriends.ui.screens.NestDetailScreen
import com.carettafriends.ui.screens.OnboardingScreen
import com.carettafriends.ui.screens.ProfileScreen
import com.carettafriends.ui.screens.StatsScreen
import com.carettafriends.ui.theme.CarettaTheme
import com.carettafriends.ui.theme.caretta

private fun navItems(s: com.carettafriends.content.AppStrings) = listOf(
    NavItem("map", "🗺️", s.navMap),
    NavItem("beaches", "🏖️", s.navBeaches),
    NavItem("learn", "📖", s.navLearn),
    NavItem("profile", "🐢", s.navProfile),
)

private fun tabKey(s: Screen): String = when (s) {
    is Screen.Map -> "map"
    is Screen.Beaches -> "beaches"
    is Screen.Learn -> "learn"
    is Screen.Profile -> "profile"
    else -> ""   // detail screens: no tab highlighted, but the bar stays visible
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
        val s = com.carettafriends.content.appStrings(state.profile.language)

        PlatformBackHandler(enabled = nav.canGoBack) { nav.back() }

        Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = caretta.sand) {
            Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (current) {
                        is Screen.Map -> MapScreen(
                            state,
                            onAddMarker = { nav.go(Screen.AddNest) },
                            onOpenNest = { nav.go(Screen.NestDetail(it)) },
                            onOpenBeach = { nav.go(Screen.BeachDetail(it)) },
                            onOpenCommunity = { nav.go(Screen.Community(it)) },
                        )
                        is Screen.Beaches -> BeachesScreen(
                            state,
                            onOpenNest = { nav.go(Screen.NestDetail(it)) },
                            onOpenBeach = { nav.go(Screen.BeachDetail(it)) },
                        )
                        is Screen.Learn -> LearnScreen(state, repo)
                        is Screen.Profile -> ProfileScreen(
                            repo, state,
                            onOpenCommunity = { nav.go(Screen.Community(state.community.id)) },
                            onOpenStats = { nav.go(Screen.Stats) },
                            onOpenNest = { nav.go(Screen.NestDetail(it)) },
                            onOpenBeach = { nav.go(Screen.BeachDetail(it)) },
                        )
                        is Screen.Stats -> StatsScreen(state) { nav.back() }
                        is Screen.AddNest -> AddNestScreen(
                            repo, state, { nav.back() }, { nav.go(Screen.Camera) },
                            onNestSaved = { nav.back(); nav.go(Screen.NestDetail(it)) },
                        )
                        is Screen.Camera -> CameraScreen(state.profile.language, { nav.back() }, { nav.back() })
                        is Screen.Community -> CommunityScreen(
                            state.communityOrPrimary(current.communityId), state,
                            onBack = { nav.back() },
                            onOpenMember = { nav.go(Screen.Member(it)) },
                        )
                        is Screen.NestDetail -> {
                            val n = state.nest(current.nestId)
                            if (n != null) {
                                NestDetailScreen(
                                    n, repo, { nav.back() }, { nav.go(Screen.Excavation(n.id)) },
                                    onOpenMember = { nav.go(Screen.Member(it)) },
                                )
                            } else {
                                EmptyHint("🐢", s.nestNotFound)
                            }
                        }
                        is Screen.Excavation -> {
                            val n = state.nest(current.nestId)
                            if (n != null) ExcavationScreen(n, repo, state.profile.language) { nav.back() } else EmptyHint("🐢", s.nestNotFound)
                        }
                        is Screen.BeachDetail -> {
                            val b = state.beach(current.beachId)
                            if (b != null) {
                                BeachDetailScreen(
                                    b, state, { nav.back() }, { nav.go(Screen.NestDetail(it)) },
                                    onOpenMember = { nav.go(Screen.Member(it)) },
                                )
                            } else {
                                EmptyHint("🏖️", s.beachNotFound)
                            }
                        }
                        is Screen.Member -> MemberProfileScreen(
                            state.resolveMember(current.memberKey), state,
                            onBack = { nav.back() },
                            onOpenNest = { nav.go(Screen.NestDetail(it)) },
                            onOpenBeach = { nav.go(Screen.BeachDetail(it)) },
                        )
                    }
                }
                // Always visible — tabs switch, the centre + adds a nest from anywhere.
                BottomBar(
                    current = tabKey(current),
                    items = navItems(s),
                    onSelect = { nav.selectTab(tabFromKey(it)) },
                    onAdd = { nav.go(Screen.AddNest) },
                )
            }
        }
            // First-run intro — shown once until dismissed.
            if (!state.profile.onboarded) {
                OnboardingScreen(state) { repo.setOnboarded() }
            }
        }
    }
}
