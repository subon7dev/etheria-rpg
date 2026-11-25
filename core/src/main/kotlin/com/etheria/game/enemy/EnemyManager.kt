package com.etheria.game.enemy

import com.badlogic.gdx.math.Vector2

class EnemyManager {

    val enemies = mutableListOf<Enemy>()

    fun spawnInitialEnemies() {
        enemies.clear()

        // Emerald Forest: Slimes & Goblin Camp
        spawn(EnemyType.SLIME, 780f, 440f)
        spawn(EnemyType.SLIME, 880f, 520f)
        spawn(EnemyType.SLIME, 960f, 380f)
        spawn(EnemyType.SLIME, 1080f, 640f)
        spawn(EnemyType.SLIME, 1140f, 470f)

        spawn(EnemyType.GOBLIN, 1280f, 420f)
        spawn(EnemyType.GOBLIN, 1360f, 560f)
        enemies.add(Goblin(1420f, 490f, isLeader = true))

        // Riverbank & Cliffs: Archers & Flying Wyverns
        spawn(EnemyType.ARCHER_ENEMY, 1520f, 880f)
        spawn(EnemyType.FLYING_MONSTER, 1650f, 760f)
        spawn(EnemyType.FLYING_MONSTER, 1950f, 940f)

        // Shattered Temple & Crypt: Skeletons, Elite Golem, Dark Knights
        spawn(EnemyType.SKELETON, 1980f, 420f)
        spawn(EnemyType.SKELETON, 2120f, 520f)
        spawn(EnemyType.ARCHER_ENEMY, 2240f, 460f)
        spawn(EnemyType.ELITE, 2280f, 720f)
        spawn(EnemyType.DARK_KNIGHT, 2440f, 480f)

        // Shadow Lord Arena (East Wing)
        spawn(EnemyType.BOSS, 2780f, 540f)
    }

    fun spawn(type: EnemyType, x: Float, y: Float) {
        val enemy: Enemy = when (type) {
            EnemyType.SLIME -> Slime(x, y)
            EnemyType.GOBLIN -> Goblin(x, y)
            EnemyType.SKELETON -> Skeleton(x, y)
            EnemyType.ARCHER_ENEMY -> EnemyArcher(x, y)
            EnemyType.FLYING_MONSTER -> FlyingMonster(x, y)
            EnemyType.DARK_KNIGHT -> DarkKnight(x, y)
            EnemyType.ELITE -> EliteGolem(x, y)
            EnemyType.BOSS -> ShadowLord(x, y)
        }
        enemies.add(enemy)
    }

    fun update(delta: Float, playerPos: Vector2) {
        enemies.forEach { it.update(delta, playerPos) }
        enemies.removeAll { it.isDead && it.deathTimer <= 0f }
    }

    fun getAliveEnemies() = enemies.filter { !it.isDead }

    fun getEnemiesNearPlayer(playerPos: Vector2, range: Float = 500f): List<Enemy> {
        return enemies.filter {
            !it.isDead && Vector2.dst(it.position.x, it.position.y, playerPos.x, playerPos.y) < range
        }
    }

    fun clearAll() = enemies.clear()
}
