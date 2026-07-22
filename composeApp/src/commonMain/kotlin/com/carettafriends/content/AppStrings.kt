package com.carettafriends.content

/**
 * UI string catalog for the whole interface (EN/RU/TR). Screens read the current bundle via
 * [appStrings]`(state.profile.language)` — the language is set in Profile → Language and persists.
 * Guide article bodies stay in [GuideContent]; this is the app chrome.
 */
data class AppStrings(
    // bottom navigation
    val navMap: String,
    val navBeaches: String,
    val navLearn: String,
    val navProfile: String,
    // common actions
    val save: String,
    val cancel: String,
    val close: String,
    val done: String,
    val change: String,
    // map filters + chrome
    val filterAll: String,
    val filterNests: String,
    val filterHatching: String,
    val filterTrash: String,
    val startPatrol: String,
    val stopPatrol: String,
    val noPatrolYet: String,
    // beaches
    val beaches: String,
    val nearYou: String,
    val allBeaches: String,
    val showBeaches: String,
    val nearMeAuto: String,
    val findingBeaches: String,
    // profile
    val profile: String,
    val saveAccount: String,
    val saveAccountSub: String,
    val signedIn: String,
    val hatchlingsReached: String,
    val dayStreak: String,
    val walked: String,
    val patrols: String,
    val community: String,
    val myBeach: String,
    val freeVolunteer: String,
    val patrolClosest: String,
    val yourHomeBeach: String,
    val homeBeach: String,
    val patrolled: String,
    val badges: String,
    val noBadges: String,
    val myPhotos: String,
    val trends: String,
    val language: String,
    val yourName: String,
    val displayName: String,
    // email auth
    val signIn: String,
    val email: String,
    val passwordHint: String,
    val haveAccount: String,
    val newHere: String,
    val saveAccountBody: String,
    val welcomeBack: String,
)

private val EN = AppStrings(
    navMap = "Map", navBeaches = "Beaches", navLearn = "Learn", navProfile = "Profile",
    save = "Save", cancel = "Cancel", close = "Close", done = "Done", change = "Change ›",
    filterAll = "All", filterNests = "🥚 Nests", filterHatching = "● Hatching soon", filterTrash = "🧺 Trash",
    startPatrol = "● Start patrol", stopPatrol = "■ Stop patrol", noPatrolYet = "No patrol yet today",
    beaches = "Beaches", nearYou = "Near you", allBeaches = "All beaches", showBeaches = "Show beaches",
    nearMeAuto = "📍 Near me (auto)", findingBeaches = "Finding beaches near you…\nThey load automatically from the map.",
    profile = "Profile", saveAccount = "Save your account",
    saveAccountSub = "Add an email so your work isn't lost if you change phone.",
    signedIn = "Signed in", hatchlingsReached = "hatchlings reached the sea 🌊",
    dayStreak = "day streak", walked = "walked", patrols = "patrols", community = "Community",
    myBeach = "My beach", freeVolunteer = "🌊 Free volunteer", patrolClosest = "Patrol wherever's closest",
    yourHomeBeach = "Your home beach", homeBeach = "Home beach", patrolled = "Patrolled",
    badges = "Badges", noBadges = "No badges yet — patrol & log nests to earn your first 🐢",
    myPhotos = "My photos", trends = "📊  Trends & charts", language = "🌐  Language",
    yourName = "Your name", displayName = "Display name",
    signIn = "Sign in", email = "Email", passwordHint = "Password (6+ chars)",
    haveAccount = "Already have an account? Sign in", newHere = "New here? Create an account",
    saveAccountBody = "Add an email + password so your work syncs and isn't lost if you change phone.",
    welcomeBack = "Welcome back — sign in to load your nests & impact.",
)

