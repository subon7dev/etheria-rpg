package com.etheria.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.etheria.game.EtheriaGame
import com.etheria.game.combat.DamageNumber
import com.etheria.game.enemy.Enemy
import com.etheria.game.player.Element
import com.etheria.game.player.PartyManager
import com.etheria.game.quest.QuestManager

class HUDRenderer(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer
) {
    private val font = EtheriaGame.createSmoothFont(1.05f)
    private val smallFont = EtheriaGame.createSmoothFont(0.85f)

    private val W = EtheriaGame.VIRTUAL_WIDTH
    private val H = EtheriaGame.VIRTUAL_HEIGHT

    private var shakeIntensity = 0f
    private var shakeTimer = 0f
    val shakeOffset = Vector2()

    fun triggerShake(intensity: Float, duration: Float = 0.25f) {
        shakeIntensity = maxOf(shakeIntensity, intensity)
        shakeTimer = maxOf(shakeTimer, duration)
    }

    fun updateShake(delta: Float) {
        if (shakeTimer > 0f) {
            shakeTimer -= delta
            shakeOffset.x = MathUtils.random(-shakeIntensity, shakeIntensity)
            shakeOffset.y = MathUtils.random(-shakeIntensity, shakeIntensity)
        } else {
            shakeIntensity = 0f
            shakeOffset.setZero()
        }
    }

    fun renderHUD(party: PartyManager, quests: QuestManager, statusHint: String = "") {
        val active = party.activeCharacter
        val hpPct = (active.stats.hp / active.stats.maxHp).coerceIn(0f, 1f)
        val stPct = (active.stats.stamina / active.stats.maxStamina).coerceIn(0f, 1f)
        val epPct = (active.stats.elementalEnergy / active.stats.maxElementalEnergy).coerceIn(0f, 1f)

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // ── ShapeRenderer pass (all HUD panels & bars together) ──────────────
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        // Top-left character panel backdrop
        shapeRenderer.setColor(0.05f, 0.07f, 0.14f, 0.72f)
        shapeRenderer.rect(14f, H - 126f, 252f, 112f)

        // HP bar
        drawBarShape(24f, H - 42f, 210f, 18f, hpPct, Color(0.22f, 0.86f, 0.36f, 0.95f))
        // Stamina bar
        drawBarShape(24f, H - 64f, 175f, 13f, stPct, Color(0.95f, 0.85f, 0.22f, 0.95f))
        // Elemental Energy bar
        drawBarShape(24f, H - 83f, 175f, 13f, epPct, getElementColor(active.element))

        // Top-right party cards
        party.characters.forEachIndexed { i, char ->
            val px = W - 188f + i * 44f
            val py = H - 82f
            val isAct = i == party.activeIndex
            shapeRenderer.setColor(
                if (isAct) 0.16f else 0.06f,
                if (isAct) 0.25f else 0.08f,
                if (isAct) 0.45f else 0.15f,
                0.85f
            )
            shapeRenderer.rect(px, py, 38f, 66f)
            val cHp = (char.stats.hp / char.stats.maxHp).coerceIn(0f, 1f)
            shapeRenderer.setColor(if (char.stats.isDead) Color.DARK_GRAY else getElementColor(char.element))
            shapeRenderer.rect(px + 4f, py + 6f, 30f * cHp, 6f)
        }

        // Quest tracker backdrop (below party cards on top-right)
        val activeQuests = quests.getActiveQuests().take(2)
        if (activeQuests.isNotEmpty()) {
            shapeRenderer.setColor(0.05f, 0.07f, 0.14f, 0.65f)
            shapeRenderer.rect(W - 300f, H - 215f, 286f, 122f)
        }

        shapeRenderer.end()

        // ── SpriteBatch pass (all HUD text together) ─────────────────────────
        batch.begin()

        smallFont.color = Color.WHITE
        smallFont.draw(batch, "HP ${active.stats.hp.toInt()}/${active.stats.maxHp.toInt()}", 28f, H - 28f)
        smallFont.draw(batch, "ST ${active.stats.stamina.toInt()}", 28f, H - 52f)
        smallFont.draw(batch, "ULT ${active.stats.elementalEnergy.toInt()}%", 28f, H - 71f)

        font.color = Color.WHITE
        font.draw(batch, "${active.name}  Lv.${active.stats.level}  [${active.element.name}]", 24f, H - 94f)
        smallFont.color = Color(0.75f, 0.88f, 1f, 0.95f)
        smallFont.draw(batch, "${active.equippedWeaponName} +${active.weaponLevel}", 24f, H - 110f)

        if (statusHint.isNotEmpty()) {
            smallFont.color = Color(0.55f, 0.95f, 1f, 0.95f)
            smallFont.draw(batch, statusHint, 24f, H - 134f)
        }

        party.characters.forEachIndexed { i, char ->
            val px = W - 188f + i * 44f
            val py = H - 82f
            smallFont.color = if (char.stats.isDead) Color.GRAY else Color.WHITE
            smallFont.draw(batch, char.name.take(3), px + 5f, py + 54f)
            smallFont.color = getElementColor(char.element)
            smallFont.draw(batch, "${char.element.name.take(3)}", px + 5f, py + 34f)
        }

        if (activeQuests.isNotEmpty()) {
            font.color = Color(0.95f, 0.85f, 0.4f, 1f)
            font.draw(batch, "ACTIVE QUESTS", W - 290f, H - 100f)
            var qy = H - 122f
            activeQuests.forEach { quest ->
                smallFont.color = Color.WHITE
                smallFont.draw(batch, "• ${quest.title}", W - 290f, qy)
                qy -= 18f
                quest.objectives.take(2).forEach { obj ->
                    smallFont.color = if (obj.isComplete) Color(0.4f, 1f, 0.5f, 1f) else Color(0.75f, 0.9f, 0.8f, 1f)
                    smallFont.draw(batch, "  ${obj.description}: ${obj.current}/${obj.required}", W - 290f, qy)
                    qy -= 16f
                }
            }
        }

        batch.end()
    }

    /**
     * Must be called while batch projection is set to world camera!
     */
    fun renderWorldDamageNumbers(numbers: List<DamageNumber>) {
        if (numbers.isEmpty()) return
        batch.begin()
        numbers.forEach { dmg ->
            val alpha = (dmg.lifetime / 1.15f).coerceIn(0f, 1f)
            val display = dmg.label.ifEmpty { "${dmg.value.toInt()}" }
            if (dmg.isCrit) {
                font.color = Color(1f, 0.42f, 0.14f, alpha)
                font.draw(batch, display, dmg.x - 18f, dmg.y)
            } else {
                val c = getElementColor(dmg.element)
                font.color = Color(c.r, c.g, c.b, alpha)
                font.draw(batch, display, dmg.x - 10f, dmg.y)
            }
        }
        batch.end()
    }

    fun renderBossBar(boss: Enemy) {
        val hpPct = boss.getHpPercent()
        val barW = W * 0.44f
        val barX = W * 0.28f
        val barY = H - 72f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.08f, 0.05f, 0.1f, 0.9f)
        shapeRenderer.rect(barX - 3f, barY - 3f, barW + 6f, 22f)
        shapeRenderer.setColor(0.32f, 0.06f, 0.08f, 1f)
        shapeRenderer.rect(barX, barY, barW, 16f)
        shapeRenderer.setColor(0.92f, 0.18f, 0.25f, 1f)
        shapeRenderer.rect(barX, barY, barW * hpPct, 16f)
        shapeRenderer.setColor(Color.WHITE)
        shapeRenderer.rect(barX + barW * 0.33f - 1f, barY, 2f, 16f)
        shapeRenderer.rect(barX + barW * 0.66f - 1f, barY, 2f, 16f)
        shapeRenderer.end()

        batch.begin()
        smallFont.color = Color.WHITE
        smallFont.draw(batch, "${boss.displayName}  (${(hpPct * 100).toInt()}%)  Phase ${boss.bossPhase + 1}/3", barX + 10f, barY + 32f)
        batch.end()
    }

    private fun drawBarShape(x: Float, y: Float, w: Float, h: Float, pct: Float, color: Color) {
        shapeRenderer.setColor(0.12f, 0.14f, 0.2f, 0.9f)
        shapeRenderer.rect(x, y, w, h)
        shapeRenderer.setColor(color)
        shapeRenderer.rect(x, y, w * pct.coerceIn(0f, 1f), h)
    }

    fun getElementColor(element: Element): Color = when (element) {
        Element.FIRE -> Color(1f, 0.42f, 0.15f, 1f)
        Element.WATER -> Color(0.22f, 0.66f, 1f, 1f)
        Element.ICE -> Color(0.55f, 0.92f, 1f, 1f)
        Element.LIGHTNING -> Color(0.95f, 0.85f, 0.2f, 1f)
        Element.WIND -> Color(0.45f, 0.95f, 0.55f, 1f)
        Element.EARTH -> Color(0.78f, 0.56f, 0.26f, 1f)
        Element.NONE -> Color.WHITE
    }

    fun dispose() {
        font.dispose()
        smallFont.dispose()
    }
}
