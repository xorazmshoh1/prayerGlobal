package uz.xumoyun.feature.hifz.domain

/**
 * Mus'haf uchun sof (Android'siz) yordamchi funksiyalar: qidiruv, oyat raqami, shrift o'lchami.
 * TZ 5-bosqich / 9-bo'lim: Qur'on matni 20–40 sp, sukut 26 sp; oyat raqamlari doim arabcha.
 */
object QuranCatalog {

    const val SURAH_COUNT = 114
    const val JUZ_COUNT = 30
    const val FONT_MIN_SP = 20f
    const val FONT_MAX_SP = 40f
    const val FONT_DEFAULT_SP = 26f
    const val FONT_STEP_SP = 2f

    /** 1..30 juz raqamlari. */
    val juzNumbers: IntRange = 1..JUZ_COUNT

    /**
     * Sura qidiruvi. Bo'sh so'rov — hammasi. Faqat raqam — sura raqami bo'yicha aniq moslik.
     * Aks holda lotin nomi (apostrof, tire va bo'shliqlarsiz, katta-kichik harfsiz) yoki arabcha nom
     * (harakat va tatvil e'tiborga olinmaydi) bo'yicha qism moslik.
     */
    fun filterSurahs(all: List<SurahInfo>, query: String): List<SurahInfo> {
        val q = query.trim()
        if (q.isEmpty()) return all
        if (q.all { it.isDigit() }) {
            val n = q.toIntOrNull() ?: return emptyList()
            return all.filter { it.id == n }
        }
        val latin = latinKey(q)
        val arabic = normalizeArabic(q)
        if (latin.isEmpty() && arabic.isEmpty()) return emptyList()
        return all.filter { s ->
            (latin.isNotEmpty() && latinKey(s.nameTranslit).contains(latin)) ||
                (arabic.isNotEmpty() && normalizeArabic(s.nameArabic).contains(arabic))
        }
    }

    /** Lotin qidiruv kaliti: kichik harf, faqat harf va raqamlar. */
    internal fun latinKey(s: String): String =
        buildString { for (c in s.lowercase()) if (c.isLetterOrDigit()) append(c) }

    /**
     * Arabcha qidiruv kaliti: harakat (U+064B–U+065F, U+0670) va tatvil (U+0640) olib tashlanadi,
     * alif variantlari (أ إ آ ٱ) oddiy alifga keltiriladi.
     */
    fun normalizeArabic(s: String): String = buildString {
        for (c in s) {
            when {
                c in 'ً'..'ٟ' || c == 'ٰ' || c == 'ـ' -> Unit
                c == 'أ' || c == 'إ' || c == 'آ' || c == 'ٱ' -> append('ا')
                c.isWhitespace() -> Unit
                else -> append(c)
            }
        }
    }

    /** 0..9 raqamlarini arab-hind (U+0660..U+0669) raqamlariga aylantiradi. */
    fun arabicIndic(n: Int): String {
        require(n >= 0) { "manfiy son: $n" }
        return n.toString().map { '٠' + (it - '0') }.joinToString("")
    }

    /** Oyat belgisi: ornamental qavslar ichida arabcha raqam, masalan ﴿١﴾. */
    fun ayahMarker(numberInSurah: Int): String = "﴿${arabicIndic(numberInSurah)}﴾"

    /** Shrift o'lchamini [FONT_MIN_SP]..[FONT_MAX_SP] oralig'ida ushlaydi. */
    fun clampFontSp(sp: Float): Float = sp.coerceIn(FONT_MIN_SP, FONT_MAX_SP)

    /** Bir qadam kattalashtirish yoki kichraytirish, oraliqdan chiqmaydi. */
    fun stepFontSp(current: Float, larger: Boolean): Float =
        clampFontSp(current + if (larger) FONT_STEP_SP else -FONT_STEP_SP)
}
