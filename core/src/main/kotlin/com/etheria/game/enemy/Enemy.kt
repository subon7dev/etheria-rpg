package com.etheria.game.enemy

import com.badlogic.gdx.math.Vector2
import com.etheria.game.player.Element
import kotlin.random.Random

data class EnemyStats(
    var maxHp: Float,
    var hp: Float,
    var attackPower: Float,
    var defense: Float,
    var speed: Float,
    var expReward: Float,
    var detectionRange: Float = 260f,
    var attackRange: Float = 60f
) {
    val isDead get() = hp <= 0f
}

enum class EnemyState { IDLE, PATROL, CHASE, ATTACK, DODGE, HURT, DEAD }
enum class EnemyType { SLIME, GOBLIN, SKELETON, DARK_KNIGHT, ARCHER_ENEMY, FLYING_MONSTER, ELITE, BOSS }

data class EnemyAttackEvent(
    val origin: Vector2,
    val direction: Vector2,
    val damage: Float,
    val element: Element,
    val isProjectile: Boolean = false,
    val isRadialBurst: Boolean = false,
    val range: Float = 68f
)

abstract class Enemy(
    val type: EnemyType,
    val element: Element = Element.NONE,
    spawnX: Float = 0f,
    spawnY: Float = 0f,
    val sheetPath: String,
    val spriteSize: Int = 48
) {
    val position = Vector2(spawnX, spawnY)
    val spawnPoint = Vector2(spawnX, spawnY)
    val velocity = Vector2()
    abstract val stats: EnemyStats

    var state = EnemyState.IDLE
    var stateTimer = 0f
    var patrolDir = if (Random.nextBoolean()) 1f else -1f
    var facingRight = true
    var currentAnim = "idle"
    var animTimer = 0f
    var attackCooldown = 0f
    var dodgeTimer = 0f
    var dodgeCooldown = 0f
    var knockbackVelocity = Vector2()
    var hurtTimer = 0f
    var isDead = false
    var deathTimer = 0f
    var canFly = false
    var canDodge = false
    var preferredMinRange = 0f

    // Elemental priming for reactions across all 6 elements
    var primedElement = Element.NONE
    var primedTimer = 0f

    // Boss specific
    var bossPhase = 0
    var isBoss = false
    var displayName: String = type.name.replace("_", " ")

    val pendingAttacks = mutableListOf<EnemyAttackEvent>()
    abstract val attackCooldownMax: Float

    fun takeDamage(dmg: Float, hitElement: Element = Element.NONE): Float {
        if (state == EnemyState.DODGE) return 0f
        val actual = maxOf(1f, dmg - stats.defense * 0.2f)
        stats.hp = maxOf(0f, stats.hp - actual)
        if (hitElement != Element.NONE) {
            primedElement = hitElement
            primedTimer = 5f
        }
        if (stats.isDead && !isDead) {
            isDead = true
            state = EnemyState.DEAD
            deathTimer = 1.0f
            animTimer = 0f
            currentAnim = "death"
        } else if (!isDead) {
            if (canDodge && dodgeCooldown <= 0f && Random.nextFloat() < 0.28f) {
                state = EnemyState.DODGE
                dodgeTimer = 0.35f
                dodgeCooldown = 3.5f
                currentAnim = "move"
            } else {
                state = EnemyState.HURT
                hurtTimer = 0.22f
                animTimer = 0f
                currentAnim = "hurt"
            }
        }
        return actual
    }

    fun applyKnockback(kb: Vector2) {
        if (isBoss) {
            knockbackVelocity.set(kb.x * 0.2f, kb.y * 0.2f)
        } else {
            knockbackVelocity.set(kb)
        }
    }

    open fun update(delta: Float, playerPos: Vector2) {
        animTimer += delta
        if (primedTimer > 0f) {
            primedTimer -= delta
            if (primedTimer <= 0f) primedElement = Element.NONE
        }

        if (isDead) {
            deathTimer -= delta
            currentAnim = "death"
            return
        }

        val decay = maxOf(0f, 1f - 9f * delta)
        knockbackVelocity.scl(decay)
        if (knockbackVelocity.len() < 4f) knockbackVelocity.setZero()

        stateTimer += delta
        if (attackCooldown > 0f) attackCooldown -= delta
        if (dodgeCooldown > 0f) dodgeCooldown -= delta

        val distToPlayer = Vector2.dst(position.x, position.y, playerPos.x, playerPos.y)
        val dirToPlayer = Vector2(playerPos.x - position.x, playerPos.y - position.y)
        if (dirToPlayer.len2() > 0.001f) dirToPlayer.nor() else dirToPlayer.set(1f, 0f)

        when (state) {
            EnemyState.IDLE -> {
                currentAnim = "idle"
                if (stateTimer > 1.8f) {
                    state = EnemyState.PATROL
                    stateTimer = 0f
                }
                if (distToPlayer < stats.detectionRange) {
                    state = EnemyState.CHASE
                    stateTimer = 0f
                }
            }
            EnemyState.PATROL -> {
                currentAnim = "move"
                val speed = stats.speed * 0.45f
                position.x += patrolDir * speed * delta
                facingRight = patrolDir >= 0f
                if (stateTimer > 2.6f || kotlin.math.abs(position.x - spawnPoint.x) > 120f) {
                    patrolDir *= -1f
                    stateTimer = 0f
                    state = EnemyState.IDLE
                }
                if (distToPlayer < stats.detectionRange) {
                    state = EnemyState.CHASE
                    stateTimer = 0f
                }
            }
            EnemyState.CHASE -> {
                currentAnim = "move"
                facingRight = dirToPlayer.x >= 0f
                if (distToPlayer > stats.detectionRange * 1.65f) {
                    state = EnemyState.PATROL
                    stateTimer = 0f
                    return
                }
                if (distToPlayer <= stats.attackRange) {
                    state = EnemyState.ATTACK
                    stateTimer = 0f
                    return
                }
                position.x += dirToPlayer.x * stats.speed * delta
                position.y += dirToPlayer.y * stats.speed * delta
            }
            EnemyState.ATTACK -> {
                currentAnim = "attack"
                facingRight = dirToPlayer.x >= 0f
                // Kite away if ranged archer and player is too close
                if (preferredMinRange > 0f && distToPlayer < preferredMinRange) {
                    position.x -= dirToPlayer.x * stats.speed * 0.7f * delta
                    position.y -= dirToPlayer.y * stats.speed * 0.7f * delta
                }
                if (attackCooldown <= 0f) {
                    performAttack(playerPos, dirToPlayer)
                    attackCooldown = attackCooldownMax
                    animTimer = 0f
                }
                if (distToPlayer > stats.attackRange * 1.3f) {
                    state = EnemyState.CHASE
                    stateTimer = 0f
                }
            }
            EnemyState.DODGE -> {
                currentAnim = "move"
                dodgeTimer -= delta
                position.x -= dirToPlayer.x * stats.speed * 1.8f * delta
                position.y -= dirToPlayer.y * stats.speed * 1.8f * delta
                if (dodgeTimer <= 0f) {
                    state = EnemyState.CHASE
                    stateTimer = 0f
                }
            }
            EnemyState.HURT -> {
                currentAnim = "hurt"
                hurtTimer -= delta
                if (hurtTimer <= 0f) {
                    state = EnemyState.CHASE
                    stateTimer = 0f
                }
            }
            EnemyState.DEAD -> {}
        }

        position.x += knockbackVelocity.x * delta
        position.y += knockbackVelocity.y * delta
        position.x = position.x.coerceIn(64f, 3136f)
        position.y = position.y.coerceIn(64f, 1856f)

        onPhaseCheck()
    }

    protected open fun performAttack(playerPos: Vector2, dir: Vector2) {
        pendingAttacks.add(
            EnemyAttackEvent(
                origin = position.cpy(),
                direction = dir.cpy(),
                damage = stats.attackPower,
                element = element,
                isProjectile = false,
                range = stats.attackRange + 14f
            )
        )
    }

    protected open fun onPhaseCheck() {}

    fun getHpPercent() = if (stats.maxHp > 0f) (stats.hp / stats.maxHp).coerceIn(0f, 1f) else 0f
}

