# Purple Gate

Pixel goblins throw spells into a purple portal. Gold piles up. Send a minotaur through. Prestige into necromancy.

**Android idle clicker MVP** — Kotlin + Jetpack Compose + Material 3.

> Spells go **into** the purple portal for gold. This is **not** a black-hole / space / aiming clone.

## Playable now (v0.1 MVP)

1. **Camp** — tap the purple portal to cast the equipped school; gold arcs onto a growing pile
2. **Spells** — Fire → Lightning → Ice unlock path; short upgrade columns; familiars for idle auto-cast
3. **Beasts** — Dire Rat → Wolf → War Boar → Troll → Minotaur timed raids (timer only)
4. **Grabber** goblin hire on Camp
5. **Offline progress** via DataStore (gold + last-seen; grant on resume)
6. **Prestige** — Slam Gate when Fire+Lightning+Ice familiars are online and the pile is huge → Grimoire pages, Necromancy stub, reset (new city starts Fire)
7. Tabs: Camp / Spells / Beasts / Prestige · HUD: gold, gold/sec, raid timer
8. Portrait, one-thumb; pixel feel via Compose Canvas (grape purple, slime green, torch orange, gold, mud brown)

See [DESIGN_BRIEF.md](DESIGN_BRIEF.md) for the full design source of truth.

## Tech

| Item | Value |
|------|--------|
| Package | `com.purplegate.app` |
| minSdk | 26 |
| targetSdk / compileSdk | 35 |
| UI | Jetpack Compose + Material 3 |
| State | ViewModel + DataStore Preferences |
| Build | AGP 8.7.3 · Kotlin 2.0.21 · Gradle 8.11.1 |

## Build

### Requirements

- JDK 17+ (21 recommended)
- Android SDK with `platforms;android-35` and `build-tools;35.0.0` (or compatible)
- Set `sdk.dir` in `local.properties` (or `ANDROID_HOME`)

```bash
# Example local.properties
sdk.dir=/path/to/Android/Sdk
```

### Assemble debug APK

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Install on device/emulator

```bash
./gradlew installDebug
```

## Project layout

```
app/src/main/java/com/purplegate/app/
  MainActivity.kt
  game/Models.kt          # schools, beasts, economy helpers
  data/GameRepository.kt  # DataStore persistence + offline
  viewmodel/GameViewModel.kt
  ui/components/CampCanvas.kt   # portal, gold pile, spells, goblin
  ui/screens/                   # Camp, Spells, Beasts, Prestige, shell
  ui/theme/
```

## Out of scope (for later)

Full gacha, deep classes past Necromancy sketch, ads, beasts past Minotaur, aiming / projectile skill.

## License

All rights reserved for now — personal MVP scaffold.
