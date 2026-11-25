package com.etheria.game.world

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.etheria.game.EtheriaGame
import com.etheria.game.player.Element
import kotlin.math.abs

enum class TileType(val tilesetIndex: Int, val passable: Boolean, val isSwimmable: Boolean = false, val isClimbable: Boolean = false) {
    GRASS(0, true),
    DIRT(1, true),
    STONE(2, true),
    WATER(3, true, isSwimmable = true),
    SAND(4, true),
    SNOW(5, true),
    LAVA(6, false),
    TREE(7, false),
    ROCK(8, false),
    WALL(9, false),
    CHEST(10, true),
    WAYPOINT(11, true),
    DUNGEON_ENTRANCE(12, true),
    HERB(13, true),
    WATERFALL(14, false),
    CHEST_OPEN(15, true),
    BRIDGE(16, true),
    CLIFF(17, true, isClimbable = true)
}

data class Tile(
    var type: TileType,
    var passable: Boolean = type.passable
)

class WorldChunk(val chunkX: Int, val chunkY: Int) {
    companion object {
        const val CHUNK_SIZE = 16
        const val TILE_PX = EtheriaGame.TILE_SIZE
    }

    val tiles = Array(CHUNK_SIZE) { Array(CHUNK_SIZE) { Tile(TileType.GRASS) } }

    fun worldX() = chunkX * CHUNK_SIZE * TILE_PX
    fun worldY() = chunkY * CHUNK_SIZE * TILE_PX

    fun getTile(localX: Int, localY: Int): Tile? {
        if (localX !in 0 until CHUNK_SIZE || localY !in 0 until CHUNK_SIZE) return null
        return tiles[localX][localY]
    }
}

class WorldGenerator {

    fun generateChunk(chunk: WorldChunk, seed: Long = 42L) {
        val rng = java.util.Random(seed + chunk.chunkX * 317L + chunk.chunkY * 971L)

        for (lx in 0 until WorldChunk.CHUNK_SIZE) {
            for (ly in 0 until WorldChunk.CHUNK_SIZE) {
                val wx = chunk.chunkX * WorldChunk.CHUNK_SIZE + lx
                val wy = chunk.chunkY * WorldChunk.CHUNK_SIZE + ly

                val tileType = determineTile(wx, wy, rng)
                chunk.tiles[lx][ly] = Tile(tileType, tileType.passable)
            }
        }
    }

    private fun determineTile(tx: Int, ty: Int, rng: java.util.Random): TileType {
        // World outer border (100x60 tiles => 3200x1920 px)
        if (tx <= 1 || ty <= 1 || tx >= 98 || ty >= 58) return TileType.WALL

        // Main east-west road at ty in 14..16
        val onMainRoad = ty in 14..16 && tx in 6..92

        // 1. Starter Village ("Aethelgard"): tx 4..20, ty 8..22
        if (tx in 4..20 && ty in 8..22) {
            if (tx in 8..16 && ty in 11..19) return TileType.STONE
            if ((tx in 6..7 || tx in 17..18) && ty in 18..20) return TileType.WALL // Village cottages
            if (onMainRoad) return TileType.DIRT
            return TileType.GRASS
        }

        // 2. Crystal River & Waterfall running vertically at tx 46..49
        if (tx in 46..49) {
            if (ty in 13..17) return TileType.BRIDGE // Wooden crossing bridge
            if (ty in 32..36) return TileType.WATERFALL
            return TileType.WATER
        }
        // Riverbanks sand
        if (tx == 45 || tx == 50) {
            if (ty !in 13..17) return TileType.SAND
        }

        // 3. Shattered Cliffs & Ancient Temple (tx 55..78, ty 8..34)
        if (tx in 55..78 && ty in 8..34) {
            // Climbable cliff ridge at ty == 23 with opening at tx 65..67
            if (ty == 23 && tx !in 65..67) return TileType.CLIFF
            // Temple stone plaza
            if (tx in 60..74 && ty in 11..21) return TileType.STONE
            // Temple outer pillars
            if ((tx == 60 || tx == 74) && ty in 11..21 && ty % 3 == 0) return TileType.WALL
            // Snow peak in far north (ty > 40)
            if (ty > 40) return TileType.SNOW
            if (onMainRoad) return TileType.DIRT
            return if (rng.nextFloat() < 0.08f) TileType.ROCK else TileType.GRASS
        }

        // 4. Shadow Lord Boss Arena & Obsidian Ruins (tx 80..96, ty 8..30)
        if (tx in 80..96 && ty in 8..30) {
            if (tx == 80 && ty !in 13..17) return TileType.WALL
            if ((ty == 8 || ty == 30)) return TileType.WALL
            if (tx in 82..94 && ty in 10..28) return TileType.STONE
        }

        // Northern Snow Biome
        if (ty >= 42) {
            if (rng.nextFloat() < 0.06f) return TileType.ROCK
            return TileType.SNOW
        }

        if (onMainRoad) return TileType.DIRT

        // Emerald Forest & Open Plains
        val distToRoad = abs(ty - 15)
        if (distToRoad > 3) {
            val roll = rng.nextFloat()
            if (roll < 0.09f) return TileType.TREE
            if (roll < 0.12f) return TileType.ROCK
        }
        return TileType.GRASS
    }
}

