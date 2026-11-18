package uz.xumoyun.feature.prayer.domain

enum class Prayer {
    FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA;

    /** Quyosh chiqishi namoz emas, lekin ro'yxatda va teskari sanoqda ko'rsatiladi. */
    val isPrayer: Boolean get() = this != SUNRISE
}

enum class AsrMadhab { SHAFI, HANAFI }

/** Shimoliy kengliklarda Bomdod/Xufton aniqlanmaganda qo'llanadigan qoida. */
enum class HighLatRule { MIDDLE_OF_THE_NIGHT, SEVENTH_OF_THE_NIGHT, TWILIGHT_ANGLE }

/**
 * Hisoblash usullari. Burchaklar va tuzatishlar adhan kutubxonasi va
 * rasmiy idoralar e'lon qilgan parametrlar asosida.
 */
enum class CalcMethod {
    MUSLIM_WORLD_LEAGUE,
    UMM_AL_QURA,
    DUBAI,
    KUWAIT,
    QATAR,
    EGYPTIAN,
    KARACHI,
    TURKEY_DIYANET,
    UZBEKISTAN,
    SINGAPORE,
    NORTH_AMERICA,
    MOONSIGHTING_COMMITTEE,
}
