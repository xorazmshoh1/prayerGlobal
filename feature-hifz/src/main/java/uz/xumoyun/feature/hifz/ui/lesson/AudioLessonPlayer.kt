package uz.xumoyun.feature.hifz.ui.lesson

import android.content.Context
import uz.xumoyun.feature.hifz.audio.data.AudioStore
import uz.xumoyun.feature.hifz.audio.domain.RepeatRange
import uz.xumoyun.feature.hifz.audio.player.AyahAudioPlayer

/**
 * [LessonAudio] ning AyahAudioPlayer ustidagi amalga oshirishi. Pleyer birinchi ijroda yaratiladi
 * (ExoPlayer darhol ochilmasin). Yuklanmagan oyat uchun ijro boshlanmaydi.
 */
class AudioLessonPlayer(
    private val context: Context,
    private val store: AudioStore,
    private val reciterKey: String,
) : LessonAudio {

    private var player: AyahAudioPlayer? = null

    private fun player(): AyahAudioPlayer = player ?: AyahAudioPlayer(context, store).also { player = it }

    override fun play(surah: Int, ayah: Int, times: Int, speed: Float): Boolean {
        if (reciterKey.isEmpty() || !store.ayahFile(reciterKey, surah, ayah).exists()) return false
        player().play(reciterKey, surah, RepeatRange(ayah, ayah, times), speed)
        return true
    }

    override fun setSpeed(speed: Float) {
        player?.setSpeed(speed)
    }

    override fun pause() {
        player?.pause()
    }

    override fun release() {
        player?.release()
        player = null
    }
}
