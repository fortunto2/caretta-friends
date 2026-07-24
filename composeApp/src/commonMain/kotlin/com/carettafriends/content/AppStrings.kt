package com.carettafriends.content

/**
 * UI string catalog for the whole interface (EN/RU/TR). Screens read the current bundle via
 * [appStrings]`(state.profile.language)` — the language is set in Profile → Language and persists.
 * Guide article bodies stay in [GuideContent]; this is the app chrome.
 */

/** Localized UI string catalog. Backed by a Map (NOT a 245-parameter constructor — that
 *  overflowed the Android/ART verifier). Fields stay `s.fieldName`; bundles build via mapOf. */
class AppStrings(private val m: Map<String, String>, val months: List<String>) {
    val navMap: String get() = m.getValue("navMap")
    val navBeaches: String get() = m.getValue("navBeaches")
    val navLearn: String get() = m.getValue("navLearn")
    val navProfile: String get() = m.getValue("navProfile")
    val save: String get() = m.getValue("save")
    val cancel: String get() = m.getValue("cancel")
    val close: String get() = m.getValue("close")
    val done: String get() = m.getValue("done")
    val change: String get() = m.getValue("change")
    val filterAll: String get() = m.getValue("filterAll")
    val filterNests: String get() = m.getValue("filterNests")
    val filterHatching: String get() = m.getValue("filterHatching")
    val filterTrash: String get() = m.getValue("filterTrash")
    val startPatrol: String get() = m.getValue("startPatrol")
    val stopPatrol: String get() = m.getValue("stopPatrol")
    val noPatrolYet: String get() = m.getValue("noPatrolYet")
    val beaches: String get() = m.getValue("beaches")
    val nearYou: String get() = m.getValue("nearYou")
    val allBeaches: String get() = m.getValue("allBeaches")
    val showBeaches: String get() = m.getValue("showBeaches")
    val nearMeAuto: String get() = m.getValue("nearMeAuto")
    val findingBeaches: String get() = m.getValue("findingBeaches")
    val profile: String get() = m.getValue("profile")
    val saveAccount: String get() = m.getValue("saveAccount")
    val saveAccountSub: String get() = m.getValue("saveAccountSub")
    val signedIn: String get() = m.getValue("signedIn")
    val hatchlingsReached: String get() = m.getValue("hatchlingsReached")
    val dayStreak: String get() = m.getValue("dayStreak")
    val walked: String get() = m.getValue("walked")
    val patrols: String get() = m.getValue("patrols")
    val community: String get() = m.getValue("community")
    val myBeach: String get() = m.getValue("myBeach")
    val freeVolunteer: String get() = m.getValue("freeVolunteer")
    val patrolClosest: String get() = m.getValue("patrolClosest")
    val yourHomeBeach: String get() = m.getValue("yourHomeBeach")
    val homeBeach: String get() = m.getValue("homeBeach")
    val patrolled: String get() = m.getValue("patrolled")
    val badges: String get() = m.getValue("badges")
    val noBadges: String get() = m.getValue("noBadges")
    val myPhotos: String get() = m.getValue("myPhotos")
    val trends: String get() = m.getValue("trends")
    val language: String get() = m.getValue("language")
    val yourName: String get() = m.getValue("yourName")
    val displayName: String get() = m.getValue("displayName")
    val signIn: String get() = m.getValue("signIn")
    val email: String get() = m.getValue("email")
    val passwordHint: String get() = m.getValue("passwordHint")
    val haveAccount: String get() = m.getValue("haveAccount")
    val newHere: String get() = m.getValue("newHere")
    val saveAccountBody: String get() = m.getValue("saveAccountBody")
    val welcomeBack: String get() = m.getValue("welcomeBack")
    val nestsWord: String get() = m.getValue("nestsWord")
    val activeWord: String get() = m.getValue("activeWord")
    val soonWord: String get() = m.getValue("soonWord")
    val hatchedWord: String get() = m.getValue("hatchedWord")
    val beachWord: String get() = m.getValue("beachWord")
    val protectedBeachLabel: String get() = m.getValue("protectedBeachLabel")
    val protectedWord: String get() = m.getValue("protectedWord")
    val watch: String get() = m.getValue("watch")
    val watching: String get() = m.getValue("watching")
    val foundWord: String get() = m.getValue("foundWord")
    val foundByWord: String get() = m.getValue("foundByWord")
    val hatchWindow: String get() = m.getValue("hatchWindow")
    val predictedModel: String get() = m.getValue("predictedModel")
    val exFullSun: String get() = m.getValue("exFullSun")
    val exPartial: String get() = m.getValue("exPartial")
    val exShade: String get() = m.getValue("exShade")
    val exUnknown: String get() = m.getValue("exUnknown")
    val airWord: String get() = m.getValue("airWord")
    val rain7d: String get() = m.getValue("rain7d")
    val timeline: String get() = m.getValue("timeline")
    val addUpdate: String get() = m.getValue("addUpdate")
    val comment: String get() = m.getValue("comment")
    val excavation: String get() = m.getValue("excavation")
    val excavationLockedSub: String get() = m.getValue("excavationLockedSub")
    val needsConfirm: String get() = m.getValue("needsConfirm")
    val condition: String get() = m.getValue("condition")
    val dateWord: String get() = m.getValue("dateWord")
    val whatObserved: String get() = m.getValue("whatObserved")
    val writeComment: String get() = m.getValue("writeComment")
    val condOk: String get() = m.getValue("condOk")
    val condHatching: String get() = m.getValue("condHatching")
    val condHatched: String get() = m.getValue("condHatched")
    val condDisturbed: String get() = m.getValue("condDisturbed")
    val condPredated: String get() = m.getValue("condPredated")
    val condWashed: String get() = m.getValue("condWashed")
    val nestFoundDate: String get() = m.getValue("nestFoundDate")
    val backDateHelp: String get() = m.getValue("backDateHelp")
    val today: String get() = m.getValue("today")
    val yesterday: String get() = m.getValue("yesterday")
    val daysAgo: String get() = m.getValue("daysAgo")
    val newMarker: String get() = m.getValue("newMarker")
    val whatFound: String get() = m.getValue("whatFound")
    val segNest: String get() = m.getValue("segNest")
    val segLandmark: String get() = m.getValue("segLandmark")
    val segTrash: String get() = m.getValue("segTrash")
    val nestOrFalse: String get() = m.getValue("nestOrFalse")
    val segFalseCrawl: String get() = m.getValue("segFalseCrawl")
    val photo: String get() = m.getValue("photo")
    val retake: String get() = m.getValue("retake")
    val cameraBtn: String get() = m.getValue("cameraBtn")
    val gallery: String get() = m.getValue("gallery")
    val photoAdded: String get() = m.getValue("photoAdded")
    val location: String get() = m.getValue("location")
    val fromExif: String get() = m.getValue("fromExif")
    val noGpsInPhoto: String get() = m.getValue("noGpsInPhoto")
    val confirmed: String get() = m.getValue("confirmed")
    val unconfirmed: String get() = m.getValue("unconfirmed")
    val detectedFrom: String get() = m.getValue("detectedFrom")
    val changePlain: String get() = m.getValue("changePlain")
    val noLocationYet: String get() = m.getValue("noLocationYet")
    val notRecognizedBeach: String get() = m.getValue("notRecognizedBeach")
    val protection: String get() = m.getValue("protection")
    val protNone: String get() = m.getValue("protNone")
    val protReed: String get() = m.getValue("protReed")
    val protCage: String get() = m.getValue("protCage")
    val protectionHelp: String get() = m.getValue("protectionHelp")
    val optionalSun: String get() = m.getValue("optionalSun")
    val segSun: String get() = m.getValue("segSun")
    val segPartial: String get() = m.getValue("segPartial")
    val segShade: String get() = m.getValue("segShade")
    val visibility: String get() = m.getValue("visibility")
    val visPublic: String get() = m.getValue("visPublic")
    val visPrivate: String get() = m.getValue("visPrivate")
    val saveNest: String get() = m.getValue("saveNest")
    val saveFalseCrawl: String get() = m.getValue("saveFalseCrawl")
    val trendsTitle: String get() = m.getValue("trendsTitle")
    val toSea: String get() = m.getValue("toSea")
    val nestsFound14: String get() = m.getValue("nestsFound14")
    val noNestsTrend: String get() = m.getValue("noNestsTrend")
    val hatchForecast8: String get() = m.getValue("hatchForecast8")
    val hatchForecastSub: String get() = m.getValue("hatchForecastSub")
    val excavationOutcomes: String get() = m.getValue("excavationOutcomes")
    val excavatedWord: String get() = m.getValue("excavatedWord")
    val hatchedEggs: String get() = m.getValue("hatchedEggs")
    val unhatchedWord: String get() = m.getValue("unhatchedWord")
    val noExcavations: String get() = m.getValue("noExcavations")
    val joinWhatsapp: String get() = m.getValue("joinWhatsapp")
    val joinSub: String get() = m.getValue("joinSub")
    val team: String get() = m.getValue("team")
    val youWord: String get() = m.getValue("youWord")
    val localGroupNear: String get() = m.getValue("localGroupNear")
    val localGroup: String get() = m.getValue("localGroup")
    val stubNotOnApp: String get() = m.getValue("stubNotOnApp")
    val stubClaim: String get() = m.getValue("stubClaim")
    val weRunGroup: String get() = m.getValue("weRunGroup")
    val reachThem: String get() = m.getValue("reachThem")
    val topVolunteers: String get() = m.getValue("topVolunteers")
    val noNestsBeFirst: String get() = m.getValue("noNestsBeFirst")
    val kindCommunity: String get() = m.getValue("kindCommunity")
    val kindNgo: String get() = m.getValue("kindNgo")
    val kindUniversity: String get() = m.getValue("kindUniversity")
    val kindOfficial: String get() = m.getValue("kindOfficial")
    val roleAdmin: String get() = m.getValue("roleAdmin")
    val roleLeader: String get() = m.getValue("roleLeader")
    val roleVolunteer: String get() = m.getValue("roleVolunteer")
    val reachWhatsapp: String get() = m.getValue("reachWhatsapp")
    val callWord: String get() = m.getValue("callWord")
    val instagramWord: String get() = m.getValue("instagramWord")
    val websiteWord: String get() = m.getValue("websiteWord")
    val contactWord: String get() = m.getValue("contactWord")
    val protectedBeachBanner: String get() = m.getValue("protectedBeachBanner")
    val unprotectedBeachBanner: String get() = m.getValue("unprotectedBeachBanner")
    val beachLeader: String get() = m.getValue("beachLeader")
    val feed: String get() = m.getValue("feed")
    val noNestsHereBeFirst: String get() = m.getValue("noNestsHereBeFirst")
    val dayWord: String get() = m.getValue("dayWord")
    val airCleanLabel: String get() = m.getValue("airCleanLabel")
    val airModerate: String get() = m.getValue("airModerate")
    val airUnhealthy: String get() = m.getValue("airUnhealthy")
    val airDustLabel: String get() = m.getValue("airDustLabel")
    val patrolNotAdvised: String get() = m.getValue("patrolNotAdvised")
    val dustNotAdvised: String get() = m.getValue("dustNotAdvised")
    val statusIncubating: String get() = m.getValue("statusIncubating")
    val statusHatchingSoon: String get() = m.getValue("statusHatchingSoon")
    val statusHatched: String get() = m.getValue("statusHatched")
    val statusExcavated: String get() = m.getValue("statusExcavated")
    val statusPredated: String get() = m.getValue("statusPredated")
    val statusWashedOver: String get() = m.getValue("statusWashedOver")
    val statusPoached: String get() = m.getValue("statusPoached")
    val statusLost: String get() = m.getValue("statusLost")
    val statusFalseCrawl: String get() = m.getValue("statusFalseCrawl")
    val dayLabel: String get() = m.getValue("dayLabel")
    val ob1Title: String get() = m.getValue("ob1Title")
    val ob1Body: String get() = m.getValue("ob1Body")
    val ob2Title: String get() = m.getValue("ob2Title")
    val ob2Body: String get() = m.getValue("ob2Body")
    val ob3Title: String get() = m.getValue("ob3Title")
    val ob3Body: String get() = m.getValue("ob3Body")
    val obNext: String get() = m.getValue("obNext")
    val obStart: String get() = m.getValue("obStart")
    val obSkip: String get() = m.getValue("obSkip")
    val segViolation: String get() = m.getValue("segViolation")
    val whatViolation: String get() = m.getValue("whatViolation")
    val vkTent: String get() = m.getValue("vkTent")
    val vkVehicle: String get() = m.getValue("vkVehicle")
    val vkLight: String get() = m.getValue("vkLight")
    val vkNoise: String get() = m.getValue("vkNoise")
    val vkDog: String get() = m.getValue("vkDog")
    val vkLitter: String get() = m.getValue("vkLitter")
    val vkOther: String get() = m.getValue("vkOther")
    val violationPrivateHint: String get() = m.getValue("violationPrivateHint")
    val anonymousWord: String get() = m.getValue("anonymousWord")
    val saveViolation: String get() = m.getValue("saveViolation")
    val filterViolations: String get() = m.getValue("filterViolations")
    val timelapse: String get() = m.getValue("timelapse")
    val shareImpact: String get() = m.getValue("shareImpact")
    val shareText: String get() = m.getValue("shareText")
    val addPhoto: String get() = m.getValue("addPhoto")
    val dupTitle: String get() = m.getValue("dupTitle")
    val dupBody: String get() = m.getValue("dupBody")
    val dupOpenExisting: String get() = m.getValue("dupOpenExisting")
    val dupAddNew: String get() = m.getValue("dupAddNew")
    val excavationTitle: String get() = m.getValue("excavationTitle")
    val excFinalCount: String get() = m.getValue("excFinalCount")
    val excCountWhatYouFind: String get() = m.getValue("excCountWhatYouFind")
    /** Excavation coordination note; contains a "%s" placeholder for the community's authority name. */
    val excAuthNote: String get() = m.getValue("excAuthNote")
    /** Generic fallback for the regulator name when a community hasn't set its own authority. */
    val authorityGeneric: String get() = m.getValue("authorityGeneric")
    val excShells: String get() = m.getValue("excShells")
    val excUnhatched: String get() = m.getValue("excUnhatched")
    val excPipped: String get() = m.getValue("excPipped")
    val excInNest: String get() = m.getValue("excInNest")
    val excHelpedOut: String get() = m.getValue("excHelpedOut")
    val excSuccessLabel: String get() = m.getValue("excSuccessLabel")
    val excHatchingSuccess: String get() = m.getValue("excHatchingSuccess")
    val excEmergenceSuccess: String get() = m.getValue("excEmergenceSuccess")
    val excFinish: String get() = m.getValue("excFinish")
    val nestNotFound: String get() = m.getValue("nestNotFound")
    val beachNotFound: String get() = m.getValue("beachNotFound")
    val noBeachesInCity: String get() = m.getValue("noBeachesInCity")
    val openWord: String get() = m.getValue("openWord")
    val yourCommunity: String get() = m.getValue("yourCommunity")
    val volunteersWord: String get() = m.getValue("volunteersWord")
    val cameraFrameHint: String get() = m.getValue("cameraFrameHint")
    val badgeFirstNest: String get() = m.getValue("badgeFirstNest")
    val badgeFirstDig: String get() = m.getValue("badgeFirstDig")
    val badgeRescuer: String get() = m.getValue("badgeRescuer")
    val badgeSeason50: String get() = m.getValue("badgeSeason50")
    val patrolPublishTitle: String get() = m.getValue("patrolPublishTitle")
    val patrolKeepPrivate: String get() = m.getValue("patrolKeepPrivate")
    val patrolPublish: String get() = m.getValue("patrolPublish")
    val patrolPublishBody: String get() = m.getValue("patrolPublishBody")
    val openSettings: String get() = m.getValue("openSettings")
    val cameraStarting: String get() = m.getValue("cameraStarting")
    val cameraDenied: String get() = m.getValue("cameraDenied")
    val memberNestsStat: String get() = m.getValue("memberNestsStat")
    val memberHatchStat: String get() = m.getValue("memberHatchStat")
    val memberNestsSection: String get() = m.getValue("memberNestsSection")
    val memberNoNests: String get() = m.getValue("memberNoNests")
    val memberLink: String get() = m.getValue("memberLink")
    val tlNestFound: String get() = m.getValue("tlNestFound")
    val tlFalseCrawl: String get() = m.getValue("tlFalseCrawl")
    val tlHatchSuccess: String get() = m.getValue("tlHatchSuccess")
    val onMapWord: String get() = m.getValue("onMapWord")
    val awayWord: String get() = m.getValue("awayWord")
    val condPoached: String get() = m.getValue("condPoached")
    val hatchWindowOpen: String get() = m.getValue("hatchWindowOpen")
    val impactZeroCta: String get() = m.getValue("impactZeroCta")
    val excZeroHint: String get() = m.getValue("excZeroHint")
    val excThanksRecord: String get() = m.getValue("excThanksRecord")
    val activityTitle: String get() = m.getValue("activityTitle")
    val rangeWeek: String get() = m.getValue("rangeWeek")
    val rangeMonth: String get() = m.getValue("rangeMonth")
    val shareDay: String get() = m.getValue("shareDay")
    val activityEmpty: String get() = m.getValue("activityEmpty")
    val moreDetails: String get() = m.getValue("moreDetails")
    val addPhotoBtn: String get() = m.getValue("addPhotoBtn")
    val noteHint: String get() = m.getValue("noteHint")
}

