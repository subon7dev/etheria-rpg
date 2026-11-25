package com.etheria.game.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.Box2D
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.utils.viewport.FitViewport
import com.etheria.game.EtheriaGame
import com.etheria.game.GameSettings
import com.etheria.game.GraphicsQuality
import com.etheria.game.combat.CombatEngine
import com.etheria.game.enemy.Enemy
import com.etheria.game.enemy.EnemyManager
import com.etheria.game.enemy.EnemyType
import com.etheria.game.enemy.Goblin
import com.etheria.game.inventory.InventoryManager
import com.etheria.game.player.Element
import com.etheria.game.player.PartyManager
import com.etheria.game.quest.ObjectiveType
import com.etheria.game.quest.QuestManager
import com.etheria.game.ui.HUDRenderer
import com.etheria.game.ui.MenuManager
import com.etheria.game.ui.MenuType
import com.etheria.game.ui.MiniMapRenderer
import com.etheria.game.ui.VirtualGamepad
import com.etheria.game.utils.SpriteSheetAnimator
import com.etheria.game.world.TileType
import com.etheria.game.world.WeatherType
import com.etheria.game.world.WorldManager

class GameScreen(private val game: EtheriaGame, private val loadSave: Boolean) : Screen {

    private val camera = OrthographicCamera()
    private val viewport = FitViewport(EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT, camera)
    private val uiCamera = OrthographicCamera()
    private val uiViewport = FitViewport(EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT, uiCamera)

    // Box2D Physics World (zero top-down gravity)
    private val b2dWorld: World
    private val playerBody: Body

    // Core systems
    private val party = PartyManager()
    private val enemyManager = EnemyManager()
    private val combatEngine = CombatEngine()
    private val world = WorldManager()
    private val inventory = InventoryManager()
    private val quests = QuestManager()

    // UI
    private val hud = HUDRenderer(game.batch, game.shapeRenderer)
    private val gamepad = VirtualGamepad()
    private val menuManager = MenuManager(game.batch, game.shapeRenderer)
    private val miniMap = MiniMapRenderer(game.batch, game.shapeRenderer)
    private val font = EtheriaGame.createSmoothFont(0.95f)
    private val uiTouchVec = Vector2()

    // Sprite animators & tileset
    private val heroAnimators = mutableMapOf<String, SpriteSheetAnimator>()
    private val enemyAnimators = mutableMapOf<String, SpriteSheetAnimator>()
    private var tileGrid: Array<Array<TextureRegion>>? = null
    private var particleTex: Texture? = null

    // State
    private var worldTime = 0f
    private var autoSaveTimer = 0f
    private val autoSaveInterval = 45f
    private var dialogueActive = false
    private var dialogueNPC: com.etheria.game.world.NPC? = null
    private var dialogueText = ""
    private var questNotifText = ""
    private var questNotifTimer = 0f
    private var interactionCooldown = 0f
    private var menuToggleCooldown = 0f
    private var statusHint = ""
    private var bossMusicActive = false

    private val particles = mutableListOf<Particle>()
    private val weatherDrops = mutableListOf<Vector2>()

