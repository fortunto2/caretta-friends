package com.carettafriends.content

import com.carettafriends.resources.Res

/** A short string localized into the app's three languages (used for titles/facts/labels). */
data class L10n(val ru: String, val tr: String, val en: String) {
    fun get(lang: String): String = when (lang) {
        "tr" -> tr
        "en" -> en
        else -> ru
    }
}

/** Guide article metadata; the body is markdown loaded from composeResources per language. */
data class GuideMeta(val id: String, val emoji: String, val title: L10n)

/**
 * Guide + facts. Article bodies are baked-in MARKDOWN files (FaceAlarm pattern), one per language:
 * composeResources/files/content/{en,ru,tr}/{id}.md — rendered with the multiplatform markdown renderer.
 * Content sourced/adapted from carettafriends.com + Türkiye nesting-beach regulations.
 */
object GuideContent {
    const val siteUrl = "https://carettafriends.com"

    /** (code, short label) for the language switcher. English is the default. */
    val languages = listOf("en" to "EN", "ru" to "RU", "tr" to "TR")

    val facts: List<L10n> = listOf(
        L10n(
            ru = "Тёплый песок = больше самок: выше ~29 °C гнездо смещается к самкам.",
            tr = "Sıcak kum = daha çok dişi: ~29 °C üstünde yuva dişilere kayar.",
            en = "Warmer sand makes more females — above ~29 °C a nest skews female.",
        ),
        L10n(
            ru = "Черепашата выходят ночью и находят море по светлому горизонту над водой.",
            tr = "Yavrular gece çıkar ve denizi, su üzerindeki aydınlık ufuktan bulur.",
            en = "Hatchlings emerge at night and find the sea by the bright horizon over the water.",
        ),
        L10n(
            ru = "Искусственный свет дезориентирует черепашат — в сезон берег должен быть тёмным.",
            tr = "Yapay ışık yavruları şaşırtır — sezonda kıyı karanlık olmalı.",
            en = "Artificial light disorients hatchlings — keep the shore dark in nesting season.",
        ),
    )

    val articles: List<GuideMeta> = listOf(
        GuideMeta("found", "🥚", L10n("Нашёл гнездо? Что делать", "Yuva mı buldun? Ne yapmalı", "Found a nest? What to do")),
        GuideMeta("lights", "🔦", L10n("Без света и вспышки ночью", "Gece ışık ve flaş yok", "No lights or flash at night")),
        GuideMeta("beach-rules", "⛔", L10n("Правила пляжа и запреты", "Plaj kuralları ve yasaklar", "Beach rules & what's banned")),
        GuideMeta("hatchlings", "🐣", L10n("Как помочь черепашатам", "Yavrulara nasıl yardım edilir", "Helping hatchlings out")),
        GuideMeta("protection", "🛡️", L10n("Как мы защищаем гнёзда", "Yuvaları nasıl koruruz", "How we protect nests")),
    )

    /** Loads the markdown body for [id] in [lang] (falls back to EN), dropping the leading H1 title. */
    suspend fun loadArticle(id: String, lang: String): String {
        val raw = try {
            Res.readBytes("files/content/$lang/$id.md").decodeToString()
        } catch (e: Throwable) {
            Res.readBytes("files/content/en/$id.md").decodeToString()
        }
        val t = raw.trimStart()
        return if (t.startsWith("#")) t.substringAfter('\n').trimStart() else t
    }
}
