package com.etheria.game.inventory

import com.etheria.game.player.Element
import com.etheria.game.player.PlayableCharacter
import com.etheria.game.world.Item
import com.etheria.game.world.ItemType

class InventoryManager {

    val items = mutableListOf<Item>()
    var gold: Int = 150
        private set

    fun addItem(item: Item): Boolean {
        if (item.type == ItemType.CONSUMABLE || item.type == ItemType.MATERIAL) {
            val idx = items.indexOfFirst { it.name == item.name }
            if (idx >= 0) {
                items[idx] = items[idx].copy(quantity = items[idx].quantity + item.quantity)
                return true
            }
        }
        items.add(item)
        return true
    }

    fun removeItem(name: String, count: Int = 1): Boolean {
        val idx = items.indexOfFirst { it.name == name }
        if (idx < 0) return false
        val existing = items[idx]
        return if (existing.quantity > count) {
            items[idx] = existing.copy(quantity = existing.quantity - count)
            true
        } else if (existing.quantity == count) {
            items.removeAt(idx)
            true
        } else {
            false
        }
    }

    fun equipOrUse(index: Int, character: PlayableCharacter): String {
        if (index !in items.indices) return ""
        val item = items[index]
        return when (item.type) {
            ItemType.WEAPON -> {
                character.equippedWeaponName = item.name
                character.weaponBonusAttack = item.power.toFloat()
                character.elementOverride = if (item.element != Element.NONE) item.element else null
                "Equipped ${item.name} (+${item.power} ATK)"
            }
            ItemType.ARMOR -> {
                character.equippedArmorName = item.name
                character.armorBonusDefense = item.power.toFloat()
                "Equipped ${item.name} (+${item.power} DEF)"
            }
            ItemType.CONSUMABLE -> {
                if (character.stats.hp >= character.stats.maxHp) {
                    return "${character.name} is already at full HP!"
                }
                val heal = item.power.toFloat()
                character.stats.hp = minOf(character.stats.maxHp, character.stats.hp + heal)
                removeItem(item.name, 1)
                "Used ${item.name}: +${heal.toInt()} HP!"
            }
            else -> "${item.name}: ${item.description.ifEmpty { "Crafting/Quest item" }}"
        }
    }

    fun upgradeActiveWeapon(character: PlayableCharacter): String {
        val cost = character.weaponLevel * 100
        if (gold < cost) return "Need $cost Gold to upgrade!"
        gold -= cost
        character.weaponLevel++
        return "${character.equippedWeaponName} upgraded to +${character.weaponLevel}!"
    }

    fun addGold(amount: Int) {
        gold += amount
    }

    fun setGold(amount: Int) {
        gold = maxOf(0, amount)
    }

    fun spendGold(amount: Int): Boolean {
        if (gold < amount) return false
        gold -= amount
        return true
    }

    fun getStarterItems() {
        items.clear()
        gold = 150
        addItem(Item("Health Potion", ItemType.CONSUMABLE, power = 350, quantity = 3, description = "Restores 350 HP"))
        addItem(Item("Iron Sword", ItemType.WEAPON, power = 35, description = "Reliable forged blade"))
        addItem(Item("Leather Armor", ItemType.ARMOR, power = 25, description = "Light adventurer armor"))
        addItem(Item("Spark Stone", ItemType.MATERIAL, quantity = 2, description = "Upgrade ore", element = Element.LIGHTNING))
    }
}
