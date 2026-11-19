package uz.xumoyun.feature.hifz.ui.lesson

/**
 * Dars pleyeri uchun tor interfeys (ViewModel'ni ExoPlayer'dan ajratadi, testda fake bilan almashtiriladi).
 * Amalga oshirish: [AudioLessonPlayer] (AyahAudioPlayer ustida).
 */
interface LessonAudio {
    /** Oyatni [times] marta ijro etadi. false — fayl yuklanmagan (ijro boshlanmadi). */
    fun play(surah: Int, ayah: Int, times: Int, speed: Float): Boolean

    fun setSpeed(speed: Float)

    fun pause()

    fun release()
}
