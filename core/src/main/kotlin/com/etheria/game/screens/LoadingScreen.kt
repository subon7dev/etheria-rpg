package com.etheria.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import com.etheria.game.EtheriaGame

class LoadingScreen(private val game: EtheriaGame) : Screen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT, camera)
    private val font = EtheriaGame.createSmoothFont(1.35f)
    private val titleFont = EtheriaGame.createSmoothFont(2.4f)
    private var splashTex: Texture? = null
    private var loaded = false

    init {
        camera.setToOrtho(false, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT)
        if (Gdx.files.internal("sprites/ui/splash.png").exists()) {
            splashTex = Texture(Gdx.files.internal("sprites/ui/splash.png"))
        }
        queueAssets()
    }

    private fun queueAssets() {
        val am = game.assetManager
        listOf(
            "sprites/player/kai.png",
            "sprites/player/lyra.png",
            "sprites/player/zara.png",
            "sprites/player/ragna.png",
            "sprites/enemies/slime.png",
            "sprites/enemies/goblin.png",
            "sprites/enemies/skeleton.png",
            "sprites/enemies/archer_enemy.png",
            "sprites/enemies/flying_monster.png",
            "sprites/enemies/darkKnight.png",
            "sprites/enemies/elite_golem.png",
            "sprites/enemies/shadow_lord.png",
            "sprites/world/tileset.png",
            "sprites/world/background.png",
            "sprites/ui/hud.png",
            "sprites/effects/particles.png"
        ).forEach { path ->
            if (Gdx.files.internal(path).exists()) {
                am.load(path, Texture::class.java)
            }
        }
    }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.04f, 0.05f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        game.shapeRenderer.projectionMatrix = camera.combined

        val progress = game.assetManager.progress
        val w = EtheriaGame.VIRTUAL_WIDTH
        val h = EtheriaGame.VIRTUAL_HEIGHT

        game.batch.begin()
        splashTex?.let { game.batch.draw(it, 0f, 0f, w, h) }
        titleFont.color = Color(0.5f, 0.85f, 1f, 1f)
        titleFont.draw(game.batch, "ETHERIA", w * 0.38f, h * 0.64f)
        font.color = Color(0.85f, 0.92f, 1f, 1f)
        font.draw(game.batch, "Echoes of Eternity", w * 0.41f, h * 0.54f)
        font.draw(game.batch, "Loading Assets... ${(progress * 100).toInt()}%", w * 0.41f, h * 0.36f)
        game.batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.14f, 0.18f, 0.28f, 0.9f)
        game.shapeRenderer.rect(w * 0.25f, h * 0.27f, w * 0.5f, 22f)
        game.shapeRenderer.setColor(0.32f, 0.78f, 1f, 1f)
        game.shapeRenderer.rect(w * 0.25f, h * 0.27f, w * 0.5f * progress, 22f)
        game.shapeRenderer.end()

        if (game.assetManager.update() && !loaded) {
            loaded = true
            game.audioManager.loadSounds()
            game.screen = MainMenuScreen(game)
            dispose()
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
        titleFont.dispose()
        splashTex?.dispose()
    }
}
