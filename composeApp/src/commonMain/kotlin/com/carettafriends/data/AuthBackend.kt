package com.carettafriends.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase Auth behind an interface. Auth stays Supabase permanently (even when data/storage move
 * to Cloudflare). Implemented over the GoTrue REST API via ktor — NOT supabase-kt — so it lives in
 * commonMain without the Kotlin-2.3 ABI trap that blocks the iOS Native target.
 *
 * V1: anonymous sign-in (each device gets a real auth user + stable uid = owner_id, so writes are
 * attributable and RLS-scoped from day one, with zero login friction). V2: "Sign in with Google"
 * via native per-platform OAuth upgrades the same anonymous user to a verified identity.
 */
interface AuthBackend {
    /** Ensure a session exists (anonymous sign-in on first run). Best-effort; returns null offline / if disabled. */
    suspend fun ensureSession(): AuthSession?

    /** Current user id (= owner_id for RLS + attribution), or null if never signed in. */
    fun currentUserId(): String?

    /** A fresh access token (refreshes when near expiry); null if no session / cannot refresh offline. */
    suspend fun accessToken(): String?
}

/** Persisted auth session (okio auth.json). */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,      // epoch seconds
    val userId: String,
    val isAnonymous: Boolean = true,
)

// GoTrue wire formats (subset).
@Serializable
private data class GoTrueSession(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0,
    @SerialName("expires_in") val expiresIn: Long = 3600,
    val user: GoTrueUser? = null,
)

@Serializable
private data class GoTrueUser(
    val id: String = "",
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
)

private const val AUTH_FILE = "caretta_auth.json"
private const val REFRESH_SKEW_SEC = 60L // refresh a minute before expiry

/** GoTrue (Supabase Auth) REST implementation of [AuthBackend]. */
class SupabaseAuth : AuthBackend {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val authBase = SupabaseConfig.URL.trimEnd('/') + "/auth/v1"
    private val http = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    private var session: AuthSession? = loadSession()

    private fun loadSession(): AuthSession? =
        LocalStore.readText(AUTH_FILE)?.let { runCatching { json.decodeFromString<AuthSession>(it) }.getOrNull() }

    private fun saveSession(s: AuthSession) {
        session = s
        runCatching { LocalStore.writeText(AUTH_FILE, json.encodeToString(AuthSession.serializer(), s)) }
    }

    private fun nowSec(): Long = Clock.System.now().epochSeconds

    private fun GoTrueSession.toSession(): AuthSession? {
        val uid = user?.id?.takeIf { it.isNotBlank() } ?: return null
        if (accessToken.isBlank() || refreshToken.isBlank()) return null
        val exp = if (expiresAt > 0) expiresAt else nowSec() + expiresIn
        return AuthSession(accessToken, refreshToken, exp, uid, user.isAnonymous)
    }

    override fun currentUserId(): String? = session?.userId

    override suspend fun ensureSession(): AuthSession? {
        session?.let { if (it.expiresAt - nowSec() > REFRESH_SKEW_SEC) return it }
        // Have a session but it is (nearly) expired → try refresh; else anonymous sign-in.
        session?.let { runCatching { refresh(it.refreshToken) }.getOrNull()?.let { s -> return s } }
        return runCatching { signInAnonymously() }.getOrNull()
    }

    override suspend fun accessToken(): String? = ensureSession()?.accessToken

    private suspend fun signInAnonymously(): AuthSession? {
        val resp: GoTrueSession = http.post("$authBase/signup") {
            header("apikey", SupabaseConfig.ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { }) // empty body = anonymous user (requires anonymous sign-ins enabled)
        }.body()
        return resp.toSession()?.also { saveSession(it) }
    }

    private suspend fun refresh(refreshToken: String): AuthSession? {
        val resp: GoTrueSession = http.post("$authBase/token?grant_type=refresh_token") {
            header("apikey", SupabaseConfig.ANON_KEY)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("refresh_token", refreshToken) })
        }.body()
        return resp.toSession()?.also { saveSession(it) }
    }
}
