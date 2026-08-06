package com.carettafriends.data

import com.carettafriends.domain.PhotoRef
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The photo FILES behind a nest, in shared storage.
 *
 * Nest rows have always synced; the images did not. A [PhotoRef.localUri] is an absolute path on the
 * phone that took the picture, so on anyone else's device it names a file that isn't there — the
 * nest arrived with no photo, which is most of what a volunteer wanted to see.
 *
 * Behind an interface like [CloudBackend]: the files sit in Cloudflare R2 while the records stay in
 * Supabase, and either half can move without touching the app.
 */
interface PhotoStorage {
    /** Upload [bytes] and return the stored object path, or null if it didn't go through. */
    suspend fun upload(path: String, bytes: ByteArray): String?

    /** Fetch a stored object's bytes, or null (offline, deleted, not permitted). */
    suspend fun download(path: String): ByteArray?
}

/** Where the photo files live: Cloudflare R2, behind a Worker that checks the volunteer's Supabase
 *  token (workers/photos). Supabase stays what it is good at — the sync database and identity —
 *  instead of also holding several megabytes per nest. */
private const val PHOTO_ENDPOINT = "https://photos.carettafriends.com/o"

/**
 * R2 through the photos Worker. The bearer token is the same Supabase access token the rest of the
 * app uses, so there is one identity and one login; the Worker verifies it against Supabase's
 * published key and enforces "write only under your own uid".
 */
class R2PhotoStorage(private val auth: AuthBackend) : PhotoStorage {
    private val http = HttpClient()

    override suspend fun upload(path: String, bytes: ByteArray): String? {
        val token = auth.accessToken() ?: return null
        val resp = http.put("$PHOTO_ENDPOINT/$path") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Image.JPEG)
            setBody(bytes)
        }
        return if (resp.status.isSuccess()) path else null
    }

    override suspend fun download(path: String): ByteArray? {
        val token = auth.accessToken() ?: return null
        val resp = http.get("$PHOTO_ENDPOINT/$path") { header("Authorization", "Bearer $token") }
        return if (resp.status.isSuccess()) resp.bodyAsBytes() else null
    }
}

/**
 * Resolves a [PhotoRef] to a file this device can actually show, fetching it once if the photo was
 * taken on someone else's phone.
 *
 * A process-wide object because photos are drawn from list rows deep in the UI, which have no
 * repository in hand; [CarettaRepository] attaches the backend at start-up. Downloads are cached on
 * disk (they never change — a nest photo is a record of a moment) and de-duplicated in flight, so a
 * feed scrolling past the same nest twice fetches once.
 */
object PhotoFiles {
    private var storage: PhotoStorage? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()
    private val resolved = mutableMapOf<String, String>()

    internal fun attach(backend: PhotoStorage) { storage = backend }

    /** Where a downloaded copy lives. Keyed by photo id: stable, and unique across nests. */
    private fun cachePath(id: String) = "${appDirPath()}/photos/$id.jpg"

    /** A readable local path for [photo], downloading the shared copy if this device lacks the file. */
    suspend fun localPath(photo: PhotoRef): String? {
        photo.localUri?.takeIf { LocalStore.existsAbs(it) }?.let { return it }
        resolved[photo.id]?.let { return it }
        val cached = cachePath(photo.id)
        if (LocalStore.existsAbs(cached)) {
            resolved[photo.id] = cached
            return cached
        }
        val remote = photo.remotePath ?: return null
        val backend = storage ?: return null
        return lock.withLock {
            // Another caller may have won the race while we waited.
            resolved[photo.id] ?: run {
                val bytes = runCatching { backend.download(remote) }.getOrNull() ?: return@run null
                if (LocalStore.writeBytesAbs(cached, bytes)) {
                    resolved[photo.id] = cached
                    cached
                } else {
                    null
                }
            }
        }
    }
}
