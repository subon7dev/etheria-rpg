package com.etheria.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.etheria.game.EtheriaGame
import com.etheria.game.GameSettings
import com.etheria.game.GraphicsQuality

class SettingsScreen(private val game: EtheriaGame) : Screen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT, camera)
    private val font = EtheriaGame.createSmoothFont(1.3f)
    private val touchVec = Vector2()

    private val btnX = 360f
    private val btnW = 560f
    private val btnH = 54f

    init {
        camera.setToOrtho(false, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT)
    }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.11f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        game.shapeRenderer.projectionMatrix = camera.combined

        val rows = listOf(
            "FPS Target: ${GameSettings.targetFps} FPS" to 500f,
            "Graphics Quality: ${GameSettings.graphicsQuality.name}" to 430f,
            "Music Volume: ${(GameSettings.musicVolume * 100).toInt()}%" to 360f,
            "SFX Volume: ${(GameSettings.sfxVolume * 100).toInt()}%" to 290f,
            "Virtual Controls Scale: ${String.format("%.2fx", GameSettings.controlsScale)}" to 220f,
            "BACK TO MAIN MENU" to 120f
        )

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        rows.forEachIndexed { idx, (_, y) ->
            if (idx == rows.lastIndex) {
                game.shapeRenderer.setColor(0.2f, 0.5f, 0.35f, 0.92f)
            } else {
                game.shapeRenderer.setColor(0.12f, 0.18f, 0.34f, 0.9f)
            }
            game.shapeRenderer.rect(btnX, y, btnW, btnH)
        }
        game.shapeRenderer.end()

        game.batch.begin()
        font.color = Color(0.55f, 0.85f, 1f, 1f)
        font.draw(game.batch, "SETTINGS (Tap any option)", btnX + 85f, 620f)
        font.color = Color.WHITE
        rows.forEach { (label, y) ->
            font.draw(game.batch, label, btnX + 24f, y + 36f)
        }
        game.batch.end()

        if (Gdx.input.justTouched()) {
            touchVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            viewport.unproject(touchVec)
            if (touchVec.x in btnX..(btnX + btnW)) {
                when {
                    touchVec.y in 500f..(500f + btnH) -> {
                        GameSettings.targetFps = if (GameSettings.targetFps == 60) 30 else 60
                        GameSettings.save()
                        game.audioManager.playSound("ui_click")
                    }
                    touchVec.y in 430f..(430f + btnH) -> {
                        val v = GraphicsQuality.values()
                        GameSettings.graphicsQuality = v[(GameSettings.graphicsQuality.ordinal + 1) % v.size]
                        GameSettings.save()
                        game.audioManager.playSound("ui_click")
                    }
                    touchVec.y in 360f..(360f + btnH) -> {
                        GameSettings.musicVolume = ((GameSettings.musicVolume + 0.25f) % 1.25f).coerceIn(0f, 1f)
                        if (GameSettings.musicVolume < 0.05f) GameSettings.musicVolume = 0f
                        game.audioManager.setMusicVolume(GameSettings.musicVolume)
                        GameSettings.save()
                        game.audioManager.playSound("ui_click")
                    }
                    touchVec.y in 290f..(290f + btnH) -> {
                        GameSettings.sfxVolume = ((GameSettings.sfxVolume + 0.25f) % 1.25f).coerceIn(0f, 1f)
                        if (GameSettings.sfxVolume < 0.05f) GameSettings.sfxVolume = 0f
                        game.audioManager.setSfxVolume(GameSettings.sfxVolume)
                        GameSettings.save()
                        game.audioManager.playSound("ui_click")
                    }
                    touchVec.y in 220f..(220f + btnH) -> {
                        GameSettings.controlsScale = when {
                            GameSettings.controlsScale < 0.95f -> 1.0f
                            GameSettings.controlsScale < 1.1f -> 1.15f
                            else -> 0.85f
                        }
                        GameSettings.save()
                        game.audioManager.playSound("ui_click")
                    }
                    touchVec.y in 120f..(120f + btnH) -> {
                        game.audioManager.playSound("ui_click")
                        game.screen = MainMenuScreen(game)
                        dispose()
                    }
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        font.dispose()
    }
}