private fun enStrings() = AppStrings(
    mapOf(
        "navMap" to "Map",
        "navBeaches" to "Beaches",
        "navLearn" to "Learn",
        "navProfile" to "Profile",
        "save" to "Save",
        "cancel" to "Cancel",
        "close" to "Close",
        "done" to "Done",
        "change" to "Change ›",
        "filterAll" to "All",
        "filterNests" to "🥚 Nests",
        "filterHatching" to "● Hatching soon",
        "filterTrash" to "🧺 Trash",
        "startPatrol" to "● Start patrol",
        "stopPatrol" to "■ Stop patrol",
        "noPatrolYet" to "No patrol yet today",
        "beaches" to "Beaches",
        "nearYou" to "Near you",
        "allBeaches" to "All beaches",
        "showBeaches" to "Show beaches",
        "nearMeAuto" to "📍 Near me (auto)",
        "findingBeaches" to "Finding beaches near you…\nThey load automatically from the map.",
        "profile" to "Profile",
        "saveAccount" to "Save your account",
        "saveAccountSub" to "Add an email so your work isn't lost if you change phone.",
        "signedIn" to "Signed in",
        "hatchlingsReached" to "hatchlings reached the sea 🌊",
        "dayStreak" to "day streak",
        "walked" to "walked",
        "patrols" to "patrols",
        "community" to "Community",
        "myBeach" to "My beach",
        "freeVolunteer" to "🌊 Free volunteer",
        "patrolClosest" to "Patrol wherever's closest",
        "yourHomeBeach" to "Your home beach",
        "homeBeach" to "Home beach",
        "patrolled" to "Patrolled",
        "badges" to "Badges",
        "noBadges" to "No badges yet — patrol & log nests to earn your first 🐢",
        "myPhotos" to "My photos",
        "trends" to "📊  Trends & charts",
        "language" to "🌐  Language",
        "yourName" to "Your name",
        "displayName" to "Display name",
        "signIn" to "Sign in",
        "email" to "Email",
        "passwordHint" to "Password (6+ chars)",
        "haveAccount" to "Already have an account? Sign in",
        "newHere" to "New here? Create an account",
        "saveAccountBody" to "Add an email + password so your work syncs and isn't lost if you change phone.",
        "welcomeBack" to "Welcome back — sign in to load your nests & impact.",
        "nestsWord" to "nests",
        "activeWord" to "active",
        "soonWord" to "soon",
        "hatchedWord" to "hatched",
        "beachWord" to "Beach",
        "protectedBeachLabel" to "🛡️ Protected nesting beach",
        "protectedWord" to "protected",
        "watch" to "🔔 Watch",
        "watching" to "🔔 Watching",
        "foundWord" to "found",
        "foundByWord" to "Found by",
        "hatchWindow" to "Hatch window",
        "predictedModel" to "predicted · temperature model",
        "exFullSun" to "full sun",
        "exPartial" to "partial",
        "exShade" to "shade",
        "exUnknown" to "exposure ?",
        "airWord" to "air",
        "rain7d" to "rain 7d",
        "timeline" to "Timeline · updates & comments",
        "addUpdate" to "＋ Add update",
        "comment" to "💬 Comment",
        "excavation" to "⛏️ Excavation",
        "excavationLockedSub" to "Done by experienced volunteers & beach leaders — ask your beach leader.",
        "needsConfirm" to "⚠️  Needs a photo / precise location — tap to confirm",
        "condition" to "Condition",
        "dateWord" to "Date",
        "whatObserved" to "What did you observe?",
        "writeComment" to "Write a comment…",
        "condOk" to "OK",
        "condHatching" to "Hatching",
        "condHatched" to "Hatched",
        "condDisturbed" to "Disturbed",
        "condPredated" to "Predated",
        "condWashed" to "Washed over",
        "nestFoundDate" to "Nest found date",
        "backDateHelp" to "Word-of-mouth it was found earlier but the photo arrived now? Set the real day — up to a month back. The incubation day & hatch forecast recompute automatically.",
        "today" to "today",
        "yesterday" to "yesterday",
        "daysAgo" to "%d days ago",
        "newMarker" to "New marker",
        "whatFound" to "What did you find?",
        "segNest" to "🥚 Nest",
        "segLandmark" to "🚩 Landmark",
        "segTrash" to "🧺 Trash",
        "nestOrFalse" to "Nest or false crawl?",
        "segFalseCrawl" to "🌀 False crawl",
        "photo" to "Photo",
        "retake" to "📷 Retake",
        "cameraBtn" to "📷 Camera",
        "gallery" to "🖼️ Gallery",
        "photoAdded" to "Photo added",
        "location" to "Location",
        "fromExif" to "From photo · EXIF",
        "noGpsInPhoto" to "No GPS in photo — set to the beach",
        "confirmed" to "Confirmed ✓",
        "unconfirmed" to "Unconfirmed",
        "detectedFrom" to "Detected from your location",
        "changePlain" to "Change",
        "noLocationYet" to "No location yet — take a photo on-site or pick the beach.",
        "notRecognizedBeach" to "🌊 Not recognized as a known nesting beach — pick the right one.",
        "protection" to "Protection",
        "protNone" to "None",
        "protReed" to "🌾 Reed",
        "protCage" to "🛡️ Cage",
        "protectionHelp" to "Reed = stakes + reed fence + tape + sign. Cage = metal cage (stronger, but scarce & heavy).",
        "optionalSun" to "Optional: sun exposure",
        "segSun" to "☀️ Sun",
        "segPartial" to "⛅ Partial",
        "segShade" to "🌴 Shade",
        "visibility" to "Visibility",
        "visPublic" to "🌍 Public",
        "visPrivate" to "🔒 Private",
        "saveNest" to "Save nest 🐢",
        "saveFalseCrawl" to "Save false crawl 🌀",
        "trendsTitle" to "Trends",
        "toSea" to "🐢 to sea",
        "nestsFound14" to "Nests found · last 14 days",
        "noNestsTrend" to "No nests logged yet — add some to see the trend.",
        "hatchForecast8" to "Hatch-window forecast · next 8 weeks",
        "hatchForecastSub" to "When incubating nests are expected to start emerging (found date + ~incubation).",
        "excavationOutcomes" to "Excavation outcomes",
        "excavatedWord" to "excavated",
        "hatchedEggs" to "hatched eggs",
        "unhatchedWord" to "unhatched",
        "noExcavations" to "No excavations recorded yet.",
        "joinWhatsapp" to "💬 Join — message us on WhatsApp",
        "joinSub" to "New here? Message us on WhatsApp or Instagram — an admin will add you to the team.",
        "team" to "Team",
        "youWord" to "you",
        "localGroupNear" to "Local group · near",
        "localGroup" to "Local conservation group",
        "stubNotOnApp" to "🐢 This local group isn't on Caretta Friends yet — reach out to them directly below. ",
        "stubClaim" to "Are you part of it? Message us and we'll give your admins access.",
        "weRunGroup" to "🙋 We run this group — give us access",
        "reachThem" to "Reach them",
        "topVolunteers" to "Top volunteers · by nests",
        "noNestsBeFirst" to "No nests logged yet — be the first to mark one this season 🥚",
        "kindCommunity" to "🐢 Volunteer group",
        "kindNgo" to "🌍 NGO",
        "kindUniversity" to "🎓 University",
        "kindOfficial" to "🏛️ Official",
        "roleAdmin" to "Admin",
        "roleLeader" to "Leader",
        "roleVolunteer" to "Volunteer",
        "reachWhatsapp" to "Message on WhatsApp",
        "callWord" to "Call",
        "instagramWord" to "Instagram",
        "websiteWord" to "Website",
        "contactWord" to "Contact",
        "protectedBeachBanner" to "🛡️ Official protected nesting beach — night access banned in season.",
        "unprotectedBeachBanner" to "🏖️ Not an official protected beach — nests logged here still count & sync.",
        "beachLeader" to "beach leader",
        "feed" to "Feed",
        "noNestsHereBeFirst" to "No nests logged here yet — be the first 🥚",
        "dayWord" to "Day",
        "airCleanLabel" to "Air clean",
        "airModerate" to "Moderate air",
        "airUnhealthy" to "Unhealthy",
        "airDustLabel" to "Dust",
        "patrolNotAdvised" to "patrol not advised",
        "dustNotAdvised" to "⚠ Dust — patrol not advised",
        "statusIncubating" to "Incubating",
        "statusHatchingSoon" to "Hatching soon",
        "statusHatched" to "Hatched",
        "statusExcavated" to "Excavated",
        "statusPredated" to "Predated",
        "statusWashedOver" to "Washed over",
        "statusPoached" to "Poached",
        "statusLost" to "Lost",
        "statusFalseCrawl" to "False crawl",
        "dayLabel" to "DAY",
        "ob1Title" to "Welcome to Caretta Friends 🐢",
        "ob1Body" to "We protect loggerhead sea-turtle nests together — one beach, one nest, one hatchling at a time.",
        "ob2Title" to "Found a nest? Add it 📍",
        "ob2Body" to "Snap a photo on the beach and mark the nest. Then patrol, log updates, and watch the hatch forecast until the hatchlings reach the sea.",
        "ob3Title" to "Every patrol counts 🙌",
        "ob3Body" to "Join your local community, keep the beach dark at night, and help hundreds of hatchlings make it to the waves. Ready?",
        "obNext" to "Next",
        "obStart" to "Let's go 🌊",
        "obSkip" to "Skip",
        "segViolation" to "⛔ Violation",
        "whatViolation" to "What's the violation?",
        "vkTent" to "⛺ Tent",
        "vkVehicle" to "🚗 Vehicle",
        "vkLight" to "🔦 Light",
        "vkNoise" to "🔊 Noise",
        "vkDog" to "🐕 Dog",
        "vkLitter" to "🧺 Litter",
        "vkOther" to "⚠ Other",
        "violationPrivateHint" to "Kept private for the record — guests won't see who reported it.",
        "anonymousWord" to "Report anonymously",
        "saveViolation" to "Report violation ⛔",
        "filterViolations" to "⛔ Violations",
        "timelapse" to "🎞️ Timelapse",
        "shareImpact" to "📣 Share my impact",
        "shareText" to "🐢 I'm a Caretta Friends volunteer — %d hatchlings have reached the sea! Join us: carettafriends.com",
        "addPhoto" to "📷 Photo",
        "dupTitle" to "Already logged here?",
        "dupBody" to "%1\$s is already marked %2\$s away. Open it instead of adding a duplicate?",
        "dupOpenExisting" to "Open %1\$s",
        "dupAddNew" to "No, add a new nest",
        "excavationTitle" to "Nest excavation",
        "excFinalCount" to "final count",
        "excCountWhatYouFind" to "Count what you find",
        "excAuthNote" to "Excavation is coordinated with %s and done with the authorized team — the count goes into the official protocol, not a public event.",
        "authorityGeneric" to "the local conservation authority",
        "excShells" to "Hatched / empty shells",
        "excUnhatched" to "Unhatched / whole eggs",
        "excPipped" to "Pipped / in egg",
        "excInNest" to "In nest / stuck",
        "excHelpedOut" to "Helped out / rescued",
        "excSuccessLabel" to "Success",
        "excHatchingSuccess" to "Hatching success",
        "excEmergenceSuccess" to "Emergence success",
        "excFinish" to "Finish & celebrate 🎉",
        "nestNotFound" to "Nest not found",
        "beachNotFound" to "Beach not found",
        "noBeachesInCity" to "No beaches in %s yet.",
        "openWord" to "Open",
        "yourCommunity" to "Your community",
        "volunteersWord" to "volunteers",
        "cameraFrameHint" to "Frame the nest",
        "badgeFirstNest" to "First nest",
        "badgeFirstDig" to "First excavation",
        "badgeRescuer" to "Rescuer",
        "badgeSeason50" to "Season 50",
        "patrolPublishTitle" to "Publish this patrol?",
        "patrolKeepPrivate" to "Keep private",
        "patrolPublish" to "Publish",
        "patrolPublishBody" to "Your walk is saved on your phone. Publish to share the route with your community — your live location is never shared.",
        "openSettings" to "Open Settings",
        "cameraStarting" to "Starting camera…",
        "cameraDenied" to "Camera access denied. Enable it in Settings.",
        "memberNestsStat" to "nests found",
        "memberHatchStat" to "hatchlings freed",
        "memberNestsSection" to "Nests",
        "memberNoNests" to "No nests logged yet.",
        "memberLink" to "Link",
        "tlNestFound" to "Nest found",
        "tlFalseCrawl" to "False crawl logged",
        "tlHatchSuccess" to "hatch success",
        "onMapWord" to "View on map",
        "awayWord" to "away",
        "condPoached" to "poached",
        "hatchWindowOpen" to "Hatch window open 🐣",
        "impactZeroCta" to "Log your first nest to start your impact 🐣",
        "excZeroHint" to "Count what you find below ↓",
        "excThanksRecord" to "Every count matters — thank you for recording this nest 🐢",
        "activityTitle" to "Activity",
        "rangeWeek" to "Week",
        "rangeMonth" to "Month",
        "shareDay" to "Share today",
        "activityEmpty" to "Nothing here yet for this period.",
        "moreDetails" to "More details",
        "addPhotoBtn" to "📷 Add photo",
        "noteHint" to "Add a note (optional)",
    ),
    months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
)

