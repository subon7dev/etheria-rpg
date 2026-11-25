package com.etheria.game

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.etheria.game.audio.AudioManager
import com.etheria.game.save.SaveManager
import com.etheria.game.screens.LoadingScreen

enum class GraphicsQuality { LOW, MEDIUM, HIGH }

object GameSettings {
    var targetFps: Int = 60
    var graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH
    var musicVolume: Float = 0.7f
    var sfxVolume: Float = 1.0f
    var controlsScale: Float = 1.0f

    private const val PREF_NAME = "etheria_settings"

    fun load() {
        val prefs: Preferences = Gdx.app.getPreferences(PREF_NAME)
        targetFps = prefs.getInteger("targetFps", 60).let { if (it == 30) 30 else 60 }
        graphicsQuality = try {
            GraphicsQuality.valueOf(prefs.getString("quality", "HIGH"))
        } catch (_: Exception) {
            GraphicsQuality.HIGH
        }
        musicVolume = prefs.getFloat("musicVolume", 0.7f).coerceIn(0f, 1f)
        sfxVolume = prefs.getFloat("sfxVolume", 1.0f).coerceIn(0f, 1f)
        controlsScale = prefs.getFloat("controlsScale", 1.0f).coerceIn(0.8f, 1.3f)
        Gdx.graphics.setForegroundFPS(targetFps)
    }

    fun save() {
        val prefs: Preferences = Gdx.app.getPreferences(PREF_NAME)
        prefs.putInteger("targetFps", targetFps)
        prefs.putString("quality", graphicsQuality.name)
        prefs.putFloat("musicVolume", musicVolume)
        prefs.putFloat("sfxVolume", sfxVolume)
        prefs.putFloat("controlsScale", controlsScale)
        prefs.flush()
        Gdx.graphics.setForegroundFPS(targetFps)
    }
}

class EtheriaGame : Game() {

    lateinit var batch: SpriteBatch
    lateinit var shapeRenderer: ShapeRenderer
    lateinit var assetManager: AssetManager
    lateinit var audioManager: AudioManager
    lateinit var saveManager: SaveManager

    companion object {
        const val VIRTUAL_WIDTH = 1280f
        const val VIRTUAL_HEIGHT = 720f
        const val TILE_SIZE = 32
        const val PPM = 32f

        fun createSmoothFont(scale: Float = 1f): BitmapFont {
            return BitmapFont().apply {
                region.texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                data.setScale(scale)
            }
        }
    }

    override fun create() {
        GameSettings.load()
        batch = SpriteBatch()
        shapeRenderer = ShapeRenderer()
        assetManager = AssetManager()
        audioManager = AudioManager().apply {
            setMusicVolume(GameSettings.musicVolume)
            setSfxVolume(GameSettings.sfxVolume)
        }
        saveManager = SaveManager()

        setScreen(LoadingScreen(this))
    }

    override fun dispose() {
        screen?.dispose()
        batch.dispose()
        shapeRenderer.dispose()
        assetManager.dispose()
        audioManager.dispose()
    }
}
