# Purple Gate — art bank

16-bit chunky pixel art for the goblin camp idle clicker. Funny greedy fantasy — **not** sci-fi / space / black holes.

## Style bible

| Rule | Detail |
|------|--------|
| Look | Game sprite, chunky pixels, readable at phone size |
| Palette | Grape purple `#7634A0` / `#B266DC`, slime green `#6EBE46`, torch orange `#F08C28`, gold `#E6B428`, mud brown `#785032` |
| Background | **Transparent RGBA** on all sprites (camp_bg is opaque dirt/torch scene) |
| Scale | Characters ~64–128px tall; portal 160×192; spells 32×32; coin 16×16 |

## Individual sprites (`app/src/main/res/drawable-nodpi/`)

Android resource names are lowercase snake (no capitals). Same files also live under `assets/art/` for the source bank.

| File | Size | Use |
|------|------|-----|
| `portal_purple.png` | 160×192 | Camp portal (front oval swirl + mud rim) |
| `goblin_idle.png` | 64×80 | Idle caster goblin |
| `goblin_cast.png` | 72×80 | Goblin mid-cast |
| `grabber_goblin.png` | 72×80 | Hired Grabber (sack + gold belly) |
| `spell_fire.png` | 32×32 | Fire projectile orb |
| `spell_lightning.png` | 32×32 | Lightning projectile orb |
| `spell_ice.png` | 32×32 | Ice projectile orb |
| `gold_pile_1.png` … `gold_pile_5.png` | 128×96 each | Stages: few coins → scatter → mound → hill → buried camp |
| `coin_single.png` | 16×16 | Flying coin VFX / starter |
| `beast_dire_rat.png` | 80×64 | Raid beast |
| `beast_wolf.png` | 96×72 | Raid beast |
| `beast_war_boar.png` | 112×80 | Raid beast |
| `beast_troll.png` | 96×128 | Raid beast |
| `beast_minotaur.png` | 112×128 | Raid beast |
| `camp_bg.png` | 360×640 | Optional 9:16 mud/torch camp backdrop |

## Sprite sheets (`assets/art/sheets/`)

| Sheet | Size | Frames | Frame size |
|-------|------|--------|------------|
| `spells.png` | 96×32 | 3 (fire, lightning, ice) | **32×32** L→R |
| `gold_piles.png` | 640×96 | 5 (stages 1–5) | **128×96** L→R |
| `beasts.png` | 560×128 | 5 (rat, wolf, boar, troll, minotaur) | **112×128** L→R (shorter beasts bottom-aligned / centered in cell) |

## Runtime mapping

- `GameState.pileStage` `0` → `coin_single` (starter); `1..5` → `gold_pile_1..5`
- `BeastId` → `beast_dire_rat` / `beast_wolf` / `beast_war_boar` / `beast_troll` / `beast_minotaur`
- Selected school → `spell_fire` / `spell_lightning` / `spell_ice`
- Grabber owned → `grabber_goblin`; cast pose → `goblin_cast` when a flying spell is active

Camp Compose UI loads drawables via `ImageBitmap` + `drawImage`, with Canvas shape fallbacks if a resource is absent.

## Regeneration

```bash
python3 /workspace/purple-gate/art_gen/generate_sprites.py
```

Copies into `assets/art/` and `app/src/main/res/drawable-nodpi/`.
