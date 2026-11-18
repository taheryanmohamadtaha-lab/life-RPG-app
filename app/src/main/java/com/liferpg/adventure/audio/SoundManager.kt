package com.liferpg.adventure.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import com.liferpg.adventure.R

enum class Cue { CLICK, COMPLETE, COIN, LEVEL_UP, ACHIEVEMENT }

/**
 * Original, synthesised sounds (see tools/generate_audio.py). Everything respects the user's switches AND the
 * phone's silent / vibrate mode: nothing is played unless the ringer is in normal mode.
 */
class SoundManager(private val context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ids = HashMap<Cue, Int>()
    private val loaded = HashSet<Int>()
    private var player: MediaPlayer? = null

    @Volatile var sfxEnabled: Boolean = true
    private var musicWanted = false
    private var foreground = true

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) synchronized(loaded) { loaded.add(sampleId) } }
        ids[Cue.CLICK] = pool.load(context, R.raw.sfx_click, 1)
        ids[Cue.COMPLETE] = pool.load(context, R.raw.sfx_complete, 1)
        ids[Cue.COIN] = pool.load(context, R.raw.sfx_coin, 1)
        ids[Cue.LEVEL_UP] = pool.load(context, R.raw.sfx_levelup, 1)
        ids[Cue.ACHIEVEMENT] = pool.load(context, R.raw.sfx_achievement, 1)
    }

    private fun silent(): Boolean = audio.ringerMode != AudioManager.RINGER_MODE_NORMAL

    fun play(cue: Cue) {
        if (!sfxEnabled || silent()) return
        val id = ids[cue] ?: return
        val ready = synchronized(loaded) { id in loaded }
        if (ready) pool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicWanted = enabled
        refreshMusic()
    }

    fun setForeground(fg: Boolean) {
        foreground = fg
        refreshMusic()
    }

    private fun refreshMusic() {
        val shouldPlay = musicWanted && foreground && !silent()
        try {
            if (shouldPlay) {
                val p = player ?: MediaPlayer.create(context, R.raw.bgm_loop)?.also {
                    it.isLooping = true
                    it.setVolume(0.35f, 0.35f)
                    player = it
                }
                if (p != null && !p.isPlaying) p.start()
            } else {
                player?.let { if (it.isPlaying) it.pause() }
            }
        } catch (_: IllegalStateException) {
            player?.release()
            player = null
        }
    }

    fun release() {
        player?.release(); player = null
        pool.release()
    }
}
