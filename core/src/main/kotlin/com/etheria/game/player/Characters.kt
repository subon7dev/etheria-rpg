package com.etheria.game.player

import com.badlogic.gdx.math.Vector2

class KaiSwordFighter : PlayableCharacter("Kai", CharacterClass.SWORD_FIGHTER, Element.FIRE, "sprites/player/kai.png") {
    override val skillCooldownMax = 6f
    override val ultimateCooldownMax = 15f
    override val dashCooldownMax = 1.2f

    init {
        equippedWeaponName = "Emberblade"
    }

    override fun normalAttack(): AttackResult {
        isAttacking = true
        attackTimer = 0.28f
        animTimer = 0f
        val mult = when (attackPhase) {
            0 -> 0.9f
            1 -> 1.15f
            else -> 1.55f
        }
        attackPhase = (attackPhase + 1) % 3
        currentAnim = "attack"
        return AttackResult(
            damage = effectiveAttack * mult,
            element = element,
            knockback = Vector2(aimDir.x * 140f, aimDir.y * 140f),
            animName = "attack",
            effectName = "flame_slash",
            radius = 74f
        )
    }

    override fun heavyAttack(): AttackResult {
        if (stats.stamina < 15f) return AttackResult(0f, Element.NONE)
        stats.stamina -= 15f
        isAttacking = true
        attackTimer = 0.48f
        animTimer = 0f
        attackPhase = 0
        currentAnim = "heavy"
        return AttackResult(
            damage = effectiveAttack * 2.4f,
            element = element,
            knockback = Vector2(aimDir.x * 260f, aimDir.y * 260f),
            animName = "heavy",
            effectName = "crimson_cleave",
            radius = 96f,
            isHeavy = true
        )
    }

    override fun useSkill(): AttackResult {
        if (skillCooldown > 0f) return AttackResult(0f, Element.NONE)
        skillCooldown = skillCooldownMax
        isAttacking = true
        attackTimer = 0.45f
        animTimer = 0f
        currentAnim = "skill"
        return AttackResult(
            damage = effectiveAttack * 3.1f,
            element = element,
            knockback = Vector2(aimDir.x * 220f, aimDir.y * 220f),
            animName = "skill",
            effectName = "blazing_vortex",
            radius = 120f
        )
    }

    override fun useUltimate(): AttackResult {
        if (ultimateCooldown > 0f || stats.elementalEnergy < stats.maxElementalEnergy) {
            return AttackResult(0f, Element.NONE)
        }
        ultimateCooldown = ultimateCooldownMax
        stats.elementalEnergy = 0f
        isAttacking = true
        attackTimer = 0.65f
        animTimer = 0f
        invincibleTimer = 0.65f
        currentAnim = "ultimate"
        return AttackResult(
            damage = effectiveAttack * 6.5f,
            element = element,
            knockback = Vector2(aimDir.x * 360f, aimDir.y * 360f),
            animName = "ultimate",
            effectName = "inferno_nova",
            radius = 190f,
            isHeavy = true
        )
    }
}

class LyraArcher : PlayableCharacter("Lyra", CharacterClass.ARCHER, Element.WIND, "sprites/player/lyra.png") {
    override val skillCooldownMax = 7f
    override val ultimateCooldownMax = 16f
    override val dashCooldownMax = 1.0f

    init {
        equippedWeaponName = "Zephyr Bow"
        stats.attackPower = 92f
        stats.speed = 235f
        stats.critChance = 0.25f
    }

    override fun normalAttack(): AttackResult {
        isAttacking = true
        attackTimer = 0.24f
        animTimer = 0f
        currentAnim = "attack"
        return AttackResult(
            damage = effectiveAttack * 0.95f,
            element = element,
            knockback = Vector2(aimDir.x * 70f, aimDir.y * 70f),
            animName = "attack",
            effectName = "wind_arrow",
            isProjectile = true,
            projectileSpeed = 560f,
            projectileCount = 1
        )
    }