private fun ruStrings() = AppStrings(
    mapOf(
        "navMap" to "Карта",
        "navBeaches" to "Пляжи",
        "navLearn" to "Гайд",
        "navProfile" to "Профиль",
        "save" to "Сохранить",
        "cancel" to "Отмена",
        "close" to "Закрыть",
        "done" to "Готово",
        "change" to "Изменить ›",
        "filterAll" to "Все",
        "filterNests" to "🥚 Гнёзда",
        "filterHatching" to "● Скоро вылупятся",
        "filterTrash" to "🧺 Мусор",
        "startPatrol" to "● Начать патруль",
        "stopPatrol" to "■ Стоп патруль",
        "noPatrolYet" to "Патруля сегодня ещё нет",
        "beaches" to "Пляжи",
        "nearYou" to "Рядом с вами",
        "allBeaches" to "Все пляжи",
        "showBeaches" to "Показывать пляжи",
        "nearMeAuto" to "📍 Рядом (авто)",
        "findingBeaches" to "Ищем пляжи рядом…\nОни подгружаются с карты автоматически.",
        "profile" to "Профиль",
        "saveAccount" to "Сохранить аккаунт",
        "saveAccountSub" to "Добавь почту, чтобы данные не потерялись при смене телефона.",
        "signedIn" to "Вы вошли",
        "hatchlingsReached" to "черепашат добрались до моря 🌊",
        "dayStreak" to "дней подряд",
        "walked" to "пройдено",
        "patrols" to "патрулей",
        "community" to "Сообщество",
        "myBeach" to "Мой пляж",
        "freeVolunteer" to "🌊 Свободный волонтёр",
        "patrolClosest" to "Патрулирую где ближе",
        "yourHomeBeach" to "Твой домашний пляж",
        "homeBeach" to "Домашний пляж",
        "patrolled" to "Был на пляжах",
        "badges" to "Бейджи",
        "noBadges" to "Бейджей пока нет — патрулируй и отмечай гнёзда 🐢",
        "myPhotos" to "Мои фото",
        "trends" to "📊  Графики и тренды",
        "language" to "🌐  Язык",
        "yourName" to "Твоё имя",
        "displayName" to "Отображаемое имя",
        "signIn" to "Войти",
        "email" to "Почта",
        "passwordHint" to "Пароль (6+ символов)",
        "haveAccount" to "Уже есть аккаунт? Войти",
        "newHere" to "Впервые? Создать аккаунт",
        "saveAccountBody" to "Добавь почту и пароль — данные синхронизируются и не потеряются при смене телефона.",
        "welcomeBack" to "С возвращением — войди, чтобы загрузить свои гнёзда и вклад.",
        "nestsWord" to "гнёзд",
        "activeWord" to "активн.",
        "soonWord" to "скоро",
        "hatchedWord" to "вылупились",
        "beachWord" to "Пляж",
        "protectedBeachLabel" to "🛡️ Охраняемый пляж гнездования",
        "protectedWord" to "под охраной",
        "watch" to "🔔 Следить",
        "watching" to "🔔 Слежу",
        "foundWord" to "найдено",
        "foundByWord" to "Автор находки",
        "hatchWindow" to "Окно вылупления",
        "predictedModel" to "прогноз · температурная модель",
        "exFullSun" to "солнце",
        "exPartial" to "полутень",
        "exShade" to "тень",
        "exUnknown" to "освещённость ?",
        "airWord" to "возд.",
        "rain7d" to "дождь 7д",
        "timeline" to "Лента · апдейты и комменты",
        "addUpdate" to "＋ Апдейт",
        "comment" to "💬 Коммент",
        "excavation" to "⛏️ Вскрытие",
        "excavationLockedSub" to "Делают опытные волонтёры и лидеры пляжа — спроси своего лидера.",
        "needsConfirm" to "⚠️  Нужно фото / точная точка — нажми, чтобы подтвердить",
        "condition" to "Состояние",
        "dateWord" to "Дата",
        "whatObserved" to "Что заметил?",
        "writeComment" to "Напиши комментарий…",
        "condOk" to "ОК",
        "condHatching" to "Вылупляются",
        "condHatched" to "Вылупились",
        "condDisturbed" to "Потревожено",
        "condPredated" to "Хищник",
        "condWashed" to "Смыло",
        "nestFoundDate" to "Дата находки гнезда",
        "backDateHelp" to "Со слов нашли раньше, а фото дошло сейчас? Поставь реальный день — до месяца назад. День инкубации и прогноз вылупления пересчитаются автоматически.",
        "today" to "сегодня",
        "yesterday" to "вчера",
        "daysAgo" to "%d дн. назад",
        "newMarker" to "Новая метка",
        "whatFound" to "Что ты нашёл?",
        "segNest" to "🥚 Гнездо",
        "segLandmark" to "🚩 Ориентир",
        "segTrash" to "🧺 Мусор",
        "nestOrFalse" to "Гнездо или ложный выход?",
        "segFalseCrawl" to "🌀 Ложный выход",
        "photo" to "Фото",
        "retake" to "📷 Переснять",
        "cameraBtn" to "📷 Камера",
        "gallery" to "🖼️ Галерея",
        "photoAdded" to "Фото добавлено",
        "location" to "Локация",
        "fromExif" to "Из фото · EXIF",
        "noGpsInPhoto" to "В фото нет GPS — привязано к пляжу",
        "confirmed" to "Подтверждено ✓",
        "unconfirmed" to "Не подтверждено",
        "detectedFrom" to "Определено по локации",
        "changePlain" to "Изменить",
        "noLocationYet" to "Локации пока нет — сделай фото на месте или выбери пляж.",
        "notRecognizedBeach" to "🌊 Не распознан как известный пляж — выбери нужный.",
        "protection" to "Защита",
        "protNone" to "Нет",
        "protReed" to "🌾 Тростник",
        "protCage" to "🛡️ Клетка",
        "protectionHelp" to "Тростник = колышки + тростниковый забор + лента + табличка. Клетка = металлическая (крепче, но редкая и тяжёлая).",
        "optionalSun" to "Опционально: освещённость",
        "segSun" to "☀️ Солнце",
        "segPartial" to "⛅ Полутень",
        "segShade" to "🌴 Тень",
        "visibility" to "Видимость",
        "visPublic" to "🌍 Публично",
        "visPrivate" to "🔒 Приватно",
        "saveNest" to "Сохранить гнездо 🐢",
        "saveFalseCrawl" to "Сохранить ложный выход 🌀",
        "trendsTitle" to "Тренды",
        "toSea" to "🐢 в море",
        "nestsFound14" to "Найдено гнёзд · за 14 дней",
        "noNestsTrend" to "Гнёзд пока нет — добавь, чтобы увидеть тренд.",
        "hatchForecast8" to "Прогноз вылупления · след. 8 недель",
        "hatchForecastSub" to "Когда инкубируемые гнёзда должны начать вылупляться (дата находки + ~инкубация).",
        "excavationOutcomes" to "Итоги вскрытий",
        "excavatedWord" to "вскрыто",
        "hatchedEggs" to "вылупилось",
        "unhatchedWord" to "не вылупилось",
        "noExcavations" to "Вскрытий пока нет.",
        "joinWhatsapp" to "💬 Присоединиться — напиши нам в WhatsApp",
        "joinSub" to "Впервые? Напиши в WhatsApp или Instagram — админ добавит тебя в команду.",
        "team" to "Команда",
        "youWord" to "ты",
        "localGroupNear" to "Локальная группа · рядом с",
        "localGroup" to "Локальная природоохранная группа",
        "stubNotOnApp" to "🐢 Этой группы пока нет в Caretta Friends — свяжись с ними напрямую ниже. ",
        "stubClaim" to "Ты из этой группы? Напиши нам, дадим доступ вашим админам.",
        "weRunGroup" to "🙋 Это наша группа — дайте нам доступ",
        "reachThem" to "Связаться",
        "topVolunteers" to "Топ волонтёров · по гнёздам",
        "noNestsBeFirst" to "Гнёзд пока нет — отметь первое в этом сезоне 🥚",
        "kindCommunity" to "🐢 Группа волонтёров",
        "kindNgo" to "🌍 НКО",
        "kindUniversity" to "🎓 Университет",
        "kindOfficial" to "🏛️ Официальный орган",
        "roleAdmin" to "Админ",
        "roleLeader" to "Лидер",
        "roleVolunteer" to "Волонтёр",
        "reachWhatsapp" to "Написать в WhatsApp",
        "callWord" to "Позвонить",
        "instagramWord" to "Instagram",
        "websiteWord" to "Сайт",
        "contactWord" to "Контакт",
        "protectedBeachBanner" to "🛡️ Официально охраняемый пляж — ночью в сезон вход запрещён.",
        "unprotectedBeachBanner" to "🏖️ Неофициальный пляж — гнёзда здесь всё равно считаются и синхронизируются.",
        "beachLeader" to "лидер пляжа",
        "feed" to "Лента",
        "noNestsHereBeFirst" to "Здесь гнёзд пока нет — будь первым 🥚",
        "dayWord" to "День",
        "airCleanLabel" to "Воздух чистый",
        "airModerate" to "Умеренный воздух",
        "airUnhealthy" to "Вредный",
        "airDustLabel" to "Пыль",
        "patrolNotAdvised" to "патруль не рекомендуется",
        "dustNotAdvised" to "⚠ Пыль — патруль не рекомендуется",
        "statusIncubating" to "Инкубация",
        "statusHatchingSoon" to "Скоро вылупление",
        "statusHatched" to "Вылупилось",
        "statusExcavated" to "Вскрыто",
        "statusPredated" to "Хищник",
        "statusWashedOver" to "Смыло",
        "statusPoached" to "Разорено",
        "statusLost" to "Потеряно",
        "statusFalseCrawl" to "Ложный выход",
        "dayLabel" to "ДЕНЬ",
        "ob1Title" to "Добро пожаловать в Caretta Friends 🐢",
        "ob1Body" to "Мы вместе защищаем гнёзда морских черепах-логгерхедов — по одному пляжу, гнезду и черепашонку.",
        "ob2Title" to "Нашёл гнездо? Добавь 📍",
        "ob2Body" to "Сделай фото на пляже и отметь гнездо. Потом патрулируй, добавляй апдейты и следи за прогнозом вылупления, пока черепашата не доберутся до моря.",
        "ob3Title" to "Каждый патруль важен 🙌",
        "ob3Body" to "Присоединяйся к местному сообществу, держи пляж тёмным ночью и помоги сотням черепашат добраться до волн. Готов?",
        "obNext" to "Далее",
        "obStart" to "Поехали 🌊",
        "obSkip" to "Пропустить",
        "segViolation" to "⛔ Нарушение",
        "whatViolation" to "Что за нарушение?",
        "vkTent" to "⛺ Палатка",
        "vkVehicle" to "🚗 Машина",
        "vkLight" to "🔦 Свет",
        "vkNoise" to "🔊 Шум",
        "vkDog" to "🐕 Собака",
        "vkLitter" to "🧺 Мусор",
        "vkOther" to "⚠ Другое",
        "violationPrivateHint" to "Хранится приватно для жалобы — гости не увидят, кто сообщил.",
        "anonymousWord" to "Сообщить анонимно",
        "saveViolation" to "Сообщить о нарушении ⛔",
        "filterViolations" to "⛔ Нарушения",
        "timelapse" to "🎞️ Таймлапс",
        "shareImpact" to "📣 Поделиться вкладом",
        "shareText" to "🐢 Я волонтёр Caretta Friends — %d черепашат добрались до моря! Присоединяйся: carettafriends.com",
        "addPhoto" to "📷 Фото",
        "dupTitle" to "Уже отмечено здесь?",
        "dupBody" to "%1\$s уже отмечено в %2\$s отсюда. Открыть его, а не создавать дубль?",
        "dupOpenExisting" to "Открыть %1\$s",
        "dupAddNew" to "Нет, это новое гнездо",
        "excavationTitle" to "Вскрытие гнезда",
        "excFinalCount" to "финальный подсчёт",
        "excCountWhatYouFind" to "Считай, что нашёл",
        "excAuthNote" to "Вскрытие — по согласованию с %s и с уполномоченной командой; подсчёт идёт в официальный протокол, а не публичное мероприятие.",
        "authorityGeneric" to "местным природоохранным органом",
        "excShells" to "Вылупились / пустые скорлупки",
        "excUnhatched" to "Не вылупились / целые яйца",
        "excPipped" to "Проклюнулись / в яйце",
        "excInNest" to "В гнезде / застряли",
        "excHelpedOut" to "Помогли выбраться / спасли",
        "excSuccessLabel" to "Успех",
        "excHatchingSuccess" to "Успех вылупления",
        "excEmergenceSuccess" to "Успех выхода",
        "excFinish" to "Готово, отмечаем 🎉",
        "nestNotFound" to "Гнездо не найдено",
        "beachNotFound" to "Пляж не найден",
        "noBeachesInCity" to "Пляжей в %s пока нет.",
        "openWord" to "Открыть",
        "yourCommunity" to "Твоё сообщество",
        "volunteersWord" to "волонтёров",
        "cameraFrameHint" to "Наведи на гнездо",
        "badgeFirstNest" to "Первое гнездо",
        "badgeFirstDig" to "Первое вскрытие",
        "badgeRescuer" to "Спасатель",
        "badgeSeason50" to "Сезон 50",
        "patrolPublishTitle" to "Опубликовать патруль?",
        "patrolKeepPrivate" to "Оставить приватным",
        "patrolPublish" to "Опубликовать",
        "patrolPublishBody" to "Прогулка сохранена на телефоне. Опубликуй, чтобы поделиться маршрутом с сообществом — твоя геопозиция в реальном времени никогда не передаётся.",
        "openSettings" to "Открыть настройки",
        "cameraStarting" to "Запуск камеры…",
        "cameraDenied" to "Доступ к камере запрещён. Включите его в Настройках.",
        "memberNestsStat" to "гнёзд найдено",
        "memberHatchStat" to "черепашат спасено",
        "memberNestsSection" to "Гнёзда",
        "memberNoNests" to "Пока нет гнёзд.",
        "memberLink" to "Ссылка",
        "tlNestFound" to "Гнездо найдено",
        "tlFalseCrawl" to "Отмечен ложный выход",
        "tlHatchSuccess" to "успех вылупления",
        "onMapWord" to "На карте",
        "awayWord" to "отсюда",
        "condPoached" to "разграблено",
        "hatchWindowOpen" to "Окно вылупления открыто 🐣",
        "impactZeroCta" to "Отметь первое гнездо — и счёт пойдёт 🐣",
        "excZeroHint" to "Считай находки ниже ↓",
        "excThanksRecord" to "Каждый учёт важен — спасибо, что записал это гнездо 🐢",
        "activityTitle" to "Активность",
        "rangeWeek" to "Неделя",
        "rangeMonth" to "Месяц",
        "shareDay" to "Отчёт за день",
        "activityEmpty" to "За этот период пока пусто.",
        "moreDetails" to "Подробнее",
        "addPhotoBtn" to "📷 Добавить фото",
        "noteHint" to "Заметка (необязательно)",
    ),
    months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"),
)

