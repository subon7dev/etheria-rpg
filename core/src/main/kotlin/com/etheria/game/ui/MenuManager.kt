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
import com.etheria.game.GraphicsQuality
import com.etheria.game.audio.AudioManager
import com.etheria.game.inventory.InventoryManager
import com.etheria.game.player.PartyManager
import com.etheria.game.quest.Quest
import com.etheria.game.quest.QuestManager
import com.etheria.game.quest.QuestStatus
import com.etheria.game.save.SaveManager
import com.etheria.game.world.WorldManager

enum class MenuType { NONE, INVENTORY, WORLD_MAP, QUEST_JOURNAL, CHARACTER, SETTINGS }

class MenuManager(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer
) {
    var currentMenu = MenuType.NONE
    var feedbackMessage = ""
    private val font = EtheriaGame.createSmoothFont(1.1f)
    private val smallFont = EtheriaGame.createSmoothFont(0.88f)
    private val W = EtheriaGame.VIRTUAL_WIDTH
    private val H = EtheriaGame.VIRTUAL_HEIGHT
    private val tmpVec = Vector2()
    private val availableQuestHits = mutableListOf<Pair<String, Float>>()

    fun isOpen() = currentMenu != MenuType.NONE

    fun toggle(type: MenuType) {
        feedbackMessage = ""
        currentMenu = if (currentMenu == type) MenuType.NONE else type
    }

    fun close() {
        currentMenu = MenuType.NONE
    }

    /**
     * Pure input handling called from update() — no GL draw calls here.
     */
    fun handleInput(
        uiViewport: FitViewport,
        party: PartyManager,
        inventory: InventoryManager,
        quests: QuestManager,
        world: WorldManager,
        audio: AudioManager,
        saveManager: SaveManager
    ) {
        if (!isOpen() || !Gdx.input.justTouched()) return

        tmpVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        uiViewport.unproject(tmpVec)
        val tx = tmpVec.x
        val ty = tmpVec.y

        // Close button top-right
        if (tx in (W - 92f)..(W - 18f) && ty in (H - 58f)..(H - 16f)) {
            audio.playSound("ui_click")
            close()
            return
        }

        when (currentMenu) {
            MenuType.INVENTORY -> {
                // Item cards grid (4 cols)
                inventory.items.take(16).forEachIndexed { i, _ ->
                    val col = i % 4
                    val row = i / 4
                    val ix = 90f + col * 270f
                    val iy = H - 175f - row * 95f
                    if (tx in ix..(ix + 250f) && ty in iy..(iy + 80f)) {
                        feedbackMessage = inventory.equipOrUse(i, party.activeCharacter)
                        audio.playSound("ui_click")
                        return
                    }
                }
                // Upgrade weapon button at bottom-left
                if (tx in 90f..360f && ty in 44f..100f) {
                    feedbackMessage = inventory.upgradeActiveWeapon(party.activeCharacter)
                    audio.playSound("ui_click")
                }
            }

            MenuType.CHARACTER -> {
                party.characters.forEachIndexed { i, ch ->
                    val cx = 80f + i * 285f
                    val cy = 120f
                    if (tx in cx..(cx + 260f) && ty in cy..(H - 110f)) {
                        if (!ch.stats.isDead) {
                            party.switchTo(i, force = true)
                            feedbackMessage = "Switched active character to ${ch.name}"
                            audio.playSound("ui_click")
                        } else {
                            feedbackMessage = "${ch.name} is fallen — heal with a Potion!"
                        }
                    }
                }
            }

            MenuType.WORLD_MAP -> {
                val mapX = 110f
                val mapY = 95f
                val mapW = W - 220f
                val mapH = H - 190f
                world.waypoints.forEach { wp ->
                    val wx = mapX + (wp.x / 3200f) * mapW
                    val wy = mapY + (wp.y / 1920f) * mapH
                    if (Vector2.dst(tx, ty, wx, wy) <= 34f) {
                        if (wp.unlocked) {
                            party.activeCharacter.position.set(wp.x, wp.y)
                            feedbackMessage = "Fast traveled to ${wp.name}!"
                            audio.playSound("puzzle_solve")
                            close()
                        } else {
                            feedbackMessage = "${wp.name} is locked — explore near it first!"
                        }
                    }
                }
            }

            MenuType.QUEST_JOURNAL -> {
                availableQuestHits.forEach { (questId, yTop) ->
                    if (tx in 90f..780f && ty in (yTop - 42f)..yTop) {
                        val q = quests.quests.find { it.id == questId }
                        quests.startQuest(questId)
                        feedbackMessage = "Accepted Quest: ${q?.title ?: questId}"
                        audio.playSound("ui_click")
                    }
                }
            }

            MenuType.SETTINGS -> {
                // FPS toggle
                if (tx in 240f..620f && ty in 500f..550f) {
                    GameSettings.targetFps = if (GameSettings.targetFps == 60) 30 else 60
                    GameSettings.save()
                    audio.playSound("ui_click")
                }
                // Quality toggle
                if (tx in 240f..620f && ty in 430f..480f) {
                    val vals = GraphicsQuality.values()
                    GameSettings.graphicsQuality = vals[(GameSettings.graphicsQuality.ordinal + 1) % vals.size]
                    GameSettings.save()
                    audio.playSound("ui_click")
                }
                // Music volume
                if (tx in 240f..620f && ty in 360f..410f) {
                    GameSettings.musicVolume = ((GameSettings.musicVolume + 0.25f) % 1.25f).coerceIn(0f, 1f)
                    if (GameSettings.musicVolume < 0.05f) GameSettings.musicVolume = 0f
                    audio.setMusicVolume(GameSettings.musicVolume)
                    GameSettings.save()
                    audio.playSound("ui_click")
                }
                // SFX volume
                if (tx in 240f..620f && ty in 290f..340f) {
                    GameSettings.sfxVolume = ((GameSettings.sfxVolume + 0.25f) % 1.25f).coerceIn(0f, 1f)
                    if (GameSettings.sfxVolume < 0.05f) GameSettings.sfxVolume = 0f
                    audio.setSfxVolume(GameSettings.sfxVolume)
                    GameSettings.save()
                    audio.playSound("ui_click")
                }
                // Controls scale
                if (tx in 240f..620f && ty in 220f..270f) {
                    GameSettings.controlsScale = when {
                        GameSettings.controlsScale < 0.95f -> 1.0f
                        GameSettings.controlsScale < 1.1f -> 1.15f
                        else -> 0.85f
                    }
                    GameSettings.save()
                    audio.playSound("ui_click")
                }
                // Manual Save button
                if (tx in 240f..520f && ty in 120f..180f) {
                    saveManager.save(party, inventory, quests, world, party.activeCharacter.position, isAuto = false)
                    feedbackMessage = "Game Saved Locally!"
                    audio.playSound("chest_open")
                }
            }

            MenuType.NONE -> {}
        }
    }

    /**
     * Pure rendering called from draw() — clean ShapeRenderer then SpriteBatch passes.
     */
    fun render(party: PartyManager, inventory: InventoryManager, quests: QuestManager, world: WorldManager) {
        if (!isOpen()) return

        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // Backdrop + panel shapes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.03f, 0.04f, 0.09f, 0.92f)
        shapeRenderer.rect(0f, 0f, W, H)

        // Close button
        shapeRenderer.setColor(0.7f, 0.18f, 0.22f, 0.95f)
        shapeRenderer.rect(W - 92f, H - 58f, 74f, 42f)

        when (currentMenu) {
            MenuType.INVENTORY -> {
                inventory.items.take(16).forEachIndexed { i, _ ->
                    val col = i % 4
                    val row = i / 4
                    val ix = 90f + col * 270f
                    val iy = H - 175f - row * 95f
                    shapeRenderer.setColor(0.12f, 0.16f, 0.28f, 0.95f)
                    shapeRenderer.rect(ix, iy, 250f, 80f)
                }
                shapeRenderer.setColor(0.22f, 0.52f, 0.3f, 0.95f)
                shapeRenderer.rect(90f, 44f, 270f, 56f)
            }
            MenuType.CHARACTER -> {
                party.characters.forEachIndexed { i, _ ->
                    val cx = 80f + i * 285f
                    val isAct = i == party.activeIndex
                    shapeRenderer.setColor(
                        if (isAct) 0.16f else 0.1f,
                        if (isAct) 0.26f else 0.13f,
                        if (isAct) 0.44f else 0.22f,
                        0.95f
                    )
                    shapeRenderer.rect(cx, 120f, 260f, H - 230f)
                }
            }
            MenuType.WORLD_MAP -> {
                val mapX = 110f
                val mapY = 95f
                val mapW = W - 220f
                val mapH = H - 190f
                shapeRenderer.setColor(0.08f, 0.18f, 0.13f, 0.95f)
                shapeRenderer.rect(mapX, mapY, mapW, mapH)
                // Crystal river band on world map
                val rx = mapX + (1520f / 3200f) * mapW
                shapeRenderer.setColor(0.18f, 0.45f, 0.85f, 0.85f)
                shapeRenderer.rect(rx, mapY, 22f, mapH)
                // Waypoints
                world.waypoints.forEach { wp ->
                    val wx = mapX + (wp.x / 3200f) * mapW
                    val wy = mapY + (wp.y / 1920f) * mapH
                    shapeRenderer.setColor(if (wp.unlocked) Color(0.25f, 0.95f, 0.65f, 1f) else Color.GRAY)
                    shapeRenderer.circle(wx, wy, 13f, 16)
                }
                // Player pin
                val p = party.activeCharacter.position
                val px = mapX + (p.x / 3200f) * mapW
                val py = mapY + (p.y / 1920f) * mapH
                shapeRenderer.setColor(Color.YELLOW)
                shapeRenderer.circle(px, py, 8f, 12)
            }
            MenuType.SETTINGS -> {
                val ys = listOf(500f, 430f, 360f, 290f, 220f)
                ys.forEach { y ->
                    shapeRenderer.setColor(0.14f, 0.2f, 0.34f, 0.95f)
                    shapeRenderer.rect(240f, y, 380f, 50f)
                }
                shapeRenderer.setColor(0.2f, 0.56f, 0.36f, 0.95f)
                shapeRenderer.rect(240f, 120f, 280f, 60f)
            }
            else -> {}
        }
        shapeRenderer.end()

        // Text pass
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "CLOSE", W - 82f, H - 30f)

        when (currentMenu) {
            MenuType.INVENTORY -> renderInventoryText(party, inventory)
            MenuType.CHARACTER -> renderCharacterText(party)
            MenuType.WORLD_MAP -> renderWorldMapText(world)
            MenuType.QUEST_JOURNAL -> renderQuestText(quests)
            MenuType.SETTINGS -> renderSettingsText()
            MenuType.NONE -> {}
        }

        if (feedbackMessage.isNotEmpty()) {
            font.color = Color(0.45f, 1f, 0.65f, 1f)
            font.draw(batch, feedbackMessage, 90f, 30f)
        }
        batch.end()
    }

    private fun renderInventoryText(party: PartyManager, inventory: InventoryManager) {
        val active = party.activeCharacter
        font.color = Color(0.95f, 0.85f, 0.35f, 1f)
        font.draw(batch, "INVENTORY & EQUIPMENT  |  Gold: ${inventory.gold}  |  Tap item to Equip / Use", 90f, H - 36f)
        smallFont.color = Color(0.75f, 0.9f, 1f, 1f)
        smallFont.draw(batch, "Equipped (${active.name}): ${active.equippedWeaponName} +${active.weaponLevel} | ${active.equippedArmorName}", 90f, H - 68f)

        inventory.items.take(16).forEachIndexed { i, item ->
            val col = i % 4
            val row = i / 4
            val ix = 102f + col * 270f
            val iy = H - 106f - row * 95f
            font.color = Color.WHITE
            font.draw(batch, item.name, ix, iy)
            smallFont.color = Color(0.7f, 0.95f, 0.75f, 1f)
            val qty = if (item.quantity > 1) " x${item.quantity}" else ""
            smallFont.draw(batch, "[${item.type.name}] Pow:${item.power}$qty", ix, iy - 22f)
            if (item.element != com.etheria.game.player.Element.NONE) {
                smallFont.color = Color(0.6f, 0.85f, 1f, 1f)
                smallFont.draw(batch, "Elem: ${item.element.name}", ix, iy - 42f)
            }
        }

        val cost = active.weaponLevel * 100
        font.color = Color.WHITE
        font.draw(batch, "UPGRADE WEAPON ($cost G)", 106f, 80f)
    }

    private fun renderCharacterText(party: PartyManager) {
        font.color = Color(0.95f, 0.85f, 0.35f, 1f)
        font.draw(batch, "PARTY CHARACTERS (Tap card to switch)", 80f, H - 36f)

        party.characters.forEachIndexed { i, ch ->
            val cx = 98f + i * 285f
            val cy = H - 135f
            font.color = if (i == party.activeIndex) Color(0.45f, 0.95f, 1f, 1f) else Color.WHITE
            font.draw(batch, "${ch.name}${if (i == party.activeIndex) " ★" else ""}", cx, cy)
            smallFont.color = Color(0.8f, 0.9f, 0.7f, 1f)
            smallFont.draw(batch, "${ch.characterClass.name.replace("_", " ")}", cx, cy - 26f)
            smallFont.draw(batch, "Element: ${ch.element.name}", cx, cy - 48f)
            smallFont.color = Color.WHITE
            smallFont.draw(batch, "Level: ${ch.stats.level} (${ch.stats.exp.toInt()}/${ch.stats.expToNext.toInt()} EXP)", cx, cy - 76f)
            smallFont.draw(batch, "HP: ${ch.stats.hp.toInt()} / ${ch.stats.maxHp.toInt()}", cx, cy - 102f)
            smallFont.draw(batch, "ATK: ${ch.effectiveAttack.toInt()}", cx, cy - 128f)
            smallFont.draw(batch, "DEF: ${ch.effectiveDefense.toInt()}", cx, cy - 154f)
            smallFont.draw(batch, "SPD: ${ch.stats.speed.toInt()}", cx, cy - 180f)
            smallFont.draw(batch, "CRIT: ${(ch.stats.critChance * 100).toInt()}%", cx, cy - 206f)
            smallFont.color = Color(0.75f, 0.88f, 1f, 1f)
            smallFont.draw(batch, "Wpn: ${ch.equippedWeaponName} +${ch.weaponLevel}", cx, cy - 242f)
            smallFont.draw(batch, "Arm: ${ch.equippedArmorName}", cx, cy - 266f)
        }
    }

    private fun renderWorldMapText(world: WorldManager) {
        font.color = Color(0.95f, 0.85f, 0.35f, 1f)
        font.draw(batch, "WORLD MAP – ETHERIA (Tap any green Waypoint to Fast Travel)", 110f, H - 36f)
        val mapX = 110f
        val mapY = 95f
        val mapW = W - 220f
        val mapH = H - 190f
        world.waypoints.forEach { wp ->
            val wx = mapX + (wp.x / 3200f) * mapW
            val wy = mapY + (wp.y / 1920f) * mapH
            smallFont.color = if (wp.unlocked) Color.WHITE else Color.LIGHT_GRAY
            smallFont.draw(batch, wp.name + if (wp.unlocked) "" else " [Locked]", wx - 42f, wy + 28f)
        }
    }

    private fun renderQuestText(quests: QuestManager) {
        font.color = Color(0.95f, 0.85f, 0.35f, 1f)
        font.draw(batch, "QUEST JOURNAL", 90f, H - 36f)
        var y = H - 85f

        fun drawGroup(label: String, list: List<Quest>, color: Color, hint: String = "") {
            if (list.isEmpty()) return
            font.color = color
            font.draw(batch, "── $label ── $hint", 90f, y)
            y -= 28f
            list.forEach { q ->
                font.color = Color.WHITE
                font.draw(batch, "• ${q.title} (${if (q.isMainQuest) "Main" else "Side"})", 110f, y)
                y -= 22f
                q.objectives.forEach { o ->
                    smallFont.color = if (o.isComplete) Color(0.45f, 1f, 0.55f, 1f) else Color(0.8f, 0.9f, 0.8f, 1f)
                    smallFont.draw(batch, "   - ${o.description}: ${o.current}/${o.required}", 120f, y)
                    y -= 18f
                }
                y -= 6f
            }
            y -= 8f
        }

        availableQuestHits.clear()
        drawGroup("ACTIVE", quests.getActiveQuests(), Color(1f, 0.9f, 0.35f, 1f))

        val avail = quests.getAvailableQuests()
        if (avail.isNotEmpty()) {
            font.color = Color(0.45f, 0.95f, 0.6f, 1f)
            font.draw(batch, "── AVAILABLE ── (Tap quest title to accept)", 90f, y)
            y -= 28f
            avail.forEach { q ->
                availableQuestHits.add(q.id to y)
                font.color = Color.WHITE
                font.draw(batch, "• [ACCEPT] ${q.title} (${if (q.isMainQuest) "Main" else "Side"})", 110f, y)
                y -= 22f
                q.objectives.forEach { o ->
                    smallFont.color = Color(0.8f, 0.9f, 0.8f, 1f)
                    smallFont.draw(batch, "   - ${o.description}: ${o.current}/${o.required}", 120f, y)
                    y -= 18f
                }
                y -= 6f
            }
            y -= 8f
        }

        drawGroup("COMPLETED", quests.getCompletedQuests(), Color(0.6f, 0.65f, 0.72f, 1f))
    }

    private fun renderSettingsText() {
        font.color = Color(0.95f, 0.85f, 0.35f, 1f)
        font.draw(batch, "SETTINGS (Tap any option to toggle)", 240f, H - 55f)
        font.color = Color.WHITE
        font.draw(batch, "FPS Mode: ${GameSettings.targetFps} FPS", 260f, 533f)
        font.draw(batch, "Graphics Quality: ${GameSettings.graphicsQuality.name}", 260f, 463f)
        font.draw(batch, "Music Volume: ${(GameSettings.musicVolume * 100).toInt()}%", 260f, 393f)
        font.draw(batch, "SFX Volume: ${(GameSettings.sfxVolume * 100).toInt()}%", 260f, 323f)
        font.draw(batch, "Touch Controls Scale: ${String.format("%.2fx", GameSettings.controlsScale)}", 260f, 253f)
        font.draw(batch, "SAVE GAME NOW", 295f, 158f)
    }

    fun dispose() {
        font.dispose()
        smallFont.dispose()
    }
}