    override fun heavyAttack(): AttackResult {
        if (stats.stamina < 15f) return AttackResult(0f, Element.NONE)
        stats.stamina -= 15f
        isAttacking = true
        attackTimer = 0.42f
        animTimer = 0f
        currentAnim = "heavy"
        return AttackResult(
            damage = effectiveAttack * 1.9f,
            element = element,
            knockback = Vector2(aimDir.x * 160f, aimDir.y * 160f),
            animName = "heavy",
            effectName = "gale_volley",
            isHeavy = true,
            isProjectile = true,
            projectileSpeed = 640f,
            projectileCount = 3
        )
    }

    override fun useSkill(): AttackResult {
        if (skillCooldown > 0f) return AttackResult(0f, Element.NONE)
        skillCooldown = skillCooldownMax
        isAttacking = true
        attackTimer = 0.42f
        animTimer = 0f
        currentAnim = "skill"
        return AttackResult(
            damage = effectiveAttack * 2.6f,
            element = element,
            knockback = Vector2(aimDir.x * 180f, aimDir.y * 180f),
            animName = "skill",
            effectName = "tempest_arrow_rain",
            radius = 150f
        )
    }

    override fun useUltimate(): AttackResult {
        if (ultimateCooldown > 0f || stats.elementalEnergy < stats.maxElementalEnergy) {
            return AttackResult(0f, Element.NONE)
        }
        ultimateCooldown = ultimateCooldownMax
        stats.elementalEnergy = 0f
        isAttacking = true
        attackTimer = 0.6f
        animTimer = 0f
        invincibleTimer = 0.5f
        currentAnim = "ultimate"
        return AttackResult(
            damage = effectiveAttack * 5.8f,
            element = element,
            knockback = Vector2(aimDir.x * 300f, aimDir.y * 300f),
            animName = "ultimate",
            effectName = "sky_cyclone",
            radius = 220f,
            isHeavy = true,
            isProjectile = true,
            projectileSpeed = 620f,
            projectileCount = 7
        )
    }
}

class ZaraMage : PlayableCharacter("Zara", CharacterClass.MAGE, Element.ICE, "sprites/player/zara.png") {
    override val skillCooldownMax = 6.5f
    override val ultimateCooldownMax = 16f
    override val dashCooldownMax = 1.4f

    init {
        equippedWeaponName = "Frostweaver Staff"
        stats.attackPower = 125f
        stats.maxHp = 850f
        stats.hp = 850f
        stats.defense = 35f
        stats.speed = 195f
    }

    override fun normalAttack(): AttackResult {
        isAttacking = true
        attackTimer = 0.32f
        animTimer = 0f
        currentAnim = "attack"
        return AttackResult(
            damage = effectiveAttack * 1.1f,
            element = element,
            knockback = Vector2(aimDir.x * 80f, aimDir.y * 80f),
            animName = "attack",
            effectName = "frost_orb",
            isProjectile = true,
            projectileSpeed = 460f,
            projectileCount = 1
        )
    }

    override fun heavyAttack(): AttackResult {
        if (stats.stamina < 15f) return AttackResult(0f, Element.NONE)
        stats.stamina -= 15f
        isAttacking = true
        attackTimer = 0.52f
        animTimer = 0f
        currentAnim = "heavy"
        return AttackResult(
            damage = effectiveAttack * 2.6f,
            element = element,
            knockback = Vector2(aimDir.x * 170f, aimDir.y * 170f),
            animName = "heavy",
            effectName = "glacier_lance",
            radius = 110f,
            isHeavy = true
        )
    }

