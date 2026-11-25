# ETHERIA: Echoes of Eternity
## Native Android 2D Open-World Anime Action RPG

---

## Overview

ETHERIA is a fully-featured native Android game built with **Kotlin** + **LibGDX**. It features:

- **Open-world exploration** with chunk-based world loading
- **4-character party system** (Sword Fighter, Archer, Mage, Heavy Fighter)
- **Real-time action combat** with 6 elements: Fire, Water, Ice, Lightning, Wind, Earth
- **Enemy AI** with patrol/chase/attack behaviors + multi-phase Boss
- **Quest system** with main story, side quests, NPC dialogue
- **Inventory & save system** with auto-save
- **Virtual touch controls** (joystick + action buttons)
- **Day/night cycle**, weather, particles, screen shake

---

## Project Structure

```
Etheria/
├── android/                    ← Android launcher module
│   ├── src/main/
│   │   ├── kotlin/com/etheria/game/
│   │   │   └── AndroidLauncher.kt
│   │   ├── res/                ← App icons, resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── core/                       ← LibGDX core (all game logic)
│   └── src/main/kotlin/com/etheria/game/
│       ├── EtheriaGame.kt      ← Main game class
│       ├── screens/
│       │   ├── LoadingScreen.kt
│       │   ├── MainMenuScreen.kt
│       │   ├── GameScreen.kt   ← Main gameplay screen
│       │   └── SettingsScreen.kt
│       ├── player/
│       │   ├── PlayableCharacter.kt  ← Abstract base + stats
│       │   ├── Characters.kt         ← Kai, Lyra, Zara, Ragna
│       │   └── PartyManager.kt
│       ├── combat/
│       │   └── CombatEngine.kt ← Damage calc, hit effects, elemental reactions
│       ├── enemy/
│       │   ├── Enemy.kt        ← All enemy types + AI state machine
│       │   └── EnemyManager.kt
│       ├── world/
│       │   ├── WorldManager.kt ← Chunk world, day/night, weather, NPCs
│       │   └── Item.kt
│       ├── inventory/
│       │   └── InventoryManager.kt
│       ├── quest/
│       │   └── QuestManager.kt
│       ├── save/
│       │   └── SaveManager.kt
│       ├── audio/
│       │   └── AudioManager.kt
│       ├── ui/
│       │   ├── HUDRenderer.kt
│       │   ├── VirtualGamepad.kt
│       │   ├── MenuManager.kt
│       │   └── MiniMapRenderer.kt
│       └── utils/
│           └── Utils.kt
│
├── assets/                     ← Game assets (shared by android module)
│   ├── sprites/
│   │   ├── player/
│   │   ├── enemies/
│   │   ├── world/
│   │   ├── ui/
│   │   └── effects/
│   ├── sounds/
│   │   ├── sfx/
│   │   └── music/
│   └── data/
│       └── game_data.yaml
│
├── gradle/
│   ├── libs.versions.toml      ← Version catalog
│   └── wrapper/
├── settings.gradle.kts
├── build.gradle.kts
└── setup_and_build.sh          ← One-step build script
```

---

## Building the APK

### Requirements
- Java 17 (JDK)
- Android SDK with:
  - Build Tools 34.0.0
  - Platform android-35

### Build Steps

```bash
# Option 1: Automated setup + build
bash setup_and_build.sh

# Option 2: Manual (after setup)
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug

# Output APK location:
# android/build/outputs/apk/debug/android-debug.apk
```

---

## Controls

| Left Side | Right Side |
|-----------|------------|
| Virtual Joystick | ATK – Normal attack |
| 360° movement | HVY – Heavy / charged attack |
| | JMP – Jump |
| | DASH – Dodge/dash |
| | SKLL – Elemental skill |
| | ULT – Ultimate ability |
| | P1–P4 – Party switch |

**Menu Buttons** (top center): INV | MAP | QST | CHR

**Keyboard shortcuts** (for testing):
- WASD / Arrow keys – Movement
- Z – Attack, X – Heavy, C – Skill, V – Ultimate
- Shift – Dash
- 1-4 – Switch party members
- I – Inventory, M – Map, J – Quest Journal, P – Character
- E – Interact with NPCs/chests
- F5 – Manual save

---

## Characters

| Character | Element | Class | Specialty |
|-----------|---------|-------|-----------|
| **Kai** | 🔥 Fire | Sword Fighter | Combo slashes, fire sweeps |
| **Lyra** | 💨 Wind | Archer | Ranged, arrow rain, storm |
| **Zara** | ❄️ Ice | Mage | High dmg, blizzard, AOE |
| **Ragna** | 🌍 Earth | Heavy Fighter | Tank, ground slam, quakes |

### Elemental Reactions (1.5× bonus damage):
- Fire → Ice
- Water → Fire  
- Ice → Wind
- Lightning → Water
- Wind → Earth
- Earth → Lightning

---

## Enemies

| Enemy | HP | Element | Notes |
|-------|-----|---------|-------|
| Slime | 120 | Water | Basic mob, bounces |
| Goblin | 200 | — | Fast, patrol |
| Skeleton | 300 | Earth | Tanky undead |
| Dark Knight | 600 | Fire | Elite armor |
| **Shadow Lord** | 5000 | Lightning | 3-phase boss |

---

## Technical Notes

- **Rendering**: LibGDX ShapeRenderer + SpriteBatch (programmatic art when atlas unavailable)
- **World**: Chunk-based (16×16 tiles per chunk, 32px tiles), 3-chunk radius loaded
- **Performance**: Object pooling, capped delta (50ms), lazy chunk loading
- **Save**: Plain-text key=value format in app's local storage
- **Physics**: Custom collision (Box2D ready, not yet integrated)

---

*ETHERIA © 2024 – Built with LibGDX on Android*
