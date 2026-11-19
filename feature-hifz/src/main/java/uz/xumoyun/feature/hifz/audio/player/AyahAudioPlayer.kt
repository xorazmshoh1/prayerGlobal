package uz.xumoyun.feature.hifz.audio.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import uz.xumoyun.feature.hifz.audio.data.AudioStore
import uz.xumoyun.feature.hifz.audio.domain.PlaybackSpeed
import uz.xumoyun.feature.hifz.audio.domain.RepeatCounter
import uz.xumoyun.feature.hifz.audio.domain.RepeatRange

/**
 * Oyat audiosini mahalliy fayllardan ijro etadi (internetsiz). Takror: [RepeatRange] oraliqni
 * [RepeatRange.times] marta o'ynaydi (bitta oyat ham oraliq sifatida). Tezlik 0.75–1.25 ga chegaralanadi.
 *
 * Kompilyatsiya qilinmagan: Media3 Google Maven'dan keladi; sof qoidalar ([RepeatCounter], [PlaybackSpeed])
 * JVM testida tekshirilgan.
 */
class AyahAudioPlayer(context: Context, private val store: AudioStore) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var counter: RepeatCounter? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_ENDED) return
            val c = counter ?: return
            if (c.onPassEnded()) {
                player.seekTo(0, 0L)
                player.play()
            }
        }
    }

    init {
        player.addListener(listener)
    }

    /** Oraliqni ijro etadi. Yuklanmagan oyat bo'lsa, Media3 xato holatini beradi (UI tekshiradi). */
    fun play(reciterKey: String, surah: Int, range: RepeatRange, speed: Float) {
        val items = range.ayahs.map { ayah ->
            MediaItem.fromUri(Uri.fromFile(store.ayahFile(reciterKey, surah, ayah)))
        }
        counter = RepeatCounter(range.times)
        player.setMediaItems(items)
        player.setPlaybackSpeed(PlaybackSpeed.clamp(speed))
        player.prepare()
        player.play()
    }

    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(PlaybackSpeed.clamp(speed))
    }

    fun pause() = player.pause()

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}
