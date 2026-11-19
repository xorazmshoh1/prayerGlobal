package uz.xumoyun.core.common.text

/**
 * Raqamlar locale bo'yicha (TZ 9-bo'lim): arab tilida sharqiy arab raqamlari (٠–٩), fors va urduda fors raqamlari
 * (۰–۹), boshqalarda lotin raqamlari. Oyat raqamlari doim arabcha — bu yerda emas, Qur'on matnida.
 */
object LocalDigits {
    private const val ARABIC_INDIC = "٠١٢٣٤٥٦٧٨٩"
    private const val EXTENDED_ARABIC_INDIC = "۰۱۲۳۴۵۶۷۸۹"

    /** Til kodi (masalan "ar", "fa-IR", "ur") bo'yicha raqam jadvali; boshqa tillarda o'zgarishsiz. */
    fun forLanguage(language: String): (String) -> String {
        val lang = language.substringBefore('-').substringBefore('_').lowercase()
        return when (lang) {
            "ar" -> { s -> map(s, ARABIC_INDIC) }
            "fa", "ur" -> { s -> map(s, EXTENDED_ARABIC_INDIC) }
            else -> { s -> s }
        }
    }

    fun map(text: String, digits: String): String = buildString(text.length) {
        for (c in text) if (c in '0'..'9') append(digits[c - '0']) else append(c)
    }
}
