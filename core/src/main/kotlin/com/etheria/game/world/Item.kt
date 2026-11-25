package com.etheria.game.world

import com.etheria.game.player.Element

enum class ItemType { WEAPON, ARMOR, ACCESSORY, CONSUMABLE, MATERIAL, KEY_ITEM }

data class Item(
    val name: String,
    val type: ItemType,
    val power: Int = 0,
    val quantity: Int = 1,
    val description: String = "",
    val element: Element = Element.NONE
)
