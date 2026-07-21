package com.carettafriends

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import com.carettafriends.data.CarettaRepository
import com.carettafriends.data.PendingPhoto
import com.carettafriends.ui.components.EmptyHint
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
import platform.UIKit.UIViewController

/**
 * iOS hybrid entry: SwiftUI owns the chrome (native iOS 26 Liquid Glass tab bar + navigation),
 * each content screen is the SHARED Compose UI, hosted per-screen and reading one shared repo.
 */
object SharedRepo {
    val repo = CarettaRepository()
}

/** A nest point for the native iOS MapLibre map. */
data class IosMapPoint(val id: String, val lat: Double, val lng: Double, val title: String, val status: String)

/** Nest points for the native SwiftUI MapLibre map (read from the shared repo). */
fun mapPoints(): List<IosMapPoint> {
    val s = SharedRepo.repo.state.value
    return s.nests.map { IosMapPoint(it.id, it.point.lat, it.point.lng, it.code, it.status.name) }
}

/** Called from Swift after the native camera captures/picks a photo (with optional GPS). */
fun setPendingPhoto(path: String, lat: Double, lng: Double, hasLocation: Boolean) {
    SharedRepo.repo.pendingPhoto = PendingPhoto(
        path = path,
        lat = if (hasLocation) lat else null,
        lng = if (hasLocation) lng else null,
    )
}

private fun host(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController { CarettaTheme { content() } }

fun MapVC(onOpenNest: (String) -> Unit, onAdd: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    MapScreen(state, onAdd, onOpenNest)
}

fun BeachesVC(onOpenNest: (String) -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    BeachesScreen(state, onOpenNest)
}

fun LearnVC(): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    LearnScreen(state, SharedRepo.repo)
}

fun ProfileVC(onOpenCommunity: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    ProfileScreen(SharedRepo.repo, state, onOpenCommunity)
}

fun AddNestVC(onDone: () -> Unit, onCamera: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    AddNestScreen(SharedRepo.repo, state, onDone, onCamera)
}

fun NestDetailVC(nestId: String, onBack: () -> Unit, onExcavate: (String) -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    val n = state.nest(nestId)
    if (n != null) {
        NestDetailScreen(n, SharedRepo.repo, onBack) { onExcavate(n.id) }
    } else {
        EmptyHint("🐢", "Nest not found")
    }
}

fun ExcavationVC(nestId: String, onBack: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    val n = state.nest(nestId)
    if (n != null) ExcavationScreen(n, SharedRepo.repo, onBack) else EmptyHint("🐢", "Nest not found")
}

fun CameraVC(onBack: () -> Unit, onCaptured: () -> Unit): UIViewController = host {
    CameraScreen(onBack, onCaptured)
}

fun CommunityVC(onBack: () -> Unit): UIViewController = host {
    val state by SharedRepo.repo.state.collectAsState()
    CommunityScreen(state, onBack)
}
