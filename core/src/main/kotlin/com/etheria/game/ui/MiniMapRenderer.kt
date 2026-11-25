package com.etheria.game.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.etheria.game.EtheriaGame
import com.etheria.game.enemy.Enemy
import com.etheria.game.player.PartyManager
import com.etheria.game.world.TileType
import com.etheria.game.world.WorldManager

class MiniMapRenderer(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer
) {
    private val font = EtheriaGame.createSmoothFont(0.8f)
    private val MAP_W = 176f
    private val MAP_H = 106f
    private val MAP_X = 16f
    private val MAP_Y = EtheriaGame.VIRTUAL_HEIGHT - 246f
    private val WORLD_W = 3200f
    private val WORLD_H = 1920f

    fun render(world: WorldManager, party: PartyManager, enemies: List<Enemy>) {
        val playerPos = party.activeCharacter.position

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(0.04f, 0.06f, 0.12f, 0.78f)
        shapeRenderer.rect(MAP_X, MAP_Y, MAP_W, MAP_H)

        // Terrain sample from active chunks
        world.getActiveChunks(playerPos).forEach { chunk ->
            val bx = chunk.worldX().toFloat()
            val by = chunk.worldY().toFloat()
            for (tx in 0 until 16 step 2) {
                for (ty in 0 until 16 step 2) {
                    val tile = chunk.tiles[tx][ty]
                    val wx = bx + tx * EtheriaGame.TILE_SIZE
                    val wy = by + ty * EtheriaGame.TILE_SIZE
                    val mx = MAP_X + (wx / WORLD_W) * MAP_W
                    val my = MAP_Y + (wy / WORLD_H) * MAP_H
                    if (mx in MAP_X..(MAP_X + MAP_W - 2f) && my in MAP_Y..(MAP_Y + MAP_H - 2f)) {
                        shapeRenderer.setColor(
                            when (tile.type) {
                                TileType.WATER, TileType.WATERFALL -> Color(0.2f, 0.48f, 0.9f, 0.9f)
                                TileType.TREE -> Color(0.12f, 0.45f, 0.18f, 0.9f)
                                TileType.WALL, TileType.CLIFF -> Color(0.35f, 0.32f, 0.36f, 0.9f)
                                TileType.STONE -> Color(0.55f, 0.56f, 0.6f, 0.9f)
                                TileType.SNOW -> Color(0.85f, 0.9f, 0.98f, 0.9f)
                                else -> Color(0.24f, 0.56f, 0.28f, 0.85f)
                            }
                        )
                        shapeRenderer.rect(mx, my, 3f, 3f)
                    }
                }
            }
        }

        // Chests
        world.chests.filter { !it.opened }.forEach { ch ->
            val mx = MAP_X + (ch.x / WORLD_W) * MAP_W
            val my = MAP_Y + (ch.y / WORLD_H) * MAP_H
            shapeRenderer.setColor(Color.GOLD)
            shapeRenderer.rect(mx - 1.5f, my - 1.5f, 3f, 3f)
        }

        // Waypoints
        world.waypoints.forEach { wp ->
            val mx = MAP_X + (wp.x / WORLD_W) * MAP_W
            val my = MAP_Y + (wp.y / WORLD_H) * MAP_H
            shapeRenderer.setColor(if (wp.unlocked) Color(0.3f, 0.95f, 0.7f, 1f) else Color.GRAY)
            shapeRenderer.circle(mx, my, 3f, 8)
        }

        // Enemies
        enemies.filter { !it.isDead }.forEach { en ->
            val mx = MAP_X + (en.position.x / WORLD_W) * MAP_W
            val my = MAP_Y + (en.position.y / WORLD_H) * MAP_H
            shapeRenderer.setColor(if (en.isBoss) Color(1f, 0.2f, 0.9f, 1f) else Color(0.95f, 0.25f, 0.25f, 0.9f))
            shapeRenderer.circle(mx, my, if (en.isBoss) 4f else 2f, 8)
        }

        // Player
        val px = (MAP_X + (playerPos.x / WORLD_W) * MAP_W).coerceIn(MAP_X + 2f, MAP_X + MAP_W - 2f)
        val py = (MAP_Y + (playerPos.y / WORLD_H) * MAP_H).coerceIn(MAP_Y + 2f, MAP_Y + MAP_H - 2f)
        shapeRenderer.setColor(Color.YELLOW)
        shapeRenderer.circle(px, py, 3.8f, 10)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(0.55f, 0.75f, 0.95f, 0.9f)
        shapeRenderer.rect(MAP_X, MAP_Y, MAP_W, MAP_H)
        shapeRenderer.end()

        batch.begin()
        font.color = Color(0.85f, 0.92f, 1f, 0.9f)
        font.draw(batch, "MINI-MAP", MAP_X + 4f, MAP_Y + 14f)
        batch.end()
    }

    fun dispose() {
        font.dispose()
    }
}
