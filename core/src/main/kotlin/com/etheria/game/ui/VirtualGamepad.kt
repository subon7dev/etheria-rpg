package com.etheria.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.FitViewport
import com.etheria.game.EtheriaGame
import com.etheria.game.GameSettings
import com.etheria.game.player.PartyManager

class VirtualGamepad {

    private val baseJoystickCenter = Vector2(130f, 130f)
    private var joystickRadius = 82f
    private var knobRadius = 34f
    private val joystickKnob = Vector2(130f, 130f)
    private var joystickPointer = -1

    val joystickDir = Vector2()

    data class ActionButton(
        val label: String,
        var x: Float,
        var y: Float,
        var r: Float = 36f,
        var pressed: Boolean = false,
        var justPressed: Boolean = false,
        var pointer: Int = -1
    )

    val attackBtn = ActionButton("ATK", EtheriaGame.VIRTUAL_WIDTH - 96f, 108f, 40f)
    val heavyBtn = ActionButton("HVY", EtheriaGame.VIRTUAL_WIDTH - 178f, 70f, 30f)
    val jumpBtn = ActionButton("JMP", EtheriaGame.VIRTUAL_WIDTH - 86f, 198f, 30f)
    val dashBtn = ActionButton("DASH", EtheriaGame.VIRTUAL_WIDTH - 176f, 152f, 30f)
    val skillBtn = ActionButton("SKLL", EtheriaGame.VIRTUAL_WIDTH - 252f, 76f, 33f)
    val ultimateBtn = ActionButton("ULT", EtheriaGame.VIRTUAL_WIDTH - 258f, 162f, 35f)
    val interactBtn = ActionButton("ACT", EtheriaGame.VIRTUAL_WIDTH - 344f, 112f, 32f)

    val partyBtns = Array(4) { i ->
        ActionButton("P${i + 1}", EtheriaGame.VIRTUAL_WIDTH - 290f + i * 54f, 256f, 22f)
    }

    var interactVisible = false
    var interactLabel = "ACT"

    private val allButtons
        get() = listOf(attackBtn, heavyBtn, jumpBtn, dashBtn, skillBtn, ultimateBtn, interactBtn) + partyBtns

    private val font = EtheriaGame.createSmoothFont(0.88f)
    private val tmpVec = Vector2()

    private fun applyScale() {
        val s = GameSettings.controlsScale
        joystickRadius = 82f * s
        knobRadius = 34f * s
        attackBtn.r = 40f * s
        heavyBtn.r = 30f * s
        jumpBtn.r = 30f * s
        dashBtn.r = 30f * s
        skillBtn.r = 33f * s
        ultimateBtn.r = 35f * s
        interactBtn.r = 32f * s
    }

    fun update(uiViewport: FitViewport) {
        applyScale()
        allButtons.forEach { it.justPressed = false }

        for (i in 0 until 8) {
            if (!Gdx.input.isTouched(i)) {
                if (joystickPointer == i) {
                    joystickPointer = -1
                    joystickKnob.set(baseJoystickCenter)
                    joystickDir.setZero()
                }
                allButtons.filter { it.pointer == i }.forEach {
                    it.pressed = false
                    it.pointer = -1
                }
                continue
            }

            tmpVec.set(Gdx.input.getX(i).toFloat(), Gdx.input.getY(i).toFloat())
            uiViewport.unproject(tmpVec)
            val tx = tmpVec.x
            val ty = tmpVec.y

            if (joystickPointer == i || (joystickPointer == -1 && tx < EtheriaGame.VIRTUAL_WIDTH * 0.42f && ty < EtheriaGame.VIRTUAL_HEIGHT * 0.65f)) {
                if (joystickPointer == -1) joystickPointer = i
                val dx = tx - baseJoystickCenter.x
                val dy = ty - baseJoystickCenter.y
                val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                if (dist <= joystickRadius) {
                    joystickKnob.set(tx, ty)
                    joystickDir.set(dx / joystickRadius, dy / joystickRadius)
                } else {
                    joystickKnob.set(
                        baseJoystickCenter.x + dx / dist * joystickRadius,
                        baseJoystickCenter.y + dy / dist * joystickRadius
                    )
                    joystickDir.set(dx / dist, dy / dist)
                }
                continue
            }

            allButtons.forEach { btn ->
                if (btn == interactBtn && !interactVisible) return@forEach
                val d = Vector2.dst(tx, ty, btn.x, btn.y)
                if (d <= btn.r + 8f) {
                    if (!btn.pressed) {
                        btn.pressed = true
                        btn.justPressed = true
                        btn.pointer = i
                    }
                } else if (btn.pointer == i) {
                    btn.pressed = false
                    btn.pointer = -1
                }
            }
        }
    }

