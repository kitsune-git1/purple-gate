# Purple Gate — art bank

## Source
Key sprites regenerated with the Grok Bot **image generator** (magenta chroma keyed to alpha), then downscaled with nearest-neighbour for runtime drawables.

Pillow procedural sprites were the prior bank; this supersedes character/prop art for Camp.

## Palette
Grape purple, slime green, torch orange, gold, mud brown (plus cyan for lightning/ice).

## Runtime drawables
`app/src/main/res/drawable-nodpi/` — used by `CampCanvas` via `ImageBitmap` + nearest-neighbour.

| File | Role | Approx max side |
|------|------|-----------------|
| portal_purple.png | Camp portal | 384 |
| goblin_idle.png / goblin_cast.png | Caster poses | 256 |
| grabber_goblin.png | Hired Grabber | 256 |
| spell_fire/lightning/ice.png | Projectiles | 96–128 |
| gold_pile_1…5.png | Pile stages | 320 |
| coin_single.png | Coin arc VFX | 64 |
| beast_*.png | Raid beasts | 280 |
| camp_bg.png | Backdrop (legacy Pillow) | — |

## Sheets
`assets/art/sheets/` — `spells.png`, `gold_piles.png`, `beasts.png` (built from runtime drawables).

## Higher-res sources
Full chroma-keyed exports also live under `assets/art/*.png` (and generation workspace `/workspace/purple-gate/imagine_sprites` on the build machine).