private val RU = AppStrings(
    navMap = "Карта", navBeaches = "Пляжи", navLearn = "Гайд", navProfile = "Профиль",
    save = "Сохранить", cancel = "Отмена", close = "Закрыть", done = "Готово", change = "Изменить ›",
    filterAll = "Все", filterNests = "🥚 Гнёзда", filterHatching = "● Скоро вылупятся", filterTrash = "🧺 Мусор",
    startPatrol = "● Начать патруль", stopPatrol = "■ Стоп патруль", noPatrolYet = "Патруля сегодня ещё нет",
    beaches = "Пляжи", nearYou = "Рядом с вами", allBeaches = "Все пляжи", showBeaches = "Показывать пляжи",
    nearMeAuto = "📍 Рядом (авто)", findingBeaches = "Ищем пляжи рядом…\nОни подгружаются с карты автоматически.",
    profile = "Профиль", saveAccount = "Сохранить аккаунт",
    saveAccountSub = "Добавь почту, чтобы данные не потерялись при смене телефона.",
    signedIn = "Вы вошли", hatchlingsReached = "черепашат добрались до моря 🌊",
    dayStreak = "дней подряд", walked = "пройдено", patrols = "патрулей", community = "Сообщество",
    myBeach = "Мой пляж", freeVolunteer = "🌊 Свободный волонтёр", patrolClosest = "Патрулирую где ближе",
    yourHomeBeach = "Твой домашний пляж", homeBeach = "Домашний пляж", patrolled = "Был на пляжах",
    badges = "Бейджи", noBadges = "Бейджей пока нет — патрулируй и отмечай гнёзда 🐢",
    myPhotos = "Мои фото", trends = "📊  Графики и тренды", language = "🌐  Язык",
    yourName = "Твоё имя", displayName = "Отображаемое имя",
    signIn = "Войти", email = "Почта", passwordHint = "Пароль (6+ символов)",
    haveAccount = "Уже есть аккаунт? Войти", newHere = "Впервые? Создать аккаунт",
    saveAccountBody = "Добавь почту и пароль — данные синхронизируются и не потеряются при смене телефона.",
    welcomeBack = "С возвращением — войди, чтобы загрузить свои гнёзда и вклад.",
)

private val TR = AppStrings(
    navMap = "Harita", navBeaches = "Plajlar", navLearn = "Rehber", navProfile = "Profil",
    save = "Kaydet", cancel = "İptal", close = "Kapat", done = "Tamam", change = "Değiştir ›",
    filterAll = "Hepsi", filterNests = "🥚 Yuvalar", filterHatching = "● Yakında çıkacak", filterTrash = "🧺 Çöp",
    startPatrol = "● Devriyeye başla", stopPatrol = "■ Devriyeyi bitir", noPatrolYet = "Bugün henüz devriye yok",
    beaches = "Plajlar", nearYou = "Yakınında", allBeaches = "Tüm plajlar", showBeaches = "Plajları göster",
    nearMeAuto = "📍 Yakınım (oto)", findingBeaches = "Yakındaki plajlar aranıyor…\nHaritadan otomatik yüklenir.",
    profile = "Profil", saveAccount = "Hesabını kaydet",
    saveAccountSub = "Telefon değişince kaybolmaması için bir e-posta ekle.",
    signedIn = "Giriş yapıldı", hatchlingsReached = "yavru denize ulaştı 🌊",
    dayStreak = "gün seri", walked = "yürüdün", patrols = "devriye", community = "Topluluk",
    myBeach = "Plajım", freeVolunteer = "🌊 Serbest gönüllü", patrolClosest = "En yakın neresiyse orada",
    yourHomeBeach = "Ana plajın", homeBeach = "Ana plaj", patrolled = "Gezdiğin plajlar",
    badges = "Rozetler", noBadges = "Henüz rozet yok — devriye gez ve yuva kaydet 🐢",
    myPhotos = "Fotoğraflarım", trends = "📊  Grafikler ve eğilimler", language = "🌐  Dil",
    yourName = "Adın", displayName = "Görünen ad",
    signIn = "Giriş yap", email = "E-posta", passwordHint = "Şifre (6+ karakter)",
    haveAccount = "Hesabın var mı? Giriş yap", newHere = "Yeni misin? Hesap oluştur",
    saveAccountBody = "E-posta + şifre ekle — verilerin senkronize olur, telefon değişince kaybolmaz.",
    welcomeBack = "Tekrar hoş geldin — yuvalarını ve katkını yüklemek için giriş yap.",
)

/** The string bundle for [lang] ("ru" / "tr" / anything else → EN). */
fun appStrings(lang: String): AppStrings = when (lang.lowercase()) {
    "ru" -> RU
    "tr" -> TR
    else -> EN
}
