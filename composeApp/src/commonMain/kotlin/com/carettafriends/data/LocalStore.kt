package com.carettafriends.data

import okio.ByteString.Companion.toByteString
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

    /** Erase every photo this device holds: the download cache and our own captures. Their EXIF
     *  carries protected-nest coordinates, so "delete my account" has to take them with it. */
    fun deletePhotos() {
        try {
            val dir = appDirPath().toPath()
            val cache = dir / "photos"
            if (fs.exists(cache)) fs.deleteRecursively(cache)
            fs.list(dir).filter { it.name.startsWith("nest_") }.forEach { runCatching { fs.delete(it) } }
        } catch (e: Throwable) {
            // best-effort; ignore
        }
    }

    /** Does a file exist at this ABSOLUTE path? */
    fun existsAbs(absPath: String): Boolean = try {
        fs.exists(absPath.toPath())
    } catch (e: Throwable) {
        false
    }

    /** Write bytes to an ABSOLUTE path, creating parent directories. False if it couldn't be written. */
    fun writeBytesAbs(absPath: String, bytes: ByteArray): Boolean = try {
        val p = absPath.toPath()
        p.parent?.let { fs.createDirectories(it) }
        fs.write(p) { write(bytes) }
        true
    } catch (e: Throwable) {
        false
    }

    /** Read raw bytes at an ABSOLUTE path (e.g. a camera photo file). Null if missing/unreadable. */
    fun readBytesAbs(absPath: String): ByteArray? = try {
        val p = absPath.toPath()
        if (fs.exists(p)) fs.read(p) { readByteArray() } else null
    } catch (e: Throwable) {
        null
    }
}

/**
 * Content identity of an image file — `"md5:<hex>"`, or null if it can't be read.
 *
 * Used to notice that the volunteer just picked the SAME photo again (which otherwise silently
 * created a second nest). Valid only for files copied byte-for-byte from the original: the gallery
 * pickers do that, while the iOS camera re-encodes with a burned-in overlay and passes the source
 * asset's id instead.
 */
fun photoFingerprint(absPath: String): String? =
    LocalStore.readBytesAbs(absPath)?.let { "md5:" + it.toByteString().md5().hex() }
