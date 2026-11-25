package com.etheria.game.save

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.math.Vector2
import com.etheria.game.inventory.InventoryManager
import com.etheria.game.player.Element
import com.etheria.game.player.PartyManager
import com.etheria.game.quest.QuestManager
import com.etheria.game.quest.QuestStatus
import com.etheria.game.world.Item
import com.etheria.game.world.ItemType
import com.etheria.game.world.WorldManager

class SaveManager {

    private val saveFile: FileHandle get() = Gdx.files.local("etheria_save.dat")
    private val autoSaveFile: FileHandle get() = Gdx.files.local("etheria_autosave.dat")

    fun save(
        party: PartyManager,
        inventory: InventoryManager,
        quests: QuestManager,
        world: WorldManager,
        playerPos: Vector2,
        isAuto: Boolean = false
    ) {
        val sb = StringBuilder()
        sb.append("x=${playerPos.x}\n")
        sb.append("y=${playerPos.y}\n")
        sb.append("activeChar=${party.activeIndex}\n")
        sb.append("gold=${inventory.gold}\n")
        sb.append("timeOfDay=${world.timeOfDay}\n")
        sb.append("puzzleSolved=${world.puzzleSolved}\n")

        party.characters.forEachIndexed { idx, ch ->
            sb.append("char_${idx}=${ch.stats.hp},${ch.stats.maxHp},${ch.stats.level},${ch.stats.exp},${ch.stats.expToNext},${ch.weaponLevel},${ch.weaponBonusAttack},${ch.armorBonusDefense},${ch.equippedWeaponName},${ch.equippedArmorName},${ch.elementOverride?.name ?: "NONE"}\n")
        }

        val invStr = inventory.items.joinToString(";") {
            "${it.name.replace(";", "_")}|${it.type.name}|${it.power}|${it.quantity}|${it.element.name}"
        }
        sb.append("inventory=$invStr\n")

        val qStr = quests.quests.joinToString(";") { q ->
            val objs = q.objectives.joinToString(",") { "${it.current}" }
            "${q.id}|${q.status.name}|${q.rewardsClaimed}|$objs"
        }
        sb.append("quests=$qStr\n")

        sb.append("waypoints=${world.waypoints.filter { it.unlocked }.joinToString(",") { it.name }}\n")
        sb.append("chests=${world.chests.filter { it.opened }.joinToString(",") { it.id }}\n")
        sb.append("collectibles=${world.collectibles.filter { it.collected }.joinToString(",") { it.id }}\n")
        sb.append("pillars=${world.puzzlePillars.filter { it.activated }.joinToString(",") { it.id.toString() }}\n")

        val target = if (isAuto) autoSaveFile else saveFile
        try {
            target.writeString(sb.toString(), false)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to save: ${e.message}")
        }
    }

    fun load(
        party: PartyManager,
        inventory: InventoryManager,
        quests: QuestManager,
        world: WorldManager
    ): Vector2 {
        val file = when {
            saveFile.exists() -> saveFile
            autoSaveFile.exists() -> autoSaveFile
            else -> return Vector2(384f, 496f)
        }
        return try {
            val map = file.readString().lines()
                .mapNotNull { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()

            val x = map["x"]?.toFloatOrNull() ?: 384f
            val y = map["y"]?.toFloatOrNull() ?: 496f

            map["gold"]?.toIntOrNull()?.let { inventory.setGold(it) }
            map["timeOfDay"]?.toFloatOrNull()?.let { world.timeOfDay = it }
            map["puzzleSolved"]?.toBooleanStrictOrNull()?.let { world.puzzleSolved = it }

            party.characters.forEachIndexed { idx, ch ->
                val raw = map["char_$idx"] ?: return@forEachIndexed
                val p = raw.split(",")
                if (p.size >= 11) {
                    ch.stats.hp = p[0].toFloatOrNull() ?: ch.stats.hp
                    ch.stats.maxHp = p[1].toFloatOrNull() ?: ch.stats.maxHp
                    ch.stats.level = p[2].toIntOrNull() ?: ch.stats.level
                    ch.stats.exp = p[3].toFloatOrNull() ?: ch.stats.exp
                    ch.stats.expToNext = p[4].toFloatOrNull() ?: ch.stats.expToNext
                    ch.weaponLevel = p[5].toIntOrNull() ?: ch.weaponLevel
                    ch.weaponBonusAttack = p[6].toFloatOrNull() ?: ch.weaponBonusAttack
                    ch.armorBonusDefense = p[7].toFloatOrNull() ?: ch.armorBonusDefense
                    ch.equippedWeaponName = p[8]
                    ch.equippedArmorName = p[9]
                    ch.elementOverride = if (p[10] != "NONE") Element.valueOf(p[10]) else null
                }
            }

            map["activeChar"]?.toIntOrNull()?.let { party.switchTo(it, force = true) }

            val invRaw = map["inventory"]
            if (!invRaw.isNullOrBlank()) {
                inventory.items.clear()
                invRaw.split(";").forEach { entry ->
                    val p = entry.split("|")
                    if (p.size >= 5) {
                        inventory.addItem(
                            Item(
                                name = p[0],
                                type = ItemType.valueOf(p[1]),
                                power = p[2].toIntOrNull() ?: 0,
                                quantity = p[3].toIntOrNull() ?: 1,
                                element = Element.valueOf(p[4])
                            )
                        )
                    }
                }
            }

            map["quests"]?.split(";")?.forEach { entry ->
                val p = entry.split("|")
                if (p.size >= 4) {
                    val q = quests.quests.find { it.id == p[0] } ?: return@forEach
                    q.status = QuestStatus.valueOf(p[1])
                    q.rewardsClaimed = p[2].toBooleanStrictOrNull() ?: false
                    val objVals = p[3].split(",")
                    q.objectives.forEachIndexed { i, obj ->
                        obj.current = objVals.getOrNull(i)?.toIntOrNull() ?: obj.current
                    }
                }
            }

            val unlockedWps = map["waypoints"]?.split(",")?.toSet() ?: emptySet()
            world.waypoints.forEach { if (it.name in unlockedWps) it.unlocked = true }

            val openedChests = map["chests"]?.split(",")?.toSet() ?: emptySet()
            world.chests.forEach { if (it.id in openedChests) it.opened = true }

            val collectedIds = map["collectibles"]?.split(",")?.toSet() ?: emptySet()
            world.collectibles.forEach { if (it.id in collectedIds) it.collected = true }

            val activePillars = map["pillars"]?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
            world.puzzlePillars.forEach { if (it.id in activePillars) it.activated = true }

            Vector2(x, y)
        } catch (e: Exception) {
            Gdx.app.error("SaveManager", "Failed to load: ${e.message}")
            Vector2(384f, 496f)
        }
    }

    fun hasSave() = saveFile.exists() || autoSaveFile.exists()

    fun deleteSave() {
        if (saveFile.exists()) saveFile.delete()
        if (autoSaveFile.exists()) autoSaveFile.delete()
    }
}