private fun trStrings() = AppStrings(
    mapOf(
        "navMap" to "Harita",
        "navBeaches" to "Plajlar",
        "navLearn" to "Rehber",
        "navProfile" to "Profil",
        "save" to "Kaydet",
        "cancel" to "İptal",
        "close" to "Kapat",
        "done" to "Tamam",
        "change" to "Değiştir ›",
        "filterAll" to "Hepsi",
        "filterNests" to "🥚 Yuvalar",
        "filterHatching" to "● Yakında çıkacak",
        "filterTrash" to "🧺 Çöp",
        "startPatrol" to "● Devriyeye başla",
        "stopPatrol" to "■ Devriyeyi bitir",
        "noPatrolYet" to "Bugün henüz devriye yok",
        "beaches" to "Plajlar",
        "nearYou" to "Yakınında",
        "allBeaches" to "Tüm plajlar",
        "showBeaches" to "Plajları göster",
        "nearMeAuto" to "📍 Yakınım (oto)",
        "findingBeaches" to "Yakındaki plajlar aranıyor…\nHaritadan otomatik yüklenir.",
        "profile" to "Profil",
        "saveAccount" to "Hesabını kaydet",
        "saveAccountSub" to "Telefon değişince kaybolmaması için bir e-posta ekle.",
        "signedIn" to "Giriş yapıldı",
        "hatchlingsReached" to "yavru denize ulaştı 🌊",
        "dayStreak" to "gün seri",
        "walked" to "yürüdün",
        "patrols" to "devriye",
        "community" to "Topluluk",
        "myBeach" to "Plajım",
        "freeVolunteer" to "🌊 Serbest gönüllü",
        "patrolClosest" to "En yakın neresiyse orada",
        "yourHomeBeach" to "Ana plajın",
        "homeBeach" to "Ana plaj",
        "patrolled" to "Gezdiğin plajlar",
        "badges" to "Rozetler",
        "noBadges" to "Henüz rozet yok — devriye gez ve yuva kaydet 🐢",
        "myPhotos" to "Fotoğraflarım",
        "trends" to "📊  Grafikler ve eğilimler",
        "language" to "🌐  Dil",
        "yourName" to "Adın",
        "displayName" to "Görünen ad",
        "signIn" to "Giriş yap",
        "email" to "E-posta",
        "passwordHint" to "Şifre (6+ karakter)",
        "haveAccount" to "Hesabın var mı? Giriş yap",
        "newHere" to "Yeni misin? Hesap oluştur",
        "saveAccountBody" to "E-posta + şifre ekle — verilerin senkronize olur, telefon değişince kaybolmaz.",
        "welcomeBack" to "Tekrar hoş geldin — yuvalarını ve katkını yüklemek için giriş yap.",
        "nestsWord" to "yuva",
        "activeWord" to "aktif",
        "soonWord" to "yakında",
        "hatchedWord" to "çıktı",
        "beachWord" to "Plaj",
        "protectedBeachLabel" to "🛡️ Korunan yuvalama plajı",
        "protectedWord" to "korunan",
        "watch" to "🔔 Takip et",
        "watching" to "🔔 Takipte",
        "foundWord" to "bulundu",
        "foundByWord" to "Bulan",
        "hatchWindow" to "Çıkış aralığı",
        "predictedModel" to "tahmin · sıcaklık modeli",
        "exFullSun" to "tam güneş",
        "exPartial" to "yarı gölge",
        "exShade" to "gölge",
        "exUnknown" to "ışık ?",
        "airWord" to "hava",
        "rain7d" to "yağmur 7g",
        "timeline" to "Zaman çizelgesi · güncelleme & yorum",
        "addUpdate" to "＋ Güncelle",
        "comment" to "💬 Yorum",
        "excavation" to "⛏️ Kazı",
        "excavationLockedSub" to "Deneyimli gönüllüler ve plaj liderleri yapar — liderine sor.",
        "needsConfirm" to "⚠️  Fotoğraf / kesin konum gerekli — onaylamak için dokun",
        "condition" to "Durum",
        "dateWord" to "Tarih",
        "whatObserved" to "Ne gözlemledin?",
        "writeComment" to "Bir yorum yaz…",
        "condOk" to "OK",
        "condHatching" to "Çıkıyor",
        "condHatched" to "Çıktı",
        "condDisturbed" to "Rahatsız",
        "condPredated" to "Avlanmış",
        "condWashed" to "Su bastı",
        "nestFoundDate" to "Yuva buluş tarihi",
        "backDateHelp" to "Söylentiye göre daha önce bulundu ama fotoğraf şimdi mi geldi? Gerçek günü ayarla — bir aya kadar. İnkübasyon günü ve çıkış tahmini otomatik güncellenir.",
        "today" to "bugün",
        "yesterday" to "dün",
        "daysAgo" to "%d gün önce",
        "newMarker" to "Yeni işaret",
        "whatFound" to "Ne buldun?",
        "segNest" to "🥚 Yuva",
        "segLandmark" to "🚩 Nirengi",
        "segTrash" to "🧺 Çöp",
        "nestOrFalse" to "Yuva mı, boş çıkış mı?",
        "segFalseCrawl" to "🌀 Boş çıkış",
        "photo" to "Fotoğraf",
        "retake" to "📷 Tekrar çek",
        "cameraBtn" to "📷 Kamera",
        "gallery" to "🖼️ Galeri",
        "photoAdded" to "Fotoğraf eklendi",
        "location" to "Konum",
        "fromExif" to "Fotoğraftan · EXIF",
        "noGpsInPhoto" to "Fotoğrafta GPS yok — plaja bağlandı",
        "confirmed" to "Onaylandı ✓",
        "unconfirmed" to "Onaysız",
        "detectedFrom" to "Konumundan algılandı",
        "changePlain" to "Değiştir",
        "noLocationYet" to "Henüz konum yok — yerinde fotoğraf çek ya da plaj seç.",
        "notRecognizedBeach" to "🌊 Bilinen bir yuvalama plajı değil — doğrusunu seç.",
        "protection" to "Koruma",
        "protNone" to "Yok",
        "protReed" to "🌾 Kamış",
        "protCage" to "🛡️ Kafes",
        "protectionHelp" to "Kamış = kazık + kamış çit + bant + tabela. Kafes = metal kafes (daha güçlü ama nadir ve ağır).",
        "optionalSun" to "İsteğe bağlı: güneş",
        "segSun" to "☀️ Güneş",
        "segPartial" to "⛅ Yarı",
        "segShade" to "🌴 Gölge",
        "visibility" to "Görünürlük",
        "visPublic" to "🌍 Herkese açık",
        "visPrivate" to "🔒 Özel",
        "saveNest" to "Yuvayı kaydet 🐢",
        "saveFalseCrawl" to "Boş çıkışı kaydet 🌀",
        "trendsTitle" to "Eğilimler",
        "toSea" to "🐢 denize",
        "nestsFound14" to "Bulunan yuva · son 14 gün",
        "noNestsTrend" to "Henüz yuva yok — eğilimi görmek için ekle.",
        "hatchForecast8" to "Çıkış tahmini · önümüzdeki 8 hafta",
        "hatchForecastSub" to "İnkübe olan yuvaların ne zaman çıkmaya başlayacağı (buluş tarihi + ~inkübasyon).",
        "excavationOutcomes" to "Kazı sonuçları",
        "excavatedWord" to "kazıldı",
        "hatchedEggs" to "çıkan",
        "unhatchedWord" to "çıkmayan",
        "noExcavations" to "Henüz kazı yok.",
        "joinWhatsapp" to "💬 Katıl — bize WhatsApp'tan yaz",
        "joinSub" to "Yeni misin? WhatsApp veya Instagram'dan yaz — bir yönetici seni ekibe ekler.",
        "team" to "Ekip",
        "youWord" to "sen",
        "localGroupNear" to "Yerel grup · yakın",
        "localGroup" to "Yerel koruma grubu",
        "stubNotOnApp" to "🐢 Bu grup henüz Caretta Friends'te değil — aşağıdan doğrudan ulaş. ",
        "stubClaim" to "Bu grubun parçası mısın? Bize yaz, yöneticilerine erişim verelim.",
        "weRunGroup" to "🙋 Bu grubu biz yönetiyoruz — bize erişim verin",
        "reachThem" to "Ulaş",
        "topVolunteers" to "En iyi gönüllüler · yuvaya göre",
        "noNestsBeFirst" to "Henüz yuva yok — bu sezon ilk işareti sen koy 🥚",
        "kindCommunity" to "🐢 Gönüllü grubu",
        "kindNgo" to "🌍 STK",
        "kindUniversity" to "🎓 Üniversite",
        "kindOfficial" to "🏛️ Resmî",
        "roleAdmin" to "Yönetici",
        "roleLeader" to "Lider",
        "roleVolunteer" to "Gönüllü",
        "reachWhatsapp" to "WhatsApp'tan yaz",
        "callWord" to "Ara",
        "instagramWord" to "Instagram",
        "websiteWord" to "Web sitesi",
        "contactWord" to "İletişim",
        "protectedBeachBanner" to "🛡️ Resmî korunan yuvalama plajı — sezonda gece girişi yasak.",
        "unprotectedBeachBanner" to "🏖️ Resmî korunan plaj değil — buradaki yuvalar yine de sayılır ve senkronize olur.",
        "beachLeader" to "plaj lideri",
        "feed" to "Akış",
        "noNestsHereBeFirst" to "Burada henüz yuva yok — ilk sen ol 🥚",
        "dayWord" to "Gün",
        "airCleanLabel" to "Hava temiz",
        "airModerate" to "Orta hava",
        "airUnhealthy" to "Sağlıksız",
        "airDustLabel" to "Toz",
        "patrolNotAdvised" to "devriye önerilmez",
        "dustNotAdvised" to "⚠ Toz — devriye önerilmez",
        "statusIncubating" to "İnkübasyon",
        "statusHatchingSoon" to "Yakında çıkış",
        "statusHatched" to "Çıktı",
        "statusExcavated" to "Kazıldı",
        "statusPredated" to "Avlandı",
        "statusWashedOver" to "Su bastı",
        "statusPoached" to "Yağmalandı",
        "statusLost" to "Kayıp",
        "statusFalseCrawl" to "Boş çıkış",
        "dayLabel" to "GÜN",
        "ob1Title" to "Caretta Friends'e hoş geldin 🐢",
        "ob1Body" to "Deniz kaplumbağası (caretta) yuvalarını birlikte koruyoruz — her plaj, her yuva, her yavru için.",
        "ob2Title" to "Yuva mı buldun? Ekle 📍",
        "ob2Body" to "Plajda bir fotoğraf çek ve yuvayı işaretle. Sonra devriye gez, güncelleme ekle ve yavrular denize ulaşana kadar çıkış tahminini izle.",
        "ob3Title" to "Her devriye önemli 🙌",
        "ob3Body" to "Yerel topluluğuna katıl, geceleri plajı karanlık tut ve yüzlerce yavrunun dalgalara ulaşmasına yardım et. Hazır mısın?",
        "obNext" to "İleri",
        "obStart" to "Hadi başlayalım 🌊",
        "obSkip" to "Atla",
        "segViolation" to "⛔ İhlal",
        "whatViolation" to "İhlal nedir?",
        "vkTent" to "⛺ Çadır",
        "vkVehicle" to "🚗 Araç",
        "vkLight" to "🔦 Işık",
        "vkNoise" to "🔊 Gürültü",
        "vkDog" to "🐕 Köpek",
        "vkLitter" to "🧺 Çöp",
        "vkOther" to "⚠ Diğer",
        "violationPrivateHint" to "Kayıt için gizli tutulur — misafirler kimin bildirdiğini görmez.",
        "anonymousWord" to "Anonim bildir",
        "saveViolation" to "İhlal bildir ⛔",
        "filterViolations" to "⛔ İhlaller",
        "timelapse" to "🎞️ Zaman akışı",
        "shareImpact" to "📣 Katkımı paylaş",
        "shareText" to "🐢 Ben bir Caretta Friends gönüllüsüyüm — %d yavru denize ulaştı! Sen de katıl: carettafriends.com",
        "addPhoto" to "📷 Fotoğraf",
        "dupTitle" to "Burada zaten var mı?",
        "dupBody" to "%1\$s zaten %2\$s ötede işaretli. Kopya eklemek yerine onu aç?",
        "dupOpenExisting" to "%1\$s'i aç",
        "dupAddNew" to "Hayır, yeni yuva",
        "excavationTitle" to "Yuva kazısı",
        "excFinalCount" to "son sayım",
        "excCountWhatYouFind" to "Bulduklarını say",
        "excAuthNote" to "Yuva boşaltımı %s ile koordineli ve yetkili ekiple yapılır; sayım resmî tutanağa geçer, halka açık bir etkinlik değildir.",
        "authorityGeneric" to "yerel koruma yetkilisi",
        "excShells" to "Çıkmış / boş kabuk",
        "excUnhatched" to "Çıkmamış / bütün yumurta",
        "excPipped" to "Gagalamış / yumurtada",
        "excInNest" to "Yuvada / sıkışmış",
        "excHelpedOut" to "Kurtarıldı / çıkarıldı",
        "excSuccessLabel" to "Başarı",
        "excHatchingSuccess" to "Çıkış başarısı",
        "excEmergenceSuccess" to "Yüzeye çıkış başarısı",
        "excFinish" to "Bitir & kutla 🎉",
        "nestNotFound" to "Yuva bulunamadı",
        "beachNotFound" to "Plaj bulunamadı",
        "noBeachesInCity" to "%s'de henüz plaj yok.",
        "openWord" to "Aç",
        "yourCommunity" to "Topluluğun",
        "volunteersWord" to "gönüllü",
        "cameraFrameHint" to "Yuvayı çerçevele",
        "badgeFirstNest" to "İlk yuva",
        "badgeFirstDig" to "İlk kazı",
        "badgeRescuer" to "Kurtarıcı",
        "badgeSeason50" to "Sezon 50",
        "patrolPublishTitle" to "Bu devriyeyi yayınla?",
        "patrolKeepPrivate" to "Özel tut",
        "patrolPublish" to "Yayınla",
        "patrolPublishBody" to "Yürüyüşün telefonunda kaydedildi. Rotayı toplulukla paylaşmak için yayınla — canlı konumun asla paylaşılmaz.",
        "openSettings" to "Ayarları aç",
        "cameraStarting" to "Kamera başlatılıyor…",
        "cameraDenied" to "Kamera erişimi reddedildi. Ayarlar'dan etkinleştirin.",
        "memberNestsStat" to "yuva bulundu",
        "memberHatchStat" to "yavru kurtarıldı",
        "memberNestsSection" to "Yuvalar",
        "memberNoNests" to "Henüz yuva yok.",
        "memberLink" to "Bağlantı",
        "tlNestFound" to "Yuva bulundu",
        "tlFalseCrawl" to "Yanlış çıkış kaydedildi",
        "tlHatchSuccess" to "çıkış başarısı",
        "onMapWord" to "Haritada",
        "awayWord" to "uzakta",
        "condPoached" to "yağmalandı",
        "hatchWindowOpen" to "Çıkış zamanı 🐣",
        "impactZeroCta" to "İlk yuvanı işaretle, katkın başlasın 🐣",
        "excZeroHint" to "Bulduklarını aşağıda say ↓",
        "excThanksRecord" to "Her kayıt önemli — bu yuvayı kaydettiğin için teşekkürler 🐢",
        "activityTitle" to "Etkinlik",
        "rangeWeek" to "Hafta",
        "rangeMonth" to "Ay",
        "shareDay" to "Bugünü paylaş",
        "activityEmpty" to "Bu dönem için henüz bir şey yok.",
        "moreDetails" to "Daha fazla",
        "addPhotoBtn" to "📷 Fotoğraf ekle",
        "noteHint" to "Not ekle (isteğe bağlı)",
    ),
    months = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"),
)

// Built lazily via functions (NOT giant top-level `val`s): a 245-field data class constructed 3× in
// one <clinit> overflowed the Android/ART verifier (VerifyError on launch). Each bundle now builds in
// its own method on first use. ⚠️ AppStrings is near the JVM 255-parameter limit — split into
// sub-objects before adding many more fields.
private val EN: AppStrings by lazy { enStrings() }
private val RU: AppStrings by lazy { ruStrings() }
private val TR: AppStrings by lazy { trStrings() }

fun appStrings(lang: String): AppStrings = when (lang.lowercase()) {
    "ru" -> RU
    "tr" -> TR
    else -> EN
}

/** Localized display name for a badge by its stable [code]; unknown codes fall back to the code.
 *  Lives next to the string bundles so a new badge only needs its fields + one branch here. */
fun AppStrings.badgeName(code: String): String = when (code) {
    "first_nest" -> badgeFirstNest
    "first_dig" -> badgeFirstDig
    "rescuer" -> badgeRescuer
    "season50" -> badgeSeason50
    else -> code
}