enum class WeatherType { CLEAR, RAIN, FOG, STORM }

data class Chest(val id: String, val x: Float, val y: Float, val items: List<Item>, var opened: Boolean = false)
data class Waypoint(val name: String, val x: Float, val y: Float, var unlocked: Boolean = false)
data class Collectible(val id: String, val name: String, val targetId: String, val x: Float, val y: Float, var collected: Boolean = false)
data class PuzzlePillar(val id: Int, val name: String, val element: Element, val x: Float, val y: Float, var activated: Boolean = false)
data class NPC(
    val name: String,
    val x: Float,
    val y: Float,
    val dialogue: List<String>,
    val hasQuest: Boolean = false,
    val questId: String = "",
    var dialogueIndex: Int = 0
)

class WorldManager {

    private val chunks = mutableMapOf<Long, WorldChunk>()
    private val generator = WorldGenerator()
    private val activeChunkRange = 2

    val chests = mutableListOf<Chest>()
    val waypoints = mutableListOf<Waypoint>()
    val npcs = mutableListOf<NPC>()
    val collectibles = mutableListOf<Collectible>()
    val puzzlePillars = mutableListOf<PuzzlePillar>()
    var puzzleSolved = false
    var dungeonEntered = false

    val healingFountainPos = Vector2(384f, 448f)
    val dungeonPortalPos = Vector2(2144f, 544f)

    // Day/night cycle
    var timeOfDay = 0.42f // starts in bright daytime
    private val daySpeed = 0.004f

    // Weather
    var weatherType = WeatherType.CLEAR
    var weatherTimer = 0f
    private val weatherChangeCooldown = 55f

    init {
        spawnWorldObjects()
    }

    private fun spawnWorldObjects() {
        chests.clear()
        waypoints.clear()
        npcs.clear()
        collectibles.clear()
        puzzlePillars.clear()

        chests.addAll(listOf(
            Chest("chest_village", 520f, 560f, listOf(
                Item("Tidebreaker Blade", ItemType.WEAPON, power = 55, description = "Infuses attacks with Water", element = Element.WATER),
                Item("Health Potion", ItemType.CONSUMABLE, power = 350, quantity = 2, description = "Restores 350 HP")
            )),
            Chest("chest_forest", 1080f, 700f, listOf(
                Item("Storm Bow", ItemType.WEAPON, power = 75, description = "Charged with Wind energy", element = Element.WIND),
                Item("Forest Herb", ItemType.MATERIAL, quantity = 2, description = "Aromatic healing herb")
            )),
            Chest("chest_river", 1560f, 940f, listOf(
                Item("Aqua Elixir", ItemType.CONSUMABLE, power = 450, quantity = 2, description = "Restores 450 HP", element = Element.WATER),
                Item("Steel Mail", ItemType.ARMOR, power = 65, description = "Heavy plated armor (+65 DEF)")
            )),
            Chest("chest_temple", 2144f, 410f, listOf(
                Item("Sunfire Greatsword", ItemType.WEAPON, power = 110, description = "Forged in ancient temple flames", element = Element.FIRE),
                Item("Health Potion", ItemType.CONSUMABLE, power = 350, quantity = 3, description = "Restores 350 HP")
            ))
        ))

        waypoints.addAll(listOf(
            Waypoint("Aethelgard Village", 384f, 496f, unlocked = true),
            Waypoint("Emerald Forest", 1180f, 496f, unlocked = false),
            Waypoint("Crystal River", 1560f, 496f, unlocked = false),
            Waypoint("Ancient Temple", 2144f, 496f, unlocked = false),
            Waypoint("Shadow Lord Gate", 2520f, 496f, unlocked = false)
        ))

        npcs.addAll(listOf(
            NPC("Elder Eryn", 352f, 416f,
                listOf(
                    "Welcome to Aethelgard, traveler! The Shadow Lord has corrupted Etheria.",
                    "Head east into the Emerald Forest, defeat 5 Slimes and the Goblin Leader.",
                    "Activate the 3 Ancient Rune Pillars at the Temple before facing the Boss!"
                ),
                hasQuest = true,
                questId = "main_01"
            ),
            NPC("Blacksmith Doren", 460f, 416f,
                listOf(
                    "Need stronger steel? Open your Inventory (INV) to equip & upgrade weapons!",
                    "Bring me 3 Forest Herbs and slay 2 Crypt Skeletons for extra gear."
                ),
                hasQuest = true,
                questId = "side_01"
            ),
            NPC("Scout Mira", 310f, 520f,
                listOf(
                    "Watch the Crystal River ahead — you can swim across water or take the bridge.",
                    "You can also climb the Shattered Cliffs north of the Temple!"
                ),
                hasQuest = false
            )
        ))

        collectibles.addAll(listOf(
            Collectible("herb_1", "Forest Herb", "forest_herb", 720f, 620f),
            Collectible("herb_2", "Forest Herb", "forest_herb", 920f, 340f),
            Collectible("herb_3", "Forest Herb", "forest_herb", 1120f, 610f),
            Collectible("herb_4", "Forest Herb", "forest_herb", 1320f, 330f),
            Collectible("crystal_1", "Storm Shard", "storm_shard", 1620f, 680f)
        ))

        puzzlePillars.addAll(listOf(
            PuzzlePillar(0, "Pyro Rune", Element.FIRE, 2020f, 620f),
            PuzzlePillar(1, "Cryo Rune", Element.ICE, 2144f, 640f),
            PuzzlePillar(2, "Geo Rune", Element.EARTH, 2268f, 620f)
        ))
    }

