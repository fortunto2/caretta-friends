package com.carettafriends.data

/**
 * Supabase project config. The anon/publishable key is a PUBLIC client key (safe to embed —
 * access is governed by Row Level Security). The DB password is a secret and lives only in .env.
 */
object SupabaseConfig {
    const val URL = "https://blameszwjkjksgttvgqi.supabase.co"
    const val ANON_KEY = "sb_publishable_wRb_-uNag1MjlxcKIQyD2g_0WQilvcv"
}
