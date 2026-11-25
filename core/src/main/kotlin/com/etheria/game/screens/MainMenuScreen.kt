package com.etheria.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.etheria.game.EtheriaGame

class MainMenuScreen(private val game: EtheriaGame) : Screen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT, camera)
    private val font = EtheriaGame.createSmoothFont(1.35f)
    private val titleFont = EtheriaGame.createSmoothFont(2.6f)
    private val subFont = EtheriaGame.createSmoothFont(1.1f)
    private val bgTex: Texture? = if (game.assetManager.isLoaded("sprites/world/background.png")) {
        game.assetManager.get("sprites/world/background.png", Texture::class.java)
    } else null

    private data class MenuItem(val label: String, val x: Float, val y: Float, val enabled: Boolean = true)

    private val buttonW = 340f
    private val buttonH = 58f
    private val hasSave = game.saveManager.hasSave()
    private val cx = EtheriaGame.VIRTUAL_WIDTH / 2f
    private val menuItems = listOf(
        MenuItem("NEW ADVENTURE", cx - buttonW / 2f, 380f, true),
        MenuItem(if (hasSave) "CONTINUE SAVE" else "NO SAVE FOUND", cx - buttonW / 2f, 304f, hasSave),
        MenuItem("SETTINGS", cx - buttonW / 2f, 228f, true),
        MenuItem("QUIT", cx - buttonW / 2f, 152f, true)
    )
    private var selectedIndex = 0
    private val touchVec = Vector2()

    init {
        camera.setToOrtho(false, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT)
        game.audioManager.playMusic("sounds/music/exploration")
    }

    override fun show() {}

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.03f, 0.04f, 0.09f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.update()
        game.batch.projectionMatrix = camera.combined
        game.shapeRenderer.projectionMatrix = camera.combined

        game.batch.begin()
        bgTex?.let { game.batch.draw(it, 0f, 0f, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT) }
        game.batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        menuItems.forEachIndexed { i, item ->
            when {
                !item.enabled -> game.shapeRenderer.setColor(0.12f, 0.14f, 0.2f, 0.55f)
                i == selectedIndex -> game.shapeRenderer.setColor(0.18f, 0.45f, 0.85f, 0.9f)
                else -> game.shapeRenderer.setColor(0.08f, 0.14f, 0.28f, 0.82f)
            }
            game.shapeRenderer.rect(item.x, item.y, buttonW, buttonH)
        }
        game.shapeRenderer.end()

        game.batch.begin()
        titleFont.color = Color(0.48f, 0.85f, 1f, 1f)
        titleFont.draw(game.batch, "ETHERIA", cx - 138f, 610f)
        subFont.color = Color(0.82f, 0.9f, 1f, 0.95f)
        subFont.draw(game.batch, "Echoes of Eternity  •  Native Android Action RPG", cx - 220f, 540f)

        menuItems.forEach { item ->
            font.color = if (item.enabled) Color.WHITE else Color.GRAY
            font.draw(game.batch, item.label, item.x + 28f, item.y + 38f)
        }
        game.batch.end()

        // Touch selection (strictly check bounds of touched button!)
        if (Gdx.input.justTouched()) {
            touchVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            viewport.unproject(touchVec)
            menuItems.forEachIndexed { i, item ->
                if (item.enabled &&
                    touchVec.x in item.x..(item.x + buttonW) &&
                    touchVec.y in item.y..(item.y + buttonH)
                ) {
                    activateItem(i)
                    return
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) selectedIndex = (selectedIndex + 1) % menuItems.size
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) selectedIndex = (selectedIndex - 1 + menuItems.size) % menuItems.size
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (menuItems[selectedIndex].enabled) activateItem(selectedIndex)
        }
    }

    private fun activateItem(index: Int) {
        game.audioManager.playSound("ui_click")
        when (index) {
            0 -> {
                game.screen = GameScreen(game, false)
                dispose()
            }
            1 -> {
                game.screen = GameScreen(game, true)
                dispose()
            }
            2 -> {
                game.screen = SettingsScreen(game)
                dispose()
            }
            3 -> Gdx.app.exit()
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
        subFont.dispose()
    }
}