class Slime(x: Float, y: Float) : Enemy(
    EnemyType.SLIME, Element.WATER, x, y, "sprites/enemies/slime.png", 48
) {
    override val stats = EnemyStats(140f, 140f, 24f, 6f, 88f, 35f, 210f, 54f)
    override val attackCooldownMax = 1.4f
    init { displayName = "Hydro Slime" }
}

class Goblin(x: Float, y: Float, val isLeader: Boolean = false) : Enemy(
    EnemyType.GOBLIN, Element.FIRE, x, y, "sprites/enemies/goblin.png", 48
) {
    override val stats = if (isLeader) {
        EnemyStats(420f, 420f, 55f, 20f, 138f, 140f, 280f, 64f)
    } else {
        EnemyStats(210f, 210f, 38f, 12f, 128f, 55f, 240f, 58f)
    }
    override val attackCooldownMax = if (isLeader) 1.05f else 1.25f
    init {
        canDodge = true
        displayName = if (isLeader) "Goblin Leader" else "Goblin Raider"
    }
}

class Skeleton(x: Float, y: Float) : Enemy(
    EnemyType.SKELETON, Element.EARTH, x, y, "sprites/enemies/skeleton.png", 48
) {
    override val stats = EnemyStats(310f, 310f, 56f, 24f, 108f, 85f, 250f, 66f)
    override val attackCooldownMax = 1.55f
    init { displayName = "Crypt Skeleton" }
}

