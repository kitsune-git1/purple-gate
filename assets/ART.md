# Purple Gate — art bank

## Direction
Low-detail **tiny chunky** pixel art (small native pixel count), inspired by mobile idle clicker pixel *scale/quality* (e.g. Space Clicker / Black Hole store screenshots) — **not** that IP’s space theme.

Sprites are image-generated, chroma-keyed, then **forced through a tiny native grid** (≈12–40px max side) and nearest-neighbour upscaled ×4 so pixels stay hard squares.

## Runtime
`app/src/main/res/drawable-nodpi/` — loaded by `CampCanvas`.

## Sheets
`assets/art/sheets/` — spells, gold_piles, beasts.

## Style refs (local build machine)
`/workspace/purple-gate/style-ref/bh_*.png` — Play Store gallery captures for scale only.

## Portal centrepiece
`portal_purple.png` is intentionally higher native detail (~112px grid ×3) than unit sprites. Alternate ornate take: `assets/art/portal_purple_ornate_alt.png`.
