package com.etheria.game.combat

import com.badlogic.gdx.math.Vector2
import com.etheria.game.enemy.Enemy
import com.etheria.game.player.AttackResult
import com.etheria.game.player.Element
import com.etheria.game.player.PlayableCharacter
import kotlin.random.Random

data class DamageNumber(
    var value: Float,
    var x: Float,
    var y: Float,
    var lifetime: Float = 1.15f,
    var isCrit: Boolean = false,
    var element: Element = Element.NONE,
    var label: String = ""
)

data class HitEffect(
    var name: String,
    var x: Float,
    var y: Float,
    var lifetime: Float = 0.45f,
    var maxLifetime: Float = 0.45f,
    var radius: Float = 24f,
    var element: Element = Element.NONE
)

data class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var damage: Float,
    var element: Element,
    var fromPlayer: Boolean,
    var lifetime: Float = 1.6f,
    var radius: Float = 14f,
    var knockbackForce: Float = 90f
)

class CombatEngine {

    val damageNumbers = mutableListOf<DamageNumber>()
    val hitEffects = mutableListOf<HitEffect>()
    val projectiles = mutableListOf<Projectile>()

    /**
     * Computes elemental reaction multiplier across all 6 elements:
     * Fire, Water, Ice, Lightning, Wind, Earth.
     */
    private fun evaluateElementalReaction(atk: Element, targetPrimed: Element, targetBase: Element): Pair<Float, String> {
        val activeTarget = if (targetPrimed != Element.NONE) targetPrimed else targetBase
        if (atk == Element.NONE || activeTarget == Element.NONE) return 1.0f to ""
        return when {
            (atk == Element.FIRE && activeTarget == Element.ICE) ||
                (atk == Element.ICE && activeTarget == Element.FIRE) -> 1.55f to "MELT!"
            (atk == Element.FIRE && activeTarget == Element.WATER) ||
                (atk == Element.WATER && activeTarget == Element.FIRE) -> 1.50f to "VAPORIZE!"
            (atk == Element.FIRE && activeTarget == Element.LIGHTNING) ||
                (atk == Element.LIGHTNING && activeTarget == Element.FIRE) -> 1.45f to "OVERLOAD!"
            (atk == Element.WATER && activeTarget == Element.LIGHTNING) ||
                (atk == Element.LIGHTNING && activeTarget == Element.WATER) -> 1.40f to "ELECTRO!"
            (atk == Element.ICE && activeTarget == Element.LIGHTNING) ||
                (atk == Element.LIGHTNING && activeTarget == Element.ICE) -> 1.35f to "SUPERCONDUCT!"
            atk == Element.WIND && activeTarget != Element.WIND -> 1.30f to "SWIRL!"
            atk == Element.EARTH && activeTarget != Element.EARTH -> 1.25f to "SHATTER!"
            atk == targetBase -> 0.8f to ""
            else -> 1.05f to ""
        }
    }