    private fun chunkKey(cx: Int, cy: Int) = (cx.toLong() shl 20) or (cy.toLong() and 0xFFFFFL)

    fun getOrCreateChunk(cx: Int, cy: Int): WorldChunk {
        val key = chunkKey(cx, cy)
        return chunks.getOrPut(key) {
            WorldChunk(cx, cy).also { generator.generateChunk(it) }
        }
    }

    fun getTileAtWorld(worldX: Float, worldY: Float): Tile? {
        val tx = (worldX / EtheriaGame.TILE_SIZE).toInt()
        val ty = (worldY / EtheriaGame.TILE_SIZE).toInt()
        val cx = Math.floorDiv(tx, WorldChunk.CHUNK_SIZE)
        val cy = Math.floorDiv(ty, WorldChunk.CHUNK_SIZE)
        val lx = Math.floorMod(tx, WorldChunk.CHUNK_SIZE)
        val ly = Math.floorMod(ty, WorldChunk.CHUNK_SIZE)
        return getOrCreateChunk(cx, cy).getTile(lx, ly)
    }

    fun isPassable(worldX: Float, worldY: Float): Boolean {
        val tile = getTileAtWorld(worldX, worldY) ?: return false
        return tile.passable
    }

    fun isWaterAt(worldX: Float, worldY: Float): Boolean {
        return getTileAtWorld(worldX, worldY)?.type == TileType.WATER
    }

    fun isCliffAt(worldX: Float, worldY: Float): Boolean {
        return getTileAtWorld(worldX, worldY)?.type == TileType.CLIFF
    }

    fun update(delta: Float, playerPos: Vector2) {
        val pcx = Math.floorDiv((playerPos.x / EtheriaGame.TILE_SIZE).toInt(), WorldChunk.CHUNK_SIZE)
        val pcy = Math.floorDiv((playerPos.y / EtheriaGame.TILE_SIZE).toInt(), WorldChunk.CHUNK_SIZE)
        for (dx in -activeChunkRange..activeChunkRange) {
            for (dy in -activeChunkRange..activeChunkRange) {
                getOrCreateChunk(pcx + dx, pcy + dy)
            }
        }

        timeOfDay = (timeOfDay + daySpeed * delta) % 1f

        weatherTimer += delta
        if (weatherTimer > weatherChangeCooldown) {
            weatherTimer = 0f
            weatherType = WeatherType.values().random()
        }
    }

    fun getAmbientColor(): Color {
        return when {
            timeOfDay < 0.22f -> Color(0.45f, 0.48f, 0.72f, 1f) // Night (still clearly readable!)
            timeOfDay < 0.32f -> Color(0.95f, 0.72f, 0.58f, 1f) // Dawn
            timeOfDay < 0.68f -> Color(1f, 1f, 1f, 1f)          // Day
            timeOfDay < 0.78f -> Color(0.95f, 0.74f, 0.56f, 1f) // Dusk
            else -> Color(0.45f, 0.48f, 0.72f, 1f)              // Night
        }
    }

    fun getActiveChunks(playerPos: Vector2): List<WorldChunk> {
        val pcx = Math.floorDiv((playerPos.x / EtheriaGame.TILE_SIZE).toInt(), WorldChunk.CHUNK_SIZE)
        val pcy = Math.floorDiv((playerPos.y / EtheriaGame.TILE_SIZE).toInt(), WorldChunk.CHUNK_SIZE)
        return (-activeChunkRange..activeChunkRange).flatMap { dx ->
            (-activeChunkRange..activeChunkRange).map { dy ->
                getOrCreateChunk(pcx + dx, pcy + dy)
            }
        }
    }
}
