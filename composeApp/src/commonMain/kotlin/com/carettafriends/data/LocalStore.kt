package com.carettafriends.data

import okio.FileSystem
import okio.Path.Companion.toPath

/** Platform app-writable directory (Android filesDir / iOS Documents). */
expect fun appDirPath(): String

/**
 * Client-generated stable UUID for every user-created row.
 * A stable PK created offline never needs a server round-trip — the #1 prerequisite for
 * FK-safe offline sync and collision-free upserts (replaces the old resets-to-1000 counter).
 */
expect fun newUuid(): String

/** Simple offline-first key→text store on disk (used to persist the app state as JSON). */
object LocalStore {
    private val fs: FileSystem = FileSystem.SYSTEM

    fun readText(name: String): String? = try {
        val p = appDirPath().toPath() / name
        if (fs.exists(p)) fs.read(p) { readUtf8() } else null
    } catch (e: Throwable) {
        null
    }

    fun writeText(name: String, text: String) {
        try {
            val p = appDirPath().toPath() / name
            fs.write(p) { writeUtf8(text) }
        } catch (e: Throwable) {
            // best-effort; ignore write failures
        }
    }

    /** Remove a stored file. Used by account deletion, which must leave nothing behind on device. */
    fun delete(name: String) {
        try {
            val p = appDirPath().toPath() / name
            if (fs.exists(p)) fs.delete(p)
        } catch (e: Throwable) {
            // best-effort; ignore delete failures
        }
    }

    /** Read raw bytes at an ABSOLUTE path (e.g. a camera photo file). Null if missing/unreadable. */
    fun readBytesAbs(absPath: String): ByteArray? = try {
        val p = absPath.toPath()
        if (fs.exists(p)) fs.read(p) { readByteArray() } else null
    } catch (e: Throwable) {
        null
    }
}