    fun processAttack(
        attack: AttackResult,
        attacker: PlayableCharacter,
        enemies: List<Enemy>
    ): List<Enemy> {
        if (attack.damage <= 0f) return emptyList()

        if (attack.healAmount > 0f) {
            attacker.stats.hp = minOf(attacker.stats.maxHp, attacker.stats.hp + attack.healAmount)
            damageNumbers.add(
                DamageNumber(
                    value = attack.healAmount,
                    x = attacker.position.x,
                    y = attacker.position.y + 54f,
                    element = Element.WIND,
                    label = "+${attack.healAmount.toInt()}"
                )
            )
        }

        if (attack.isProjectile) {
            spawnPlayerProjectiles(attack, attacker)
            // Ultimate AOE skills that also fire projectiles still hit nearby enemies
            if (attack.radius <= 0f) return emptyList()
        }

        val hit = mutableListOf<Enemy>()
        val hitRange = if (attack.radius > 0f) attack.radius else 72f

        hitEffects.add(
            HitEffect(
                name = attack.effectName,
                x = attacker.position.x + if (attack.radius > 0f) 0f else attacker.aimDir.x * 28f,
                y = attacker.position.y + if (attack.radius > 0f) 0f else attacker.aimDir.y * 28f,
                radius = hitRange,
                element = attack.element
            )
        )

        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dist = Vector2.dst(attacker.position.x, attacker.position.y, enemy.position.x, enemy.position.y)
            if (dist <= hitRange) {
                applyHitToEnemy(attack.damage, attack.element, attacker.stats.critChance, attacker.stats.critMultiplier, attack.knockback, enemy)
                hit.add(enemy)
            }
        }
        return hit
    }

    private fun spawnPlayerProjectiles(attack: AttackResult, attacker: PlayableCharacter) {
        val count = attack.projectileCount.coerceAtLeast(1)
        val baseAngle = attacker.aimDir.angleDeg()
        val spread = 14f
        for (i in 0 until count) {
            val offsetDeg = (i - (count - 1) / 2f) * spread
            val dir = Vector2(1f, 0f).setAngleDeg(baseAngle + offsetDeg)
            projectiles.add(
                Projectile(
                    x = attacker.position.x + dir.x * 18f,
                    y = attacker.position.y + 16f + dir.y * 18f,
                    vx = dir.x * attack.projectileSpeed,
                    vy = dir.y * attack.projectileSpeed,
                    damage = attack.damage,
                    element = attack.element,
                    fromPlayer = true,
                    radius = 16f,
                    knockbackForce = if (attack.isHeavy) 160f else 80f
                )
            )
        }
    }

    fun spawnEnemyProjectile(origin: Vector2, dir: Vector2, damage: Float, element: Element, radialBurst: Boolean) {
        if (radialBurst) {
            for (k in 0 until 8) {
                val v = Vector2(1f, 0f).setAngleDeg(k * 45f)
                projectiles.add(
                    Projectile(
                        x = origin.x + v.x * 24f,
                        y = origin.y + 18f + v.y * 24f,
                        vx = v.x * 280f,
                        vy = v.y * 280f,
                        damage = damage,
                        element = element,
                        fromPlayer = false,
                        lifetime = 2.1f,
                        radius = 16f
                    )
                )
            }
        } else {
            val n = dir.cpy().nor()
            projectiles.add(
                Projectile(
                    x = origin.x + n.x * 20f,
                    y = origin.y + 16f + n.y * 20f,
                    vx = n.x * 340f,
                    vy = n.y * 340f,
                    damage = damage,
                    element = element,
                    fromPlayer = false,
                    lifetime = 2.0f,
                    radius = 14f
                )
            )
        }
    }

    private fun applyHitToEnemy(
        rawDamage: Float,
        element: Element,
        critChance: Float,
        critMult: Float,
        knockback: Vector2,
        enemy: Enemy
    ) {
        val isCrit = Random.nextFloat() < critChance
        val (elemMult, reactionLabel) = evaluateElementalReaction(element, enemy.primedElement, enemy.element)
        var dmg = rawDamage * elemMult
        if (isCrit) dmg *= critMult

        val actual = enemy.takeDamage(dmg, element)
        if (actual <= 0f) {
            damageNumbers.add(DamageNumber(0f, enemy.position.x, enemy.position.y + 42f, label = "DODGE!"))
            return
        }

        val text = buildString {
            if (reactionLabel.isNotEmpty()) append("$reactionLabel ")
            if (isCrit) append("CRIT ")
            append(actual.toInt())
        }

        damageNumbers.add(
            DamageNumber(
                value = actual,
                x = enemy.position.x + Random.nextFloat() * 18f - 9f,
                y = enemy.position.y + 44f,
                isCrit = isCrit,
                element = element,
                label = text
            )
        )

        hitEffects.add(
            HitEffect(
                name = reactionLabel.ifEmpty { "hit" },
                x = enemy.position.x,
                y = enemy.position.y + 18f,
                lifetime = 0.28f,
                maxLifetime = 0.28f,
                radius = 22f,
                element = element
            )
        )

        if (knockback.len2() > 0f) {
            enemy.applyKnockback(knockback)
        }
    }

    fun update(
        delta: Float,
        attacker: PlayableCharacter,
        enemies: List<Enemy>,
        onEnemyKilled: (Enemy) -> Unit,
        onPlayerHit: (Float, Element) -> Unit
    ) {
        // Update projectiles & check collisions
        val toRemove = mutableListOf<Projectile>()
        for (p in projectiles) {
            p.x += p.vx * delta
            p.y += p.vy * delta
            p.lifetime -= delta
            if (p.lifetime <= 0f) {
                toRemove.add(p)
                continue
            }

            if (p.fromPlayer) {
                for (enemy in enemies) {
                    if (enemy.isDead) continue
                    if (Vector2.dst(p.x, p.y, enemy.position.x, enemy.position.y + 16f) <= p.radius + 18f) {
                        val wasDead = enemy.isDead
                        val kb = Vector2(p.vx, p.vy).nor().scl(p.knockbackForce)
                        applyHitToEnemy(p.damage, p.element, attacker.stats.critChance, attacker.stats.critMultiplier, kb, enemy)
                        if (!wasDead && enemy.isDead) onEnemyKilled(enemy)
                        toRemove.add(p)
                        break
                    }
                }
            } else {
                if (attacker.invincibleTimer <= 0f && !attacker.stats.isDead) {
                    if (Vector2.dst(p.x, p.y, attacker.position.x, attacker.position.y + 16f) <= p.radius + 16f) {
                        onPlayerHit(p.damage, p.element)
                        toRemove.add(p)
                    }
                }
            }
        }
        projectiles.removeAll(toRemove)

        damageNumbers.removeAll { it.lifetime <= 0f }
        damageNumbers.forEach {
            it.lifetime -= delta
            it.y += 38f * delta
        }

        hitEffects.removeAll { it.lifetime <= 0f }
        hitEffects.forEach { it.lifetime -= delta }
    }
}
