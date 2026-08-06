package com.carettafriends.content

import kotlin.random.Random

/**
 * A name for a volunteer who hasn't typed their own yet.
 *
 * Every install used to start as the literal word "Volunteer", so a beach's records were signed by
 * a dozen different people with one identical name — nobody could tell who found what, and the
 * community leaderboard was meaningless. A generated guardian name gives an anonymous volunteer a
 * face from the first photo they send, without asking them to fill in a form first.
 *
 * The vocabulary stays inside the world the app is about — the night beach, the dunes, the sand
 * lilies, the hatchling run to the sea.
 */
private val ADJECTIVES = mapOf(
    "en" to listOf(
        "Moonlit", "Dawn", "Tideline", "Dune", "Seafoam", "Lantern",
        "Coral", "Silver", "Sandlily", "Night", "Saltwind", "Driftwood",
    ),
    "ru" to listOf(
        "Лунный", "Рассветный", "Прибойный", "Дюнный", "Пенный", "Фонарный",
        "Коралловый", "Серебряный", "Лилейный", "Ночной", "Солёный", "Песчаный",
    ),
    // Turkish compounds read "<noun> <noun>+possessive", so the modifier stays a bare noun here.
    "tr" to listOf(
        "Ay", "Şafak", "Dalga", "Kumul", "Köpük", "Fener",
        "Mercan", "Gümüş", "Kum Zambağı", "Gece", "Tuz", "Deniz",
    ),
)

private val NOUNS = mapOf(
    "en" to listOf("Keeper", "Guardian", "Watcher", "Ranger", "Sentinel", "Friend", "Walker", "Herald"),
    "ru" to listOf("Хранитель", "Страж", "Наблюдатель", "Смотритель", "Часовой", "Друг", "Ходок", "Вестник"),
    "tr" to listOf("Bekçisi", "Koruyucusu", "Gözcüsü", "Kollayıcısı", "Nöbetçisi", "Dostu", "Yoldaşı", "Habercisi"),
)

/**
 * A guardian name in [lang] ("en"/"ru"/"tr"), e.g. "Dune Keeper" · "Лунный Страж" · "Kumul Bekçisi".
 * Pass [random] to make it reproducible in a test.
 */
fun randomGuardianName(lang: String, random: Random = Random.Default): String {
    val key = lang.lowercase().take(2).takeIf { it in ADJECTIVES } ?: "en"
    val adjectives = ADJECTIVES.getValue(key)
    val nouns = NOUNS.getValue(key)
    return "${adjectives[random.nextInt(adjectives.size)]} ${nouns[random.nextInt(nouns.size)]}"
}