    fun render(batch: SpriteBatch, shapeRenderer: ShapeRenderer, party: PartyManager) {
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Left joystick
        shapeRenderer.setColor(0.05f, 0.08f, 0.14f, 0.35f)
        shapeRenderer.circle(baseJoystickCenter.x, baseJoystickCenter.y, joystickRadius, 24)
        shapeRenderer.setColor(0.6f, 0.8f, 1f, 0.55f)
        shapeRenderer.circle(joystickKnob.x, joystickKnob.y, knobRadius, 18)

        fun drawBtn(btn: ActionButton, color: Color) {
            val alpha = if (btn.pressed) 0.92f else 0.48f
            shapeRenderer.setColor(color.r, color.g, color.b, alpha)
            shapeRenderer.circle(btn.x, btn.y, btn.r, 20)
        }

        drawBtn(attackBtn, Color(0.92f, 0.3f, 0.3f, 1f))
        drawBtn(heavyBtn, Color(0.92f, 0.55f, 0.2f, 1f))
        drawBtn(jumpBtn, Color(0.3f, 0.72f, 0.95f, 1f))
        drawBtn(dashBtn, Color(0.45f, 0.9f, 0.55f, 1f))
        drawBtn(skillBtn, Color(0.68f, 0.36f, 0.92f, 1f))
        val ultReady = party.activeCharacter.stats.elementalEnergy >= party.activeCharacter.stats.maxElementalEnergy
        drawBtn(ultimateBtn, if (ultReady) Color(1f, 0.82f, 0.12f, 1f) else Color(0.45f, 0.42f, 0.25f, 1f))

        if (interactVisible) {
            drawBtn(interactBtn, Color(0.25f, 0.9f, 0.65f, 1f))
        }

        partyBtns.forEachIndexed { i, btn ->
            val active = i == party.activeIndex
            val dead = party.characters[i].stats.isDead
            shapeRenderer.setColor(
                when {
                    dead -> Color(0.2f, 0.2f, 0.2f, 0.55f)
                    active -> Color(0.25f, 0.72f, 1f, 0.9f)
                    else -> Color(0.15f, 0.18f, 0.32f, 0.65f)
                }
            )
            shapeRenderer.rect(btn.x - btn.r, btn.y - btn.r, btn.r * 2f, btn.r * 2f)
        }

        shapeRenderer.end()

        batch.begin()
        font.color = Color.WHITE
        val btnsToDraw = mutableListOf(attackBtn, heavyBtn, jumpBtn, dashBtn, skillBtn, ultimateBtn)
        if (interactVisible) btnsToDraw.add(interactBtn)
        btnsToDraw.forEach { btn ->
            val lbl = if (btn == interactBtn) interactLabel else btn.label
            font.draw(batch, lbl, btn.x - lbl.length * 4.5f, btn.y + 5f)
        }
        partyBtns.forEachIndexed { i, btn ->
            val ch = party.characters[i]
            font.color = if (ch.stats.isDead) Color.GRAY else Color.WHITE
            font.draw(batch, ch.name.take(2), btn.x - 10f, btn.y + 5f)
        }
        batch.end()
    }

    fun dispose() {
        font.dispose()
    }
}