    data class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        var life: Float, var maxLife: Float, var r: Float, var g: Float,
        var b: Float, var size: Float
    )

    private data class TopMenuBtn(val label: String, val type: MenuType, val x: Float)
    private val topButtons = listOf(
        TopMenuBtn("INV", MenuType.INVENTORY, EtheriaGame.VIRTUAL_WIDTH * 0.5f - 155f),
        TopMenuBtn("MAP", MenuType.WORLD_MAP, EtheriaGame.VIRTUAL_WIDTH * 0.5f - 93f),
        TopMenuBtn("QST", MenuType.QUEST_JOURNAL, EtheriaGame.VIRTUAL_WIDTH * 0.5f - 31f),
        TopMenuBtn("CHR", MenuType.CHARACTER, EtheriaGame.VIRTUAL_WIDTH * 0.5f + 31f),
        TopMenuBtn("SET", MenuType.SETTINGS, EtheriaGame.VIRTUAL_WIDTH * 0.5f + 93f)
    )

    init {
        Box2D.init()
        b2dWorld = World(Vector2(0f, 0f), true)
        uiCamera.setToOrtho(false, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT)

        loadVisuals()

        game.audioManager.loadSounds()
        game.audioManager.playMusic("sounds/music/exploration")

        inventory.getStarterItems()
        quests.startQuest("main_01")
        enemyManager.spawnInitialEnemies()

        val startPos = if (loadSave) {
            game.saveManager.load(party, inventory, quests, world)
        } else {
            Vector2(384f, 496f)
        }
        party.activeCharacter.position.set(startPos)
        camera.position.set(startPos.x, startPos.y, 0f)
        camera.update()

        // Create Box2D kinematic/dynamic player body
        val bd = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(startPos.x / EtheriaGame.PPM, startPos.y / EtheriaGame.PPM)
            fixedRotation = true
        }
        playerBody = b2dWorld.createBody(bd)
        val circle = CircleShape().apply { radius = 12f / EtheriaGame.PPM }
        playerBody.createFixture(FixtureDef().apply {
            shape = circle
            density = 1f
        })
        circle.dispose()

        // Pre-seed weather particles
        repeat(60) {
            weatherDrops.add(Vector2(MathUtils.random(0f, EtheriaGame.VIRTUAL_WIDTH), MathUtils.random(0f, EtheriaGame.VIRTUAL_HEIGHT)))
        }
    }

    private fun loadVisuals() {
        val am = game.assetManager
        val heroRows = listOf("idle", "walk", "run", "sprint", "jump", "dash", "attack", "heavy", "skill", "ultimate", "hurt", "death")
        listOf(
            "sprites/player/kai.png",
            "sprites/player/lyra.png",
            "sprites/player/zara.png",
            "sprites/player/ragna.png"
        ).forEach { path ->
            if (am.isLoaded(path)) {
                heroAnimators[path] = SpriteSheetAnimator(am.get(path, Texture::class.java), 48, 48, heroRows, 0.12f)
            }
        }

        val enemyRows = listOf("idle", "move", "attack", "hurt", "death")
        listOf(
            "sprites/enemies/slime.png" to 48,
            "sprites/enemies/goblin.png" to 48,
            "sprites/enemies/skeleton.png" to 48,
            "sprites/enemies/archer_enemy.png" to 48,
            "sprites/enemies/flying_monster.png" to 48,
            "sprites/enemies/darkKnight.png" to 56,
            "sprites/enemies/elite_golem.png" to 56,
            "sprites/enemies/shadow_lord.png" to 72
        ).forEach { (path, size) ->
            if (am.isLoaded(path)) {
                enemyAnimators[path] = SpriteSheetAnimator(am.get(path, Texture::class.java), size, size, enemyRows, 0.16f)
            }
        }

        if (am.isLoaded("sprites/world/tileset.png")) {
            tileGrid = TextureRegion.split(am.get("sprites/world/tileset.png", Texture::class.java), 32, 32)
        }
        if (am.isLoaded("sprites/effects/particles.png")) {
            particleTex = am.get("sprites/effects/particles.png", Texture::class.java)
        }
    }

    override fun show() {}

    override fun render(delta: Float) {
        val cappedDelta = minOf(delta, 0.05f)
        update(cappedDelta)
        draw(cappedDelta)
    }

    private fun update(delta: Float) {
        worldTime += delta
        if (interactionCooldown > 0f) interactionCooldown -= delta
        if (menuToggleCooldown > 0f) menuToggleCooldown -= delta
        if (questNotifTimer > 0f) questNotifTimer -= delta

        handleTopMenuAndShortcuts()

        if (menuManager.isOpen()) {
            menuManager.handleInput(uiViewport, party, inventory, quests, world, game.audioManager, game.saveManager)
            return
        }

        gamepad.update(uiViewport)

        if (dialogueActive) {
            if (interactionCooldown <= 0f && (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.E))) {
                val npc = dialogueNPC
                if (npc != null && npc.dialogueIndex < npc.dialogue.size) {
                    dialogueText = npc.dialogue[npc.dialogueIndex]
                    npc.dialogueIndex++
                } else {
                    dialogueActive = false
                    dialogueNPC?.dialogueIndex = 0
                }
                interactionCooldown = 0.25f
            }
            return
        }

        handleMovement(delta)
        handleActions()
        party.update(delta)

        b2dWorld.step(delta, 4, 2)

        val playerPos = party.activeCharacter.position
        enemyManager.update(delta, playerPos)

        // Process pending enemy attacks (melee & projectiles)
        enemyManager.enemies.forEach { enemy ->
            if (enemy.pendingAttacks.isNotEmpty()) {
                enemy.pendingAttacks.forEach { evt ->
                    if (evt.isProjectile) {
                        combatEngine.spawnEnemyProjectile(evt.origin, evt.direction, evt.damage, evt.element, evt.isRadialBurst)
                    } else {
                        val dist = Vector2.dst(evt.origin.x, evt.origin.y, playerPos.x, playerPos.y)
                        if (dist <= evt.range && party.activeCharacter.invincibleTimer <= 0f) {
                            onPlayerHit(evt.damage, evt.element)
                        }
                    }
                }
                enemy.pendingAttacks.clear()
            }
        }

        combatEngine.update(
            delta = delta,
            attacker = party.activeCharacter,
            enemies = enemyManager.enemies,
            onEnemyKilled = { trackKill(it) },
            onPlayerHit = { dmg, el -> onPlayerHit(dmg, el) }
        )

        handleInteractions()
        checkZoneTriggers(playerPos)
        updateContextualInteractHint(playerPos)
        hud.updateShake(delta)
        world.update(delta, playerPos)

        // Boss music switch
        val boss = enemyManager.enemies.firstOrNull { it.isBoss && !it.isDead }
        val nearBoss = boss != null && Vector2.dst(playerPos.x, playerPos.y, boss.position.x, boss.position.y) < 520f
        if (nearBoss && !bossMusicActive) {
            bossMusicActive = true
            game.audioManager.playMusic("sounds/music/boss_battle")
        } else if (!nearBoss && bossMusicActive) {
            bossMusicActive = false
            game.audioManager.playMusic("sounds/music/exploration")
        }

        // Auto-save
        autoSaveTimer += delta
        if (autoSaveTimer >= autoSaveInterval) {
            autoSaveTimer = 0f
            game.saveManager.save(party, inventory, quests, world, playerPos, isAuto = true)
        }

        // Smooth camera follow
        val smoothing = 6f
        camera.position.x += (playerPos.x - camera.position.x) * smoothing * delta + hud.shakeOffset.x
        camera.position.y += (playerPos.y - camera.position.y) * smoothing * delta + hud.shakeOffset.y
        camera.position.x = camera.position.x.coerceIn(EtheriaGame.VIRTUAL_WIDTH / 2f, 3200f - EtheriaGame.VIRTUAL_WIDTH / 2f)
        camera.position.y = camera.position.y.coerceIn(EtheriaGame.VIRTUAL_HEIGHT / 2f, 1920f - EtheriaGame.VIRTUAL_HEIGHT / 2f)
        camera.update()

        // Particles
        particles.removeAll { it.life <= 0f }
        particles.forEach {
            it.x += it.vx * delta
            it.y += it.vy * delta
            it.vy -= 140f * delta
            it.life -= delta
        }

        // Weather drops
        if (GameSettings.graphicsQuality != GraphicsQuality.LOW && world.weatherType != WeatherType.CLEAR) {
            weatherDrops.forEach { drop ->
                drop.y -= (if (world.weatherType == WeatherType.STORM) 520f else 360f) * delta
                drop.x -= 95f * delta
                if (drop.y < 0f) {
                    drop.y = EtheriaGame.VIRTUAL_HEIGHT
                    drop.x = MathUtils.random(0f, EtheriaGame.VIRTUAL_WIDTH)
                }
                if (drop.x < 0f) drop.x = EtheriaGame.VIRTUAL_WIDTH
            }
        }
    }

    private fun handleTopMenuAndShortcuts() {
        if (menuToggleCooldown <= 0f && Gdx.input.justTouched()) {
            uiTouchVec.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            uiViewport.unproject(uiTouchVec)
            val btnY = EtheriaGame.VIRTUAL_HEIGHT - 36f
            topButtons.forEach { btn ->
                if (uiTouchVec.x in btn.x..(btn.x + 56f) && uiTouchVec.y in btnY..(btnY + 26f)) {
                    game.audioManager.playSound("ui_click")
                    menuManager.toggle(btn.type)
                    menuToggleCooldown = 0.25f
                    return
                }
            }
        }

        if (menuToggleCooldown <= 0f) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.I)) { menuManager.toggle(MenuType.INVENTORY); menuToggleCooldown = 0.25f }
            if (Gdx.input.isKeyJustPressed(Input.Keys.M)) { menuManager.toggle(MenuType.WORLD_MAP); menuToggleCooldown = 0.25f }
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) { menuManager.toggle(MenuType.QUEST_JOURNAL); menuToggleCooldown = 0.25f }
            if (Gdx.input.isKeyJustPressed(Input.Keys.P)) { menuManager.toggle(MenuType.CHARACTER); menuToggleCooldown = 0.25f }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (menuManager.isOpen()) menuManager.close() else menuManager.toggle(MenuType.SETTINGS)
                menuToggleCooldown = 0.25f
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
                game.saveManager.save(party, inventory, quests, world, party.activeCharacter.position, isAuto = false)
                showNotification("Game Saved!", 2f)
                menuToggleCooldown = 0.25f
            }
        }
    }

    private fun handleMovement(delta: Float) {
        val ch = party.activeCharacter
        if (ch.stats.isDead) return

        var mx = gamepad.joystickDir.x
        var my = gamepad.joystickDir.y
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) mx -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) mx += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) my += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) my -= 1f

        val len = kotlin.math.hypot(mx.toDouble(), my.toDouble()).toFloat()

        // Terrain states: Swimming & Climbing
        ch.isSwimming = !ch.isJumping && world.isWaterAt(ch.position.x, ch.position.y)
        ch.isClimbing = world.isCliffAt(ch.position.x, ch.position.y)
        ch.isSprinting = len > 0.88f && ch.stats.stamina > 8f && !ch.isSwimming && !ch.isClimbing

        if (ch.isDashing) {
            val nx = ch.position.x + ch.velocity.x * delta
            val ny = ch.position.y + ch.velocity.y * delta
            tryMoveTo(ch, nx, ny)
            return
        }

        if (ch.isAttacking) return

        if (len > 0.12f) {
            val dirX = mx / len
            val dirY = my / len
            ch.aimDir.set(dirX, dirY)
            if (kotlin.math.abs(dirX) > 0.08f) ch.facingRight = dirX >= 0f

            var moveSpeed = ch.stats.speed
            if (ch.isSprinting) moveSpeed *= 1.38f
            if (ch.isSwimming) moveSpeed *= 0.62f
            if (ch.isClimbing) moveSpeed *= 0.55f

            val step = moveSpeed * minOf(1f, len) * delta
            tryMoveTo(ch, ch.position.x + dirX * step, ch.position.y + dirY * step)

            ch.currentAnim = when {
                ch.isJumping -> "jump"
                ch.isSprinting -> "sprint"
                len > 0.55f -> "run"
                else -> "walk"
            }

            if (MathUtils.random() < 0.03f && !ch.isJumping) {
                game.audioManager.playSound("footstep", 0.3f)
            }
        } else {
            ch.isSprinting = false
            ch.currentAnim = if (ch.isJumping) "jump" else "idle"
        }
    }

    private fun tryMoveTo(ch: com.etheria.game.player.PlayableCharacter, targetX: Float, targetY: Float) {
        val cx = targetX.coerceIn(48f, 3152f)
        val cy = targetY.coerceIn(48f, 1872f)
        val canPassX = ch.isJumping || world.isPassable(cx, ch.position.y)
        if (canPassX) ch.position.x = cx
        val canPassY = ch.isJumping || world.isPassable(ch.position.x, cy)
        if (canPassY) ch.position.y = cy
        playerBody.setTransform(ch.position.x / EtheriaGame.PPM, ch.position.y / EtheriaGame.PPM, 0f)
    }

    private fun handleActions() {
        val ch = party.activeCharacter
        if (ch.stats.isDead) return
        val enemies = enemyManager.getAliveEnemies()

        if (gamepad.jumpBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (ch.jump()) game.audioManager.playSound("jump", 0.7f)
        }

        if (gamepad.dashBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)) {
            if (ch.dash(gamepad.joystickDir.x, gamepad.joystickDir.y)) {
                game.audioManager.playSound("dash", 0.7f)
                spawnHitParticles(ch.position.x, ch.position.y + 8f, ch.element, 8, 28f)
            }
        }

        if (gamepad.attackBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            val res = ch.normalAttack()
            val hits = combatEngine.processAttack(res, ch, enemies)
            val sfx = when (ch.characterClass) {
                com.etheria.game.player.CharacterClass.ARCHER -> "attack_bow"
                com.etheria.game.player.CharacterClass.MAGE -> "attack_magic"
                com.etheria.game.player.CharacterClass.HEAVY_FIGHTER -> "attack_heavy"
                else -> "attack_sword"
            }
            game.audioManager.playSound(sfx, 0.75f)
            spawnHitParticles(ch.position.x + ch.aimDir.x * 26f, ch.position.y + 20f, res.element, 8, 36f)
            if (hits.isNotEmpty()) {
                game.audioManager.playSound("enemy_hit", 0.7f)
                hud.triggerShake(2.5f, 0.1f)
                ch.stats.elementalEnergy = minOf(ch.stats.maxElementalEnergy, ch.stats.elementalEnergy + 12f)
                hits.filter { it.isDead }.forEach { trackKill(it) }
            }
        }

        if (gamepad.heavyBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            val res = ch.heavyAttack()
            if (res.damage > 0f) {
                val hits = combatEngine.processAttack(res, ch, enemies)
                game.audioManager.playSound("attack_heavy", 0.85f)
                spawnHitParticles(ch.position.x + ch.aimDir.x * 32f, ch.position.y + 20f, res.element, 16, 60f)
                hud.triggerShake(4.5f, 0.16f)
                if (hits.isNotEmpty()) {
                    ch.stats.elementalEnergy = minOf(ch.stats.maxElementalEnergy, ch.stats.elementalEnergy + 20f)
                    hits.filter { it.isDead }.forEach { trackKill(it) }
                }
            }
        }

        if (gamepad.skillBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            val res = ch.useSkill()
            if (res.damage > 0f) {
                val hits = combatEngine.processAttack(res, ch, enemies)
                game.audioManager.playSound("skill", 0.85f)
                spawnHitParticles(ch.position.x, ch.position.y + 20f, res.element, 26, 120f)
                hud.triggerShake(5f, 0.22f)
                ch.stats.elementalEnergy = minOf(ch.stats.maxElementalEnergy, ch.stats.elementalEnergy + 26f)
                hits.filter { it.isDead }.forEach { trackKill(it) }
                activateNearbyPuzzlePillar(ch.position, res.element)
            }
        }

        if (gamepad.ultimateBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.V)) {
            val res = ch.useUltimate()
            if (res.damage > 0f) {
                val hits = combatEngine.processAttack(res, ch, enemies)
                game.audioManager.playSound("ultimate", 0.95f)
                spawnHitParticles(ch.position.x, ch.position.y + 24f, res.element, 52, 200f)
                hud.triggerShake(9f, 0.4f)
                hits.filter { it.isDead }.forEach { trackKill(it) }
                activateNearbyPuzzlePillar(ch.position, res.element)
            }
        }

        gamepad.partyBtns.forEachIndexed { i, btn ->
            if (btn.justPressed && party.switchTo(i)) {
                game.audioManager.playSound("ui_click")
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) party.switchTo(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) party.switchTo(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) party.switchTo(2)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) party.switchTo(3)
    }

    private fun activateNearbyPuzzlePillar(pos: Vector2, element: Element) {
        world.puzzlePillars.filter { !it.activated }.forEach { pillar ->
            if (Vector2.dst(pos.x, pos.y, pillar.x, pillar.y) < 150f) {
                pillar.activated = true
                game.audioManager.playSound("puzzle_solve")
                quests.startQuest("temple_01")
                val done = quests.progressAllActive("temple_puzzle", ObjectiveType.PUZZLE, 1, inventory, party)
                showNotification("${pillar.name} Activated!", 2.5f)
                if (world.puzzlePillars.all { it.activated } && !world.puzzleSolved) {
                    world.puzzleSolved = true
                    inventory.addGold(300)
                    showNotification("Ancient Temple Seal Unlocked! (+300 Gold)", 3.5f)
                }
                done.forEach { showNotification("Quest Complete: ${it.title}!", 3.5f) }
            }
        }
    }

    private fun updateContextualInteractHint(playerPos: Vector2) {
        val npc = world.npcs.firstOrNull { Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 76f }
        val chest = world.chests.firstOrNull { !it.opened && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 68f }
        val item = world.collectibles.firstOrNull { !it.collected && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 62f }
        val pillar = world.puzzlePillars.firstOrNull { !it.activated && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 74f }
        val nearFountain = Vector2.dst(playerPos.x, playerPos.y, world.healingFountainPos.x, world.healingFountainPos.y) < 68f
        val nearPortal = Vector2.dst(playerPos.x, playerPos.y, world.dungeonPortalPos.x, world.dungeonPortalPos.y) < 74f

        when {
            npc != null -> { gamepad.interactVisible = true; gamepad.interactLabel = "TALK"; statusHint = "Near ${npc.name}" }
            chest != null -> { gamepad.interactVisible = true; gamepad.interactLabel = "OPEN"; statusHint = "Treasure Chest nearby" }
            item != null -> { gamepad.interactVisible = true; gamepad.interactLabel = "PICK"; statusHint = "${item.name} nearby" }
            pillar != null -> { gamepad.interactVisible = true; gamepad.interactLabel = "RUNE"; statusHint = "Activate ${pillar.name}" }
            nearFountain -> { gamepad.interactVisible = true; gamepad.interactLabel = "HEAL"; statusHint = "Healing Fountain" }
            nearPortal -> { gamepad.interactVisible = true; gamepad.interactLabel = "GATE"; statusHint = "Ancient Dungeon Gate" }
            party.activeCharacter.isSwimming -> { gamepad.interactVisible = false; statusHint = "Swimming (Stamina draining)" }
            party.activeCharacter.isClimbing -> { gamepad.interactVisible = false; statusHint = "Climbing Cliff" }
            else -> { gamepad.interactVisible = false; statusHint = "" }
        }
    }

    private fun handleInteractions() {
        val trigger = gamepad.interactBtn.justPressed || Gdx.input.isKeyJustPressed(Input.Keys.E)
        val playerPos = party.activeCharacter.position

        // Unlock waypoints automatically on approach
        world.waypoints.filter { !it.unlocked }.forEach { wp ->
            if (Vector2.dst(playerPos.x, playerPos.y, wp.x, wp.y) < 84f) {
                wp.unlocked = true
                game.audioManager.playSound("level_up", 0.7f)
                showNotification("Waypoint Unlocked: ${wp.name}", 3f)
            }
        }

        if (!trigger || interactionCooldown > 0f) return

        world.npcs.firstOrNull { Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 76f }?.let { npc ->
            dialogueActive = true
            dialogueNPC = npc
            dialogueText = npc.dialogue[0]
            npc.dialogueIndex = 1
            if (npc.hasQuest && npc.questId.isNotEmpty()) {
                if (quests.startQuest(npc.questId)) {
                    showNotification("New Quest Started!", 2.5f)
                }
            }
            interactionCooldown = 0.35f
            return
        }

        world.chests.firstOrNull { !it.opened && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 68f }?.let { chest ->
            chest.opened = true
            chest.items.forEach { inventory.addItem(it) }
            inventory.addGold(120)
            game.audioManager.playSound("chest_open")
            showNotification("Opened Chest: ${chest.items.joinToString { it.name }} + 120G!", 3.5f)
            interactionCooldown = 0.35f
            return
        }

        world.collectibles.firstOrNull { !it.collected && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 62f }?.let { col ->
            col.collected = true
            inventory.addItem(com.etheria.game.world.Item(col.name, com.etheria.game.world.ItemType.MATERIAL, quantity = 1))
            val completed = quests.progressAllActive(col.targetId, ObjectiveType.COLLECT, 1, inventory, party)
            game.audioManager.playSound("ui_click")
            showNotification("Collected ${col.name}!", 2.2f)
            completed.forEach { showNotification("Quest Complete: ${it.title}!", 3.5f) }
            interactionCooldown = 0.35f
            return
        }

        world.puzzlePillars.firstOrNull { !it.activated && Vector2.dst(playerPos.x, playerPos.y, it.x, it.y) < 74f }?.let {
            activateNearbyPuzzlePillar(playerPos, party.activeCharacter.element)
            interactionCooldown = 0.35f
            return
        }

        if (Vector2.dst(playerPos.x, playerPos.y, world.healingFountainPos.x, world.healingFountainPos.y) < 68f) {
            party.characters.forEach { if (it.stats.isDead) it.revive(1f) }
            party.healAll(1f)
            game.audioManager.playSound("level_up", 0.7f)
            showNotification("Party Fully Restored at Fountain!", 2.5f)
            interactionCooldown = 0.35f
            return
        }

        if (Vector2.dst(playerPos.x, playerPos.y, world.dungeonPortalPos.x, world.dungeonPortalPos.y) < 74f) {
            if (!world.puzzleSolved) {
                showNotification("Activate the 3 Temple Rune Pillars first!", 3f)
            } else {
                party.activeCharacter.position.set(2600f, 520f)
                quests.startQuest("boss_01")
                showNotification("Entered Shadow Lord's Domain!", 3f)
            }
            interactionCooldown = 0.35f
        }
    }

    private fun checkZoneTriggers(pos: Vector2) {
        if (pos.x > 1540f) {
            val done = quests.progressAllActive("crystal_river", ObjectiveType.REACH, 1, inventory, party)
            done.forEach { showNotification("Quest Complete: ${it.title}!", 3.5f) }
        }
        if (pos.x > 2560f) {
            quests.startQuest("boss_01")
        }
    }

    private fun onPlayerHit(damage: Float, element: Element) {
        val ch = party.activeCharacter
        val actual = ch.stats.takeDamage(damage, ch.armorBonusDefense)
        ch.invincibleTimer = 0.65f
        ch.hurtTimer = 0.25f
        combatEngine.damageNumbers.add(
            com.etheria.game.combat.DamageNumber(actual, ch.position.x, ch.position.y + 48f, element = element, label = "-${actual.toInt()}")
        )
        hud.triggerShake(5f, 0.2f)
        game.audioManager.playSound("player_hurt", 0.85f)

        if (ch.stats.isDead) {
            if (!party.switchToFirstAlive()) {
                // Full party defeat -> respawn at Village Fountain
                party.characters.forEach { it.revive(0.75f) }
                party.activeCharacter.position.set(384f, 496f)
                showNotification("Party revived at Aethelgard Village!", 3.5f)
            } else {
                showNotification("${ch.name} fell! Switched to ${party.activeCharacter.name}", 2.5f)
            }
        }
    }

    private fun trackKill(enemy: Enemy) {
        game.audioManager.playSound("enemy_death", 0.7f)
        val leveled = party.activeCharacter.stats.addExp(enemy.stats.expReward)
        inventory.addGold(25 * (1 + enemy.type.ordinal))
        if (leveled) {
            game.audioManager.playSound("level_up", 0.9f)
            showNotification("${party.activeCharacter.name} reached Level ${party.activeCharacter.stats.level}!", 3f)
        }

        val targetId = when (enemy.type) {
            EnemyType.SLIME -> "slime"
            EnemyType.GOBLIN -> if ((enemy as? Goblin)?.isLeader == true) "goblin_leader" else "goblin"
            EnemyType.SKELETON -> "skeleton"
            EnemyType.DARK_KNIGHT -> "dark_knight"
            EnemyType.BOSS -> "shadow_lord"
            else -> return
        }
        val completed = quests.progressAllActive(targetId, ObjectiveType.KILL, 1, inventory, party)
        completed.forEach { showNotification("Quest Complete: ${it.title}!", 3.5f) }
    }

    private fun showNotification(text: String, duration: Float = 3f) {
        questNotifText = text
        questNotifTimer = duration
    }

    private fun spawnHitParticles(x: Float, y: Float, element: Element, count: Int = 10, spread: Float = 50f) {
        if (GameSettings.graphicsQuality == GraphicsQuality.LOW) return
        val mult = if (GameSettings.graphicsQuality == GraphicsQuality.MEDIUM) 0.5f else 1f
        val n = maxOf(2, (count * mult).toInt())
        val c = hud.getElementColor(element)
        repeat(n) {
            val angle = MathUtils.random(0f, 360f)
            val speed = MathUtils.random(50f, 190f)
            particles.add(
                Particle(
                    x = x + MathUtils.random(-spread * 0.4f, spread * 0.4f),
                    y = y + MathUtils.random(-10f, 14f),
                    vx = MathUtils.cosDeg(angle) * speed,
                    vy = MathUtils.sinDeg(angle) * speed + 45f,
                    life = MathUtils.random(0.25f, 0.65f),
                    maxLife = 0.65f,
                    r = c.r, g = c.g, b = c.b,
                    size = MathUtils.random(3f, 7f)
                )
            )
        }
    }

    private fun draw(delta: Float) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.12f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        // ── WORLD SPACE ───────────────────────────────────────────────────────
        game.batch.projectionMatrix = camera.combined
        game.shapeRenderer.projectionMatrix = camera.combined

        drawWorldTiles()
        drawShadows()
        drawWorldObjectSprites()
        drawEntities()
        drawProjectilesAndEffects()
        hud.renderWorldDamageNumbers(combatEngine.damageNumbers)

        // ── SCREEN SPACE UI ───────────────────────────────────────────────────
        game.batch.projectionMatrix = uiCamera.combined
        game.shapeRenderer.projectionMatrix = uiCamera.combined

        drawWeatherOverlay()
        hud.renderHUD(party, quests, statusHint)
        miniMap.render(world, party, enemyManager.enemies)

        val boss = enemyManager.enemies.firstOrNull { it.isBoss && !it.isDead }
        if (boss != null && Vector2.dst(party.activeCharacter.position.x, party.activeCharacter.position.y, boss.position.x, boss.position.y) < 580f) {
            hud.renderBossBar(boss)
        }

        if (!menuManager.isOpen() && !dialogueActive) {
            gamepad.render(game.batch, game.shapeRenderer, party)
            renderSkillCooldowns()
        }

        renderTopMenuButtons()

        if (dialogueActive) renderDialogue()
        if (questNotifTimer > 0f) renderQuestNotification()
        if (menuManager.isOpen()) menuManager.render(party, inventory, quests, world)
    }

    private fun drawWorldTiles() {
        val playerPos = party.activeCharacter.position
        val activeChunks = world.getActiveChunks(playerPos)
        val ambient = world.getAmbientColor()
        val grid = tileGrid ?: return

        game.batch.begin()
        game.batch.color = ambient
        activeChunks.forEach { chunk ->
            val bx = chunk.worldX().toFloat()
            val by = chunk.worldY().toFloat()
            for (tx in 0 until 16) {
                for (ty in 0 until 16) {
                    val tile = chunk.tiles[tx][ty]
                    val idx = tile.type.tilesetIndex
                    val region = grid[idx / 8][idx % 8]
                    val px = bx + tx * 32f
                    val py = by + ty * 32f
                    game.batch.draw(region, px, py, 32f, 32f)
                }
            }
        }
        game.batch.color = Color.WHITE
        game.batch.end()

        // Water shimmer & grass sway accents on HIGH/MEDIUM
        if (GameSettings.graphicsQuality != GraphicsQuality.LOW) {
            game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            activeChunks.forEach { chunk ->
                val bx = chunk.worldX().toFloat()
                val by = chunk.worldY().toFloat()
                for (tx in 0 until 16 step 2) {
                    for (ty in 0 until 16 step 2) {
                        val t = chunk.tiles[tx][ty].type
                        val px = bx + tx * 32f
                        val py = by + ty * 32f
                        if (t == TileType.WATER || t == TileType.WATERFALL) {
                            val shift = MathUtils.sin(worldTime * 3f + tx + ty) * 6f
                            game.shapeRenderer.setColor(0.7f, 0.9f, 1f, 0.32f)
                            game.shapeRenderer.rect(px + 6f + shift, py + 14f, 14f, 2f)
                        } else if (t == TileType.GRASS && ((tx + ty) % 4 == 0)) {
                            val sway = MathUtils.sin(worldTime * 2.4f + tx) * 3f
                            game.shapeRenderer.setColor(0.45f, 0.85f, 0.45f, 0.45f)
                            game.shapeRenderer.rectLine(px + 10f, py + 6f, px + 10f + sway, py + 14f, 1.5f)
                        }
                    }
                }
            }
            game.shapeRenderer.end()
        }
    }

    private fun drawShadows() {
        if (GameSettings.graphicsQuality == GraphicsQuality.LOW) return
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0f, 0f, 0f, 0.34f)

        val ch = party.activeCharacter
        val shadowScale = (1f - ch.jumpZ / 160f).coerceIn(0.45f, 1f)
        game.shapeRenderer.ellipse(ch.position.x - 14f * shadowScale, ch.position.y - 5f, 28f * shadowScale, 10f * shadowScale, 12)

        enemyManager.enemies.filter { !it.isDead }.forEach { en ->
            val w = en.spriteSize * 0.55f
            game.shapeRenderer.ellipse(en.position.x - w / 2f, en.position.y - 4f, w, 9f, 10)
        }
        world.npcs.forEach { npc ->
            game.shapeRenderer.ellipse(npc.x - 12f, npc.y - 4f, 24f, 8f, 10)
        }
        game.shapeRenderer.end()
    }

    private fun drawWorldObjectSprites() {
        val grid = tileGrid ?: return
        val chestClosed = grid[TileType.CHEST.tilesetIndex / 8][TileType.CHEST.tilesetIndex % 8]
        val chestOpen = grid[TileType.CHEST_OPEN.tilesetIndex / 8][TileType.CHEST_OPEN.tilesetIndex % 8]
        val wpRegion = grid[TileType.WAYPOINT.tilesetIndex / 8][TileType.WAYPOINT.tilesetIndex % 8]
        val herbRegion = grid[TileType.HERB.tilesetIndex / 8][TileType.HERB.tilesetIndex % 8]
        val dungeonRegion = grid[TileType.DUNGEON_ENTRANCE.tilesetIndex / 8][TileType.DUNGEON_ENTRANCE.tilesetIndex % 8]

        // Glowing pillars & fountain via ShapeRenderer
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        // Healing fountain
        game.shapeRenderer.setColor(0.25f, 0.78f, 1f, 0.75f)
        game.shapeRenderer.circle(world.healingFountainPos.x, world.healingFountainPos.y, 20f, 16)
        game.shapeRenderer.setColor(0.75f, 0.95f, 1f, 0.9f)
        game.shapeRenderer.circle(world.healingFountainPos.x, world.healingFountainPos.y, 10f + MathUtils.sin(worldTime * 3f) * 2f, 12)

        // Elemental puzzle pillars
        world.puzzlePillars.forEach { p ->
            val c = hud.getElementColor(p.element)
            game.shapeRenderer.setColor(if (p.activated) c else Color(0.32f, 0.32f, 0.38f, 0.9f))
            game.shapeRenderer.rect(p.x - 10f, p.y, 20f, 34f)
            if (p.activated) {
                game.shapeRenderer.setColor(c.r, c.g, c.b, 0.35f)
                game.shapeRenderer.circle(p.x, p.y + 34f, 18f, 14)
            }
        }
        game.shapeRenderer.end()

        game.batch.begin()
        world.chests.forEach { ch ->
            game.batch.draw(if (ch.opened) chestOpen else chestClosed, ch.x - 16f, ch.y, 32f, 32f)
        }
        world.waypoints.forEach { wp ->
            game.batch.color = if (wp.unlocked) Color.WHITE else Color(0.55f, 0.55f, 0.6f, 0.85f)
            game.batch.draw(wpRegion, wp.x - 16f, wp.y, 32f, 32f)
        }
        game.batch.color = Color.WHITE
        world.collectibles.filter { !it.collected }.forEach { col ->
            game.batch.draw(herbRegion, col.x - 14f, col.y, 28f, 28f)
        }
        game.batch.draw(dungeonRegion, world.dungeonPortalPos.x - 20f, world.dungeonPortalPos.y - 12f, 40f, 40f)

        // NPC sprites (reusing mage/hero sheet idle frame tinted)
        val npcAnim = heroAnimators["sprites/player/zara.png"]
        world.npcs.forEach { npc ->
            if (npcAnim != null) {
                val frame = npcAnim.getFrame("idle", worldTime)
                game.batch.color = Color(0.85f, 0.92f, 0.78f, 1f)
                game.batch.draw(frame, npc.x - 24f, npc.y, 48f, 48f)
                game.batch.color = Color.WHITE
            }
            font.color = Color(1f, 0.9f, 0.45f, 1f)
            font.draw(game.batch, npc.name, npc.x - 36f, npc.y + 58f)
        }

        font.color = Color(0.65f, 0.95f, 1f, 1f)
        font.draw(game.batch, "Fountain", world.healingFountainPos.x - 26f, world.healingFountainPos.y + 36f)
        font.draw(game.batch, "Dungeon Gate", world.dungeonPortalPos.x - 42f, world.dungeonPortalPos.y + 42f)
        world.puzzlePillars.forEach { p ->
            font.color = if (p.activated) Color.GREEN else Color.LIGHT_GRAY
            font.draw(game.batch, p.name, p.x - 28f, p.y + 52f)
        }
        game.batch.end()
    }

    private fun drawEntities() {
        game.batch.begin()

        // Draw Enemies
        enemyManager.enemies.forEach { en ->
            val animator = enemyAnimators[en.sheetPath]
            if (animator != null) {
                val frame = animator.getFrame(en.currentAnim, en.animTimer, !en.isDead)
                val sz = en.spriteSize.toFloat()
                val scaleX = if (en.facingRight) 1f else -1f
                game.batch.draw(frame, en.position.x - sz / 2f, en.position.y, sz / 2f, sz / 2f, sz, sz, scaleX, 1f, 0f)
            }
            if (!en.isDead) {
                font.color = if (en.isBoss) Color(1f, 0.45f, 0.85f, 1f) else Color(1f, 0.72f, 0.72f, 0.95f)
                font.draw(game.batch, en.displayName, en.position.x - 38f, en.position.y + en.spriteSize + 18f)
            }
        }

        // Draw Active Player
        val ch = party.activeCharacter
        val heroAnim = heroAnimators[ch.sheetPath]
        if (heroAnim != null) {
            val frame = heroAnim.getFrame(ch.currentAnim, ch.animTimer, !ch.stats.isDead)
            val scaleX = if (ch.facingRight) 1f else -1f
            val yDraw = ch.position.y + ch.jumpZ - (if (ch.isSwimming) 8f else 0f)
            game.batch.draw(frame, ch.position.x - 24f, yDraw, 24f, 24f, 48f, 48f, scaleX, 1f, 0f)
        }

        game.batch.end()

        // Enemy HP bars
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        enemyManager.enemies.filter { !it.isDead }.forEach { en ->
            val bw = if (en.isBoss) 72f else 38f
            val by = en.position.y + en.spriteSize + 2f
            game.shapeRenderer.setColor(0.1f, 0.1f, 0.12f, 0.85f)
            game.shapeRenderer.rect(en.position.x - bw / 2f, by, bw, 5f)
            game.shapeRenderer.setColor(0.95f, 0.22f, 0.24f, 0.95f)
            game.shapeRenderer.rect(en.position.x - bw / 2f, by, bw * en.getHpPercent(), 5f)
        }
        game.shapeRenderer.end()
    }

    private fun drawProjectilesAndEffects() {
        // Projectiles & particles
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        combatEngine.projectiles.forEach { p ->
            val c = hud.getElementColor(p.element)
            game.shapeRenderer.setColor(c.r, c.g, c.b, 0.95f)
            game.shapeRenderer.circle(p.x, p.y, if (p.fromPlayer) 6f else 7f, 10)
            game.shapeRenderer.setColor(1f, 1f, 1f, 0.85f)
            game.shapeRenderer.circle(p.x, p.y, 2.5f, 8)
        }
        particles.forEach { p ->
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            game.shapeRenderer.setColor(p.r, p.g, p.b, alpha * 0.9f)
            game.shapeRenderer.circle(p.x, p.y, p.size * alpha, 8)
        }
        game.shapeRenderer.end()

        // Expanding slash / spell rings
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        combatEngine.hitEffects.forEach { fx ->
            val progress = 1f - (fx.lifetime / fx.maxLifetime).coerceIn(0f, 1f)
            val c = hud.getElementColor(fx.element)
            game.shapeRenderer.setColor(c.r, c.g, c.b, 1f - progress)
            game.shapeRenderer.circle(fx.x, fx.y, fx.radius * (0.35f + 0.65f * progress), 20)
        }
        game.shapeRenderer.end()
    }

    private fun drawWeatherOverlay() {
        if (GameSettings.graphicsQuality == GraphicsQuality.LOW || world.weatherType == WeatherType.CLEAR) return
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        when (world.weatherType) {
            WeatherType.RAIN, WeatherType.STORM -> {
                game.shapeRenderer.setColor(0.65f, 0.8f, 1f, 0.35f)
                weatherDrops.forEach { d ->
                    game.shapeRenderer.rectLine(d.x, d.y, d.x - 6f, d.y - 16f, 1.5f)
                }
            }
            WeatherType.FOG -> {
                game.shapeRenderer.setColor(0.75f, 0.82f, 0.9f, 0.14f)
                game.shapeRenderer.rect(0f, 0f, EtheriaGame.VIRTUAL_WIDTH, EtheriaGame.VIRTUAL_HEIGHT)
            }
            WeatherType.CLEAR -> {}
        }
        game.shapeRenderer.end()
    }

    private fun renderTopMenuButtons() {
        val btnY = EtheriaGame.VIRTUAL_HEIGHT - 36f
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        topButtons.forEach { btn ->
            val active = menuManager.currentMenu == btn.type
            game.shapeRenderer.setColor(if (active) 0.25f else 0.08f, if (active) 0.52f else 0.12f, if (active) 0.92f else 0.25f, 0.86f)
            game.shapeRenderer.rect(btn.x, btnY, 56f, 26f)
        }
        game.shapeRenderer.end()

        game.batch.begin()
        font.color = Color.WHITE
        topButtons.forEach { btn ->
            font.draw(game.batch, btn.label, btn.x + 12f, btnY + 19f)
        }
        game.batch.end()
    }

    private fun renderSkillCooldowns() {
        val ch = party.activeCharacter
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        if (ch.skillCooldown > 0f) {
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.58f)
            game.shapeRenderer.circle(gamepad.skillBtn.x, gamepad.skillBtn.y, gamepad.skillBtn.r, 18)
        }
        if (ch.ultimateCooldown > 0f || ch.stats.elementalEnergy < ch.stats.maxElementalEnergy) {
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.45f)
            game.shapeRenderer.circle(gamepad.ultimateBtn.x, gamepad.ultimateBtn.y, gamepad.ultimateBtn.r, 18)
        }
        if (ch.dashCooldown > 0f) {
            game.shapeRenderer.setColor(0f, 0f, 0f, 0.55f)
            game.shapeRenderer.circle(gamepad.dashBtn.x, gamepad.dashBtn.y, gamepad.dashBtn.r, 18)
        }
        game.shapeRenderer.end()

        game.batch.begin()
        font.color = Color.WHITE
        if (ch.skillCooldown > 0f) {
            font.draw(game.batch, "${ch.skillCooldown.toInt() + 1}s", gamepad.skillBtn.x - 10f, gamepad.skillBtn.y + 6f)
        }
        if (ch.ultimateCooldown > 0f) {
            font.draw(game.batch, "${ch.ultimateCooldown.toInt() + 1}s", gamepad.ultimateBtn.x - 10f, gamepad.ultimateBtn.y + 6f)
        }
        if (ch.dashCooldown > 0f) {
            font.draw(game.batch, "${ch.dashCooldown.toInt() + 1}s", gamepad.dashBtn.x - 10f, gamepad.dashBtn.y + 6f)
        }
        game.batch.end()
    }

    private fun renderDialogue() {
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.03f, 0.04f, 0.1f, 0.9f)
        game.shapeRenderer.rect(60f, 24f, EtheriaGame.VIRTUAL_WIDTH - 120f, 118f)
        game.shapeRenderer.setColor(0.35f, 0.65f, 0.95f, 0.9f)
        game.shapeRenderer.rect(60f, 139f, EtheriaGame.VIRTUAL_WIDTH - 120f, 3f)
        game.shapeRenderer.end()

        game.batch.begin()
        font.color = Color(1f, 0.88f, 0.4f, 1f)
        font.draw(game.batch, dialogueNPC?.name ?: "", 84f, 126f)
        font.color = Color.WHITE
        font.draw(game.batch, dialogueText, 84f, 94f)
        font.color = Color(0.55f, 0.95f, 0.65f, 1f)
        font.draw(game.batch, "Tap anywhere or press [E] to continue", EtheriaGame.VIRTUAL_WIDTH - 360f, 48f)
        game.batch.end()
    }

    private fun renderQuestNotification() {
        val alpha = minOf(1f, questNotifTimer).coerceIn(0f, 1f)
        game.shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        game.shapeRenderer.setColor(0.05f, 0.08f, 0.15f, 0.82f * alpha)
        game.shapeRenderer.rect(EtheriaGame.VIRTUAL_WIDTH * 0.25f, EtheriaGame.VIRTUAL_HEIGHT - 128f, EtheriaGame.VIRTUAL_WIDTH * 0.5f, 42f)
        game.shapeRenderer.end()

        game.batch.begin()
        font.color = Color(1f, 0.9f, 0.38f, alpha)
        font.draw(game.batch, questNotifText, EtheriaGame.VIRTUAL_WIDTH * 0.27f, EtheriaGame.VIRTUAL_HEIGHT - 101f)
        game.batch.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        uiViewport.update(width, height, true)
    }

    override fun pause() {
        game.saveManager.save(party, inventory, quests, world, party.activeCharacter.position, isAuto = true)
    }

    override fun resume() {}
    override fun hide() {}

    override fun dispose() {
        b2dWorld.dispose()
        hud.dispose()
        gamepad.dispose()
        menuManager.dispose()
        miniMap.dispose()
        font.dispose()
    }
}
