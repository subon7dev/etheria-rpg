package com.etheria.game.player

import com.badlogic.gdx.math.Vector2

enum class Element { FIRE, WATER, ICE, LIGHTNING, WIND, EARTH, NONE }
enum class CharacterClass { SWORD_FIGHTER, ARCHER, MAGE, HEAVY_FIGHTER }

data class CharacterStats(
    var maxHp: Float = 1000f,
    var hp: Float = 1000f,
    var maxStamina: Float = 100f,
    var stamina: Float = 100f,
    var maxElementalEnergy: Float = 100f,
    var elementalEnergy: Float = 25f,
    var attackPower: Float = 100f,
    var defense: Float = 50f,
    var critChance: Float = 0.15f,
    var critMultiplier: Float = 1.6f,
    var speed: Float = 210f,
    var level: Int = 1,
    var exp: Float = 0f,
    var expToNext: Float = 500f
) {
    fun levelUp() {
        level++
        maxHp += 120f
        hp = maxHp
        maxStamina += 10f
        stamina = maxStamina
        attackPower += 18f
        defense += 8f
        exp -= expToNext
        expToNext *= 1.45f
    }

    fun addExp(amount: Float): Boolean {
        exp += amount
        var leveled = false
        while (exp >= expToNext) {
            levelUp()
            leveled = true
        }
        return leveled
    }

    val isDead get() = hp <= 0f

    fun takeDamage(dmg: Float, bonusDef: Float = 0f): Float {
        val totalDef = defense + bonusDef
        val actual = maxOf(2f, dmg - totalDef * 0.3f)
        hp = maxOf(0f, hp - actual)
        return actual
    }
}

data class AttackResult(
    val damage: Float,
    val element: Element,
    val knockback: Vector2 = Vector2(),
    val animName: String = "attack",
    val effectName: String = "",
    val radius: Float = 0f,
    val isHeavy: Boolean = false,
    val isProjectile: Boolean = false,
    val projectileSpeed: Float = 480f,
    val projectileCount: Int = 1,
    val healAmount: Float = 0f
)

abstract class PlayableCharacter(
    val name: String,
    val characterClass: CharacterClass,
    val baseElement: Element,
    val sheetPath: String
) {
    val stats = CharacterStats()
    val position = Vector2(320f, 384f)
    val velocity = Vector2()

    var elementOverride: Element? = null
    val element: Element get() = elementOverride ?: baseElement

    var equippedWeaponName: String = "Starter Blade"
    var weaponBonusAttack: Float = 0f
    var weaponLevel: Int = 1
    var equippedArmorName: String = "Adventurer Garb"
    var armorBonusDefense: Float = 0f

    var isActive = false
    var facingRight = true
    var aimDir = Vector2(1f, 0f)
    var currentAnim = "idle"
    var animTimer = 0f

    var isAttacking = false
    var attackTimer = 0f
    var attackPhase = 0
    var isDashing = false
    var dashTimer = 0f
    var dashCooldown = 0f
    var skillCooldown = 0f
    var ultimateCooldown = 0f
    var invincibleTimer = 0f
    var hurtTimer = 0f

    // Jump / Swim / Climb / Sprint states
    var jumpZ = 0f
    var jumpVelZ = 0f
    val isJumping get() = jumpZ > 0f || jumpVelZ > 0f
    var isSwimming = false
    var isClimbing = false
    var isSprinting = false

    abstract val skillCooldownMax: Float
    abstract val ultimateCooldownMax: Float
    abstract val dashCooldownMax: Float

    val effectiveAttack get() = stats.attackPower + weaponBonusAttack + (weaponLevel - 1) * 12f
    val effectiveDefense get() = stats.defense + armorBonusDefense

    open fun update(delta: Float) {
        animTimer += delta

        if (stats.isDead) {
            currentAnim = "death"
            return
        }

        if (isSprinting && !isDashing) {
            stats.stamina = maxOf(0f, stats.stamina - 18f * delta)
            if (stats.stamina <= 0f) isSprinting = false
        } else if (isSwimming) {
            stats.stamina = maxOf(0f, stats.stamina - 10f * delta)
        } else if (isClimbing) {
            stats.stamina = maxOf(0f, stats.stamina - 12f * delta)
        } else if (!isDashing) {
            stats.stamina = minOf(stats.maxStamina, stats.stamina + 24f * delta)
        }

        // Passive elemental energy trickle
        stats.elementalEnergy = minOf(stats.maxElementalEnergy, stats.elementalEnergy + 1.5f * delta)

        if (dashCooldown > 0f) dashCooldown -= delta
        if (skillCooldown > 0f) skillCooldown -= delta
        if (ultimateCooldown > 0f) ultimateCooldown -= delta
        if (invincibleTimer > 0f) invincibleTimer -= delta

        if (hurtTimer > 0f) {
            hurtTimer -= delta
            if (!isAttacking) currentAnim = "hurt"
        }

        // Jump physics
        if (jumpZ > 0f || jumpVelZ != 0f) {
            jumpZ += jumpVelZ * delta
            jumpVelZ -= 650f * delta
            if (jumpZ <= 0f) {
                jumpZ = 0f
                jumpVelZ = 0f
            }
        }

        if (isDashing) {
            dashTimer -= delta
            if (dashTimer <= 0f) {
                isDashing = false
                velocity.setZero()
            }
        }

        if (isAttacking) {
            attackTimer -= delta
            if (attackTimer <= 0f) {
                isAttacking = false
                currentAnim = "idle"
            }
        }
    }

    fun jump(): Boolean {
        if (isJumping || stats.stamina < 8f || stats.isDead) return false
        jumpVelZ = 270f
        stats.stamina -= 8f
        currentAnim = "jump"
        return true
    }

    fun dash(dirX: Float, dirY: Float): Boolean {
        if (dashCooldown > 0f || stats.stamina < 20f || stats.isDead) return false
        isDashing = true
        dashTimer = 0.22f
        dashCooldown = dashCooldownMax
        stats.stamina -= 20f
        val dx = if (dirX == 0f && dirY == 0f) aimDir.x else dirX
        val dy = if (dirX == 0f && dirY == 0f) aimDir.y else dirY
        val len = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(0.001f)
        velocity.set(dx / len * 560f, dy / len * 560f)
        invincibleTimer = 0.25f
        currentAnim = "dash"
        animTimer = 0f
        return true
    }

    fun revive(hpFraction: Float = 0.5f) {
        stats.hp = stats.maxHp * hpFraction.coerceIn(0.1f, 1f)
        currentAnim = "idle"
    }

    abstract fun normalAttack(): AttackResult
    abstract fun heavyAttack(): AttackResult
    abstract fun useSkill(): AttackResult
    abstract fun useUltimate(): AttackResult
}