class EnemyArcher(x: Float, y: Float) : Enemy(
    EnemyType.ARCHER_ENEMY, Element.WIND, x, y, "sprites/enemies/archer_enemy.png", 48
) {
    override val stats = EnemyStats(190f, 190f, 44f, 10f, 122f, 75f, 330f, 230f)
    override val attackCooldownMax = 1.8f

    init {
        canDodge = true
        preferredMinRange = 100f
        displayName = "Shadow Archer"
    }

    override fun performAttack(playerPos: Vector2, dir: Vector2) {
        pendingAttacks.add(
            EnemyAttackEvent(
                origin = position.cpy(),
                direction = dir.cpy(),
                damage = stats.attackPower,
                element = element,
                isProjectile = true
            )
        )
    }
}

class FlyingMonster(x: Float, y: Float) : Enemy(
    EnemyType.FLYING_MONSTER, Element.LIGHTNING, x, y, "sprites/enemies/flying_monster.png", 48
) {
    override val stats = EnemyStats(230f, 230f, 48f, 12f, 152f, 90f, 300f, 170f)
    override val attackCooldownMax = 1.6f

    init {
        canFly = true
        displayName = "Storm Wyvern"
    }

    override fun performAttack(playerPos: Vector2, dir: Vector2) {
        pendingAttacks.add(
            EnemyAttackEvent(
                origin = position.cpy(),
                direction = dir.cpy(),
                damage = stats.attackPower,
                element = element,
                isProjectile = true
            )
        )
    }
}

class DarkKnight(x: Float, y: Float) : Enemy(
    EnemyType.DARK_KNIGHT, Element.FIRE, x, y, "sprites/enemies/darkKnight.png", 56
) {
    override val stats = EnemyStats(640f, 640f, 88f, 48f, 112f, 180f, 280f, 78f)
    override val attackCooldownMax = 1.65f
    init {
        canDodge = true
        displayName = "Dark Knight"
    }
}

class EliteGolem(x: Float, y: Float) : Enemy(
    EnemyType.ELITE, Element.EARTH, x, y, "sprites/enemies/elite_golem.png", 56
) {
    override val stats = EnemyStats(950f, 950f, 105f, 65f, 82f, 280f, 270f, 86f)
    override val attackCooldownMax = 2.0f
    init { displayName = "Ancient Golem (Elite)" }
}

class ShadowLord(x: Float, y: Float) : Enemy(
    EnemyType.BOSS, Element.LIGHTNING, x, y, "sprites/enemies/shadow_lord.png", 72
) {
    override val stats = EnemyStats(3800f, 3800f, 130f, 75f, 118f, 1500f, 440f, 160f)
    override val attackCooldownMax = 1.35f
    private var attackCycle = 0

    init {
        isBoss = true
        displayName = "Shadow Lord Malakor"
    }

    override fun performAttack(playerPos: Vector2, dir: Vector2) {
        attackCycle = (attackCycle + 1) % 3
        val dist = Vector2.dst(position.x, position.y, playerPos.x, playerPos.y)
        when {
            bossPhase >= 1 && attackCycle == 0 -> {
                // Radial shadow lightning barrage
                pendingAttacks.add(
                    EnemyAttackEvent(
                        origin = position.cpy(),
                        direction = dir.cpy(),
                        damage = stats.attackPower * 0.9f,
                        element = Element.LIGHTNING,
                        isProjectile = true,
                        isRadialBurst = true
                    )
                )
            }
            dist > 88f -> {
                pendingAttacks.add(
                    EnemyAttackEvent(
                        origin = position.cpy(),
                        direction = dir.cpy(),
                        damage = stats.attackPower * 0.95f,
                        element = Element.LIGHTNING,
                        isProjectile = true
                    )
                )
            }
            else -> {
                pendingAttacks.add(
                    EnemyAttackEvent(
                        origin = position.cpy(),
                        direction = dir.cpy(),
                        damage = stats.attackPower,
                        element = Element.FIRE,
                        isProjectile = false,
                        range = 105f
                    )
                )
            }
        }
    }

    override fun onPhaseCheck() {
        val hpPct = getHpPercent()
        val newPhase = when {
            hpPct > 0.66f -> 0
            hpPct > 0.33f -> 1
            else -> 2
        }
        if (newPhase != bossPhase) {
            bossPhase = newPhase
            stats.speed += 18f
            stats.attackPower += 24f
        }
    }
}
