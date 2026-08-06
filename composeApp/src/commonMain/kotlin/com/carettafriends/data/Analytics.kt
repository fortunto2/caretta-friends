package com.carettafriends.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Which platform the event came from — the schema's `platform` field. */
expect fun platformName(): String

/** The app's marketing version, so a regression can be pinned to a release. */
expect fun appVersion(): String

private const val ANALYTICS_ENDPOINT = "https://analytics.superduperai.co/e"

/** Registered in superduper-analytics `registry/sources.yaml`; shared with the landing page, so a
 *  visit and an app launch are comparable without a join. Ingest refuses unknown ids. */
private const val ANALYTICS_SOURCE = "carettafriends"

/** Where the install-scoped id lives. Not an account, not a device id — see [anonId]. */
private const val ANON_FILE = "caretta_anon.txt"

@Serializable
private data class AnalyticsEvent(
    val source: String,
    val platform: String,
    val name: String,
    val anon: String,
    val ts: Long,
    val version: String,
    val props: Map<String, String>? = null,
    val metrics: Map<String, Double>? = null,
)

@Serializable
private data class AnalyticsBatch(val events: List<AnalyticsEvent>)

/**
 * How many volunteers use this, and whether the landing page turns into installs.
 *
 * Deliberately tiny — a handful of counts, no funnels, no session replay, nothing that follows a
 * person. The identifier is an **install-scoped UUID**: generated on first run, stored on the
 * device, gone when the app is deleted. Not an advertising id, not a device id, not the account —
 * which is what keeps the App Store declaration at "Data Not Linked to You" and keeps the consent
 * banner question closed. If events ever start carrying the auth uid, that label has to change and
 * the whole point is lost.
 *
 * Failures are swallowed on purpose. A nest record is precious; a count is not — analytics must
 * never cost the volunteer a retry, a delay or an error message.
 */
object Analytics {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val http = HttpClient { install(ContentNegotiation) { json(json) } }
    private val lock = Mutex()
    private val queue = mutableListOf<AnalyticsEvent>()

    /** A burst coalesces into one request; anything smaller leaves after [FLUSH_AFTER_MS].
     *  Waiting for a full batch would have lost `app_launched` every time — it is the only event
     *  of a launch, and the buffer dies with the process. */
    private const val FLUSH_AT = 5
    private const val FLUSH_AFTER_MS = 2_000L

    private val anon: String by lazy { anonId() }

    /**
     * Install-scoped anonymous id. Read once from disk, created if absent. Uses the same UUID
     * generator as the records, but this one never leaves the device except in the `anon` field.
     */
    private fun anonId(): String {
        LocalStore.readText(ANON_FILE)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        val fresh = newUuid()
        LocalStore.writeText(ANON_FILE, fresh)
        return fresh
    }

    /** Record something worth counting. Name in `object_verb` past tense, snake_case. */
    fun track(name: String, props: Map<String, String>? = null, metrics: Map<String, Double>? = null) {
        val event = AnalyticsEvent(
            source = ANALYTICS_SOURCE,
            platform = platformName(),
            name = name,
            anon = anon,
            ts = Clock.System.now().toEpochMilliseconds(),
            version = appVersion(),
            props = props?.takeIf { it.isNotEmpty() },
            metrics = metrics?.takeIf { it.isNotEmpty() },
        )
        scope.launch {
            val full = lock.withLock {
                queue += event
                queue.size >= FLUSH_AT
            }
            if (full) {
                flushNow()
            } else {
                kotlinx.coroutines.delay(FLUSH_AFTER_MS)
                flushNow()
            }
        }
    }

    /** Send whatever is buffered — call when the app goes to the background. */
    fun flush() {
        scope.launch { flushNow() }
    }

    private suspend fun flushNow() {
        val batch = lock.withLock { queue.toList().also { queue.clear() } }
        if (batch.isNotEmpty()) send(batch)
    }

    private suspend fun send(events: List<AnalyticsEvent>) {
        runCatching {
            http.post(ANALYTICS_ENDPOINT) {
                contentType(ContentType.Application.Json)
                setBody(AnalyticsBatch(events))
            }
        }
    }
}
