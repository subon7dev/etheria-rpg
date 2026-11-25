package com.etheria.game.audio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound

class AudioManager {

    private val sounds = mutableMapOf<String, Sound>()
    private var bgMusic: Music? = null
    private var musicVolume = 0.7f
    private var sfxVolume = 1.0f
    private var currentMusicPath = ""
    private var loaded = false

    fun loadSounds() {
        if (loaded) return
        loaded = true
        val sfxNames = listOf(
            "attack_sword", "attack_bow", "attack_magic", "attack_heavy",
            "skill", "ultimate", "enemy_hit", "enemy_death", "player_hurt",
            "chest_open", "level_up", "footstep", "dash", "jump", "ui_click", "puzzle_solve"
        )
        sfxNames.forEach { name ->
            val wavPath = "sounds/sfx/$name.wav"
            val oggPath = "sounds/sfx/$name.ogg"
            val path = when {
                Gdx.files.internal(wavPath).exists() -> wavPath
                Gdx.files.internal(oggPath).exists() -> oggPath
                else -> null
            }
            if (path != null) {
                try {
                    sounds[name] = Gdx.audio.newSound(Gdx.files.internal(path))
                } catch (e: Exception) {
                    Gdx.app.log("AudioManager", "Could not load $path: ${e.message}")
                }
            }
        }
    }

    fun playMusic(basePathWithoutExt: String, loop: Boolean = true) {
        val resolved = when {
            Gdx.files.internal(basePathWithoutExt).exists() -> basePathWithoutExt
            Gdx.files.internal("$basePathWithoutExt.wav").exists() -> "$basePathWithoutExt.wav"
            Gdx.files.internal("$basePathWithoutExt.ogg").exists() -> "$basePathWithoutExt.ogg"
            else -> return
        }
        if (currentMusicPath == resolved) return
        try {
            bgMusic?.stop()
            bgMusic?.dispose()
            bgMusic = Gdx.audio.newMusic(Gdx.files.internal(resolved)).apply {
                volume = musicVolume
                isLooping = loop
                play()
            }
            currentMusicPath = resolved
        } catch (e: Exception) {
            Gdx.app.log("AudioManager", "Could not play music $resolved: ${e.message}")
        }
    }

    fun playSound(name: String, volume: Float = 1f, pitch: Float = 1f) {
        if (sfxVolume <= 0.01f) return
        sounds[name]?.play((sfxVolume * volume).coerceIn(0f, 1f), pitch, 0f)
    }

    fun setMusicVolume(v: Float) {
        musicVolume = v.coerceIn(0f, 1f)
        bgMusic?.volume = musicVolume
    }

    fun setSfxVolume(v: Float) {
        sfxVolume = v.coerceIn(0f, 1f)
    }

    fun getMusicVolume() = musicVolume
    fun getSfxVolume() = sfxVolume

    fun stopMusic() {
        bgMusic?.stop()
        currentMusicPath = ""
    }

    fun pauseMusic() {
        bgMusic?.pause()
    }

    fun resumeMusic() {
        bgMusic?.play()
    }

    fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
        bgMusic?.dispose()
        bgMusic = null
    }
}
