package com.etheria.game.quest

import com.etheria.game.inventory.InventoryManager
import com.etheria.game.player.Element
import com.etheria.game.player.PartyManager
import com.etheria.game.world.Item
import com.etheria.game.world.ItemType

enum class QuestStatus { AVAILABLE, ACTIVE, COMPLETED, FAILED }
enum class ObjectiveType { KILL, COLLECT, REACH, TALK, PUZZLE }

data class QuestObjective(
    val type: ObjectiveType,
    val description: String,
    val targetId: String,
    val required: Int,
    var current: Int = 0
) {
    val isComplete get() = current >= required
    fun progress(amount: Int = 1) {
        current = minOf(required, current + amount)
    }
}

data class QuestReward(
    val type: String,
    val value: Int,
    val itemName: String = "",
    val itemType: ItemType = ItemType.WEAPON,
    val itemPower: Int = 60,
    val itemElement: Element = Element.NONE
)

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val objectives: List<QuestObjective>,
    val rewards: List<QuestReward>,
    val isMainQuest: Boolean = false,
    var status: QuestStatus = QuestStatus.AVAILABLE,
    var rewardsClaimed: Boolean = false
) {
    val isComplete get() = objectives.all { it.isComplete }
}

class QuestManager {

    val quests = mutableListOf<Quest>()

    init {
        initializeQuests()
    }

    private fun initializeQuests() {
        quests.clear()
        quests.add(
            Quest(
                id = "main_01",
                title = "The Shadow Rises",
                description = "Clear the Emerald Forest and unlock the Temple path.",
                objectives = listOf(
                    QuestObjective(ObjectiveType.KILL, "Defeat 5 Slimes", "slime", 5),
                    QuestObjective(ObjectiveType.KILL, "Defeat Goblin Leader", "goblin_leader", 1),
                    QuestObjective(ObjectiveType.REACH, "Cross Crystal River", "crystal_river", 1)
                ),
                rewards = listOf(
                    QuestReward("gold", 500),
                    QuestReward("exp", 800),
                    QuestReward("item", 1, "Thunder Staff", ItemType.WEAPON, 95, Element.LIGHTNING)
                ),
                isMainQuest = true
            )
        )

        quests.add(
            Quest(
                id = "side_01",
                title = "Blacksmith's Request",
                description = "Collect Forest Herbs and defeat Crypt Skeletons.",
                objectives = listOf(
                    QuestObjective(ObjectiveType.COLLECT, "Collect 3 Forest Herbs", "forest_herb", 3),
                    QuestObjective(ObjectiveType.KILL, "Defeat 2 Skeletons", "skeleton", 2)
                ),
                rewards = listOf(
                    QuestReward("gold", 300),
                    QuestReward("exp", 450),
                    QuestReward("item", 1, "Reinforced Armor", ItemType.ARMOR, 85)
                )
            )
        )

        quests.add(
            Quest(
                id = "temple_01",
                title = "Echoes of the Ancients",
                description = "Awaken all 3 Elemental Rune Pillars at the Ancient Temple.",
                objectives = listOf(
                    QuestObjective(ObjectiveType.PUZZLE, "Activate 3 Rune Pillars", "temple_puzzle", 3)
                ),
                rewards = listOf(
                    QuestReward("gold", 400),
                    QuestReward("exp", 600),
                    QuestReward("item", 1, "Aqua Blade", ItemType.WEAPON, 105, Element.WATER)
                ),
                isMainQuest = true
            )
        )

        quests.add(
            Quest(
                id = "boss_01",
                title = "Shadow Lord's Lair",
                description = "Defeat Shadow Lord Malakor in the eastern arena to save Etheria.",
                objectives = listOf(
                    QuestObjective(ObjectiveType.KILL, "Defeat Shadow Lord", "shadow_lord", 1)
                ),
                rewards = listOf(
                    QuestReward("gold", 5000),
                    QuestReward("exp", 3000),
                    QuestReward("item", 1, "Etheria's Blessing", ItemType.WEAPON, 180, Element.LIGHTNING)
                ),
                isMainQuest = true
            )
        )
    }

    fun startQuest(id: String): Boolean {
        val quest = quests.find { it.id == id } ?: return false
        if (quest.status != QuestStatus.AVAILABLE) return false
        quest.status = QuestStatus.ACTIVE
        return true
    }

    fun progressAllActive(
        targetId: String,
        type: ObjectiveType,
        amount: Int = 1,
        inventory: InventoryManager? = null,
        party: PartyManager? = null
    ): List<Quest> {
        val newlyCompleted = mutableListOf<Quest>()
        quests.filter { it.status == QuestStatus.ACTIVE }.forEach { quest ->
            quest.objectives.filter { it.targetId == targetId && it.type == type }.forEach { obj ->
                obj.progress(amount)
            }
            if (quest.isComplete) {
                quest.status = QuestStatus.COMPLETED
                if (!quest.rewardsClaimed) {
                    grantRewards(quest, inventory, party)
                }
                newlyCompleted.add(quest)
            }
        }
        // Auto-unlock follow-up boss quest when main_01 or temple_01 finishes
        if (newlyCompleted.any { it.id == "main_01" || it.id == "temple_01" }) {
            startQuest("temple_01")
            startQuest("boss_01")
        }
        return newlyCompleted
    }

    private fun grantRewards(quest: Quest, inventory: InventoryManager?, party: PartyManager?) {
        quest.rewardsClaimed = true
        quest.rewards.forEach { reward ->
            when (reward.type) {
                "gold" -> inventory?.addGold(reward.value)
                "exp" -> party?.activeCharacter?.stats?.addExp(reward.value.toFloat())
                "item" -> inventory?.addItem(
                    Item(
                        name = reward.itemName,
                        type = reward.itemType,
                        power = reward.itemPower,
                        quantity = reward.value,
                        description = "Quest reward from ${quest.title}",
                        element = reward.itemElement
                    )
                )
            }
        }
    }

    fun getActiveQuests() = quests.filter { it.status == QuestStatus.ACTIVE }
    fun getAvailableQuests() = quests.filter { it.status == QuestStatus.AVAILABLE }
    fun getCompletedQuests() = quests.filter { it.status == QuestStatus.COMPLETED }
}
