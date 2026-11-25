package com.etheria.game.player

class PartyManager {

    val characters: List<PlayableCharacter> = listOf(
        KaiSwordFighter(),
        LyraArcher(),
        ZaraMage(),
        RagnaHeavyFighter()
    )

    var activeIndex = 0
        private set

    val activeCharacter get() = characters[activeIndex]

    private var switchCooldown = 0f
    private val switchCooldownMax = 0.8f

    init {
        characters[0].isActive = true
    }

    fun switchTo(index: Int, force: Boolean = false): Boolean {
        if (index == activeIndex || index !in characters.indices) return false
        if (!force && (switchCooldown > 0f || characters[index].stats.isDead)) return false
        val prevPos = activeCharacter.position.cpy()
        val prevAim = activeCharacter.aimDir.cpy()
        val prevFacing = activeCharacter.facingRight
        characters[activeIndex].isActive = false
        activeIndex = index
        characters[activeIndex].apply {
            isActive = true
            position.set(prevPos)
            aimDir.set(prevAim)
            facingRight = prevFacing
            invincibleTimer = maxOf(invincibleTimer, 0.25f)
        }
        if (!force) switchCooldown = switchCooldownMax
        return true
    }

    fun switchToFirstAlive(): Boolean {
        val idx = characters.indexOfFirst { !it.stats.isDead }
        return if (idx >= 0) switchTo(idx, force = true) else false
    }

    fun healAll(fraction: Float = 1f) {
        characters.forEach { ch ->
            ch.stats.hp = minOf(ch.stats.maxHp, ch.stats.hp + ch.stats.maxHp * fraction)
            ch.stats.stamina = ch.stats.maxStamina
        }
    }

    fun update(delta: Float) {
        if (switchCooldown > 0f) switchCooldown -= delta
        characters.forEachIndexed { idx, ch ->
            if (idx == activeIndex) {
                ch.update(delta)
            } else {
                // Background cooldown & energy tick for inactive party members
                if (ch.skillCooldown > 0f) ch.skillCooldown = maxOf(0f, ch.skillCooldown - delta)
                if (ch.ultimateCooldown > 0f) ch.ultimateCooldown = maxOf(0f, ch.ultimateCooldown - delta)
                if (ch.dashCooldown > 0f) ch.dashCooldown = maxOf(0f, ch.dashCooldown - delta)
                if (!ch.stats.isDead) {
                    ch.stats.stamina = minOf(ch.stats.maxStamina, ch.stats.stamina + 15f * delta)
                }
            }
        }
    }

    fun getSwitchCooldownPercent() = if (switchCooldown <= 0f) 1f else 1f - (switchCooldown / switchCooldownMax)

    fun allDead() = characters.all { it.stats.isDead }
}