    override fun useSkill(): AttackResult {
        if (skillCooldown > 0f) return AttackResult(0f, Element.NONE)
        skillCooldown = skillCooldownMax
        isAttacking = true
        attackTimer = 0.5f
        animTimer = 0f
        currentAnim = "skill"
        return AttackResult(
            damage = effectiveAttack * 3.2f,
            element = element,
            animName = "skill",
            effectName = "blizzard_ring",
            radius = 165f,
            healAmount = stats.maxHp * 0.12f
        )
    }

    override fun useUltimate(): AttackResult {
        if (ultimateCooldown > 0f || stats.elementalEnergy < stats.maxElementalEnergy) {
            return AttackResult(0f, Element.NONE)
        }
        ultimateCooldown = ultimateCooldownMax
        stats.elementalEnergy = 0f
        isAttacking = true
        attackTimer = 0.7f
        animTimer = 0f
        invincibleTimer = 0.7f
        currentAnim = "ultimate"
        return AttackResult(
            damage = effectiveAttack * 7.2f,
            element = element,
            animName = "ultimate",
            effectName = "absolute_zero",
            radius = 250f,
            isHeavy = true,
            healAmount = stats.maxHp * 0.25f
        )
    }
}

class RagnaHeavyFighter : PlayableCharacter("Ragna", CharacterClass.HEAVY_FIGHTER, Element.EARTH, "sprites/player/ragna.png") {
    override val skillCooldownMax = 8f
    override val ultimateCooldownMax = 18f
    override val dashCooldownMax = 1.6f

    init {
        equippedWeaponName = "Titan Maul"
        stats.attackPower = 140f
        stats.maxHp = 1450f
        stats.hp = 1450f
        stats.defense = 95f
        stats.speed = 175f
    }

    override fun normalAttack(): AttackResult {
        isAttacking = true
        attackTimer = 0.38f
        animTimer = 0f
        val mult = when (attackPhase) {
            0 -> 1.15f
            1 -> 1.4f
            else -> 1.85f
        }
        attackPhase = (attackPhase + 1) % 3
        currentAnim = "attack"
        return AttackResult(
            damage = effectiveAttack * mult,
            element = element,
            knockback = Vector2(aimDir.x * 210f, aimDir.y * 210f),
            animName = "attack",
            effectName = "stone_crush",
            radius = 84f
        )
    }

    override fun heavyAttack(): AttackResult {
        if (stats.stamina < 18f) return AttackResult(0f, Element.NONE)
        stats.stamina -= 18f
        isAttacking = true
        attackTimer = 0.6f
        animTimer = 0f
        attackPhase = 0
        currentAnim = "heavy"
        return AttackResult(
            damage = effectiveAttack * 3.1f,
            element = element,
            knockback = Vector2(aimDir.x * 280f, aimDir.y * 280f),
            animName = "heavy",
            effectName = "earth_shatter",
            radius = 125f,
            isHeavy = true
        )
    }

    override fun useSkill(): AttackResult {
        if (skillCooldown > 0f) return AttackResult(0f, Element.NONE)
        skillCooldown = skillCooldownMax
        isAttacking = true
        attackTimer = 0.5f
        animTimer = 0f
        invincibleTimer = 0.8f
        currentAnim = "skill"
        return AttackResult(
            damage = effectiveAttack * 3.6f,
            element = element,
            animName = "skill",
            effectName = "bastion_quake",
            radius = 135f
        )
    }

    override fun useUltimate(): AttackResult {
        if (ultimateCooldown > 0f || stats.elementalEnergy < stats.maxElementalEnergy) {
            return AttackResult(0f, Element.NONE)
        }
        ultimateCooldown = ultimateCooldownMax
        stats.elementalEnergy = 0f
        isAttacking = true
        attackTimer = 0.75f
        animTimer = 0f
        invincibleTimer = 0.9f
        currentAnim = "ultimate"
        return AttackResult(
            damage = effectiveAttack * 8.0f,
            element = element,
            animName = "ultimate",
            effectName = "tectonic_cataclysm",
            radius = 260f,
            isHeavy = true
        )
    }
}
