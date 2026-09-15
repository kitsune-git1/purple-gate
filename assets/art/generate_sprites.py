#!/usr/bin/env python3
"""Purple Gate — 16-bit chunky pixel-art sprite bank generator.

Palette: grape purple, slime green, torch orange, gold, mud brown.
Transparent backgrounds (RGBA). Characters ~64–128px; portal larger.
Funny greedy goblin fantasy — NOT sci-fi / space.
"""
from __future__ import annotations

import math
import os
from pathlib import Path

from PIL import Image, ImageDraw

# --- Palette (documented in ART.md) ---
GRAPE = (118, 52, 160, 255)
GRAPE_BRIGHT = (178, 102, 220, 255)
GRAPE_DEEP = (64, 24, 96, 255)
GRAPE_CORE = (40, 10, 70, 255)
SLIME = (110, 190, 70, 255)
SLIME_DARK = (60, 120, 40, 255)
SLIME_LIGHT = (160, 230, 110, 255)
TORCH = (240, 140, 40, 255)
TORCH_BRIGHT = (255, 200, 80, 255)
TORCH_DARK = (180, 80, 20, 255)
GOLD = (230, 180, 40, 255)
GOLD_BRIGHT = (255, 230, 100, 255)
GOLD_DARK = (160, 110, 20, 255)
MUD = (120, 80, 50, 255)
MUD_DARK = (70, 45, 28, 255)
MUD_LIGHT = (160, 120, 80, 255)
CAMP_DIRT = (90, 60, 40, 255)
WHITE = (250, 250, 240, 255)
BLACK = (20, 16, 24, 255)
EYE_WHITE = (245, 245, 230, 255)
BLOOD = (160, 40, 40, 255)
ICE = (140, 210, 240, 255)
ICE_BRIGHT = (210, 240, 255, 255)
SPARK = (255, 245, 120, 255)
SPARK_BLUE = (180, 200, 255, 255)
TRANSPARENT = (0, 0, 0, 0)

OUT = Path("/workspace/purple-gate/art_gen/out")
REPO_ART = Path("/workspace/purple-gate/repo/assets/art")
DRAWABLE = Path("/workspace/purple-gate/repo/app/src/main/res/drawable-nodpi")


def new_img(w: int, h: int) -> Image.Image:
    return Image.new("RGBA", (w, h), TRANSPARENT)


def px(draw: ImageDraw.ImageDraw, x: int, y: int, color, size: int = 1):
    draw.rectangle([x, y, x + size - 1, y + size - 1], fill=color)


def fill_rect(draw, x, y, w, h, color):
    draw.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def ellipse_fill(img: Image.Image, cx, cy, rx, ry, color, chunk=2):
    """Chunky pixel ellipse."""
    draw = ImageDraw.Draw(img)
    for y in range(cy - ry, cy + ry + 1, chunk):
        for x in range(cx - rx, cx + rx + 1, chunk):
            nx = (x - cx) / max(rx, 1)
            ny = (y - cy) / max(ry, 1)
            if nx * nx + ny * ny <= 1.0:
                fill_rect(draw, x, y, chunk, chunk, color)


def save(img: Image.Image, name: str):
    OUT.mkdir(parents=True, exist_ok=True)
    REPO_ART.mkdir(parents=True, exist_ok=True)
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    path = OUT / f"{name}.png"
    img.save(path, "PNG")
    img.save(REPO_ART / f"{name}.png", "PNG")
    img.save(DRAWABLE / f"{name}.png", "PNG")
    print(f"  wrote {name}.png ({img.size[0]}x{img.size[1]})")
    return path


# ---------- PORTAL ----------
def gen_portal():
    w, h = 160, 192
    img = new_img(w, h)
    cx, cy = w // 2, h // 2
    # Outer glow rings (chunky)
    for i, (rx, ry, col) in enumerate([
        (70, 88, (*GRAPE_DEEP[:3], 180)),
        (62, 78, GRAPE),
        (52, 66, GRAPE_BRIGHT),
        (40, 52, GRAPE),
        (28, 36, GRAPE_CORE),
        (16, 20, (*GRAPE_BRIGHT[:3], 200)),
        (8, 10, WHITE),
    ]):
        ellipse_fill(img, cx, cy, rx, ry, col, chunk=2)
    # Swirl arcs as pixel blobs
    draw = ImageDraw.Draw(img)
    for a in range(0, 360, 40):
        rad = math.radians(a)
        for r in (34, 48, 58):
            x = int(cx + math.cos(rad + r * 0.02) * r)
            y = int(cy + math.sin(rad + r * 0.02) * (r * 1.25))
            fill_rect(draw, x - 2, y - 2, 4, 4, GRAPE_BRIGHT if a % 80 == 0 else GRAPE)
    # Stone/mud rim
    for a in range(0, 360, 8):
        rad = math.radians(a)
        x = int(cx + math.cos(rad) * 68)
        y = int(cy + math.sin(rad) * 86)
        fill_rect(draw, x - 3, y - 3, 6, 6, MUD if a % 16 == 0 else MUD_DARK)
        if a % 24 == 0:
            fill_rect(draw, x - 2, y - 2, 3, 3, GOLD_DARK)  # greedy runes
    # Tiny sparkles
    for sx, sy in [(40, 50), (120, 60), (50, 140), (110, 130), (80, 40)]:
        fill_rect(draw, sx, sy, 3, 3, GOLD_BRIGHT)
    return save(img, "portal_purple")


# ---------- GOBLIN helpers ----------
def draw_goblin(img, ox, oy, casting=False, grabber=False, scale=2):
    """Draw chunky goblin at offset. scale=2 → ~64–96px tall."""
    d = ImageDraw.Draw(img)
    s = scale

    def r(x, y, w, h, c):
        fill_rect(d, ox + x * s, oy + y * s, w * s, h * s, c)

    # Legs
    r(6, 28, 5, 8, SLIME_DARK)
    r(13, 28, 5, 8, SLIME_DARK)
    # Feet
    r(5, 35, 6, 3, MUD_DARK)
    r(13, 35, 6, 3, MUD_DARK)
    # Body
    r(5, 16, 14, 13, SLIME)
    r(6, 17, 12, 4, SLIME_LIGHT)
    # Belly pouch / gold greed
    r(8, 22, 8, 5, GOLD_DARK if grabber else SLIME_DARK)
    if grabber:
        r(9, 23, 2, 2, GOLD_BRIGHT)
        r(12, 24, 2, 2, GOLD)
    # Arms
    if casting:
        r(1, 14, 5, 4, SLIME)  # left back
        r(18, 10, 8, 4, SLIME)  # casting arm up
        r(24, 8, 4, 4, TORCH_BRIGHT)  # spell spark in hand
        r(25, 6, 3, 3, TORCH)
    else:
        r(1, 18, 5, 8, SLIME)
        r(18, 18, 5, 8, SLIME)
        r(0, 24, 4, 3, SLIME_DARK)
        r(20, 24, 4, 3, SLIME_DARK)
    # Head
    r(6, 6, 12, 11, SLIME)
    r(7, 7, 10, 3, SLIME_LIGHT)
    # Ears
    r(3, 8, 4, 5, SLIME)
    r(17, 8, 4, 5, SLIME)
    r(2, 9, 2, 3, SLIME_DARK)
    r(20, 9, 2, 3, SLIME_DARK)
    # Eyes (greedy)
    r(8, 10, 3, 3, EYE_WHITE)
    r(13, 10, 3, 3, EYE_WHITE)
    r(9, 11, 2, 2, BLACK)
    r(14, 11, 2, 2, BLACK)
    # Nose / grin
    r(11, 13, 2, 2, SLIME_DARK)
    r(9, 15, 6, 2, BLACK)
    r(9, 15, 2, 1, SLIME_LIGHT)  # tooth
    r(13, 15, 1, 1, SLIME_LIGHT)
    # Hair tuft
    r(10, 4, 4, 3, SLIME_DARK)
    if grabber:
        # little sack
        r(20, 20, 7, 8, MUD)
        r(21, 21, 5, 3, GOLD)
        r(22, 18, 3, 3, MUD_DARK)  # tie


def gen_goblin_idle():
    img = new_img(64, 80)
    draw_goblin(img, 8, 4, casting=False, scale=2)
    return save(img, "goblin_idle")


def gen_goblin_cast():
    img = new_img(72, 80)
    draw_goblin(img, 8, 4, casting=True, scale=2)
    return save(img, "goblin_cast")


def gen_grabber():
    img = new_img(72, 80)
    draw_goblin(img, 8, 4, casting=False, grabber=True, scale=2)
    return save(img, "grabber_goblin")


# ---------- SPELLS ----------
def gen_spell(name: str, colors: list):
    img = new_img(32, 32)
    d = ImageDraw.Draw(img)
    # Chunky orb with glow
    cx, cy = 16, 16
    for r, c in zip([14, 10, 7, 4], colors):
        ellipse_fill(img, cx, cy, r, r, c, chunk=2)
    # Highlight
    fill_rect(d, 10, 9, 4, 4, WHITE)
    fill_rect(d, 20, 18, 3, 3, colors[0])
    return save(img, name)


def gen_spells():
    gen_spell("spell_fire", [TORCH_DARK, TORCH, TORCH_BRIGHT, GOLD_BRIGHT])
    gen_spell("spell_lightning", [SPARK_BLUE, SPARK, WHITE, SPARK])
    gen_spell("spell_ice", [(*ICE[:3], 200), ICE, ICE_BRIGHT, WHITE])


# ---------- GOLD PILES ----------
def gen_gold_pile(stage: int):
    """1=few coins … 5=buried camp."""
    w, h = 128, 96
    img = new_img(w, h)
    d = ImageDraw.Draw(img)
    base_y = 80

    def coin(x, y, big=False):
        s = 6 if big else 4
        fill_rect(d, x, y, s, s - 1, GOLD)
        fill_rect(d, x + 1, y + 1, s - 2, 1, GOLD_BRIGHT)
        fill_rect(d, x, y + s - 2, s, 1, GOLD_DARK)

    if stage == 1:
        for i, (x, y) in enumerate([(50, 70), (60, 72), (70, 69), (55, 64)]):
            coin(x, y)
    elif stage == 2:
        for i in range(12):
            coin(40 + (i % 5) * 10 + (i % 3), 68 - (i // 5) * 8, big=i % 4 == 0)
    elif stage == 3:
        # mound
        for layer, n in [(0, 10), (1, 8), (2, 6), (3, 4)]:
            for i in range(n):
                x = 30 + i * 8 + layer * 4
                y = base_y - 8 - layer * 10
                coin(x, y, big=True)
        # fill under
        fill_rect(d, 34, 55, 70, 28, GOLD_DARK)
        fill_rect(d, 40, 48, 55, 12, GOLD)
        fill_rect(d, 50, 42, 30, 8, GOLD_BRIGHT)
    elif stage == 4:
        fill_rect(d, 20, 50, 90, 35, GOLD_DARK)
        fill_rect(d, 28, 38, 75, 20, GOLD)
        fill_rect(d, 40, 28, 50, 16, GOLD_BRIGHT)
        # sparkle crest
        fill_rect(d, 60, 18, 6, 6, WHITE)
        fill_rect(d, 58, 20, 10, 2, GOLD_BRIGHT)
        for i in range(20):
            coin(22 + (i % 8) * 11, 55 + (i // 8) * 10, big=True)
    else:  # 5 buried
        # huge hill
        fill_rect(d, 8, 40, 112, 50, GOLD_DARK)
        fill_rect(d, 16, 28, 96, 30, GOLD)
        fill_rect(d, 30, 16, 68, 20, GOLD_BRIGHT)
        fill_rect(d, 48, 8, 32, 12, GOLD)
        # buried tent/camp tip poking out
        fill_rect(d, 70, 20, 18, 14, MUD)
        fill_rect(d, 74, 12, 10, 10, TORCH)  # torch tip
        fill_rect(d, 76, 8, 6, 6, TORCH_BRIGHT)
        # sparkles
        for sx, sy in [(40, 12), (90, 18), (55, 6), (100, 30)]:
            fill_rect(d, sx, sy, 4, 4, WHITE)
        for i in range(30):
            coin(10 + (i % 10) * 11, 50 + (i // 10) * 12, big=True)
    return save(img, f"gold_pile_{stage}")


def gen_coin_single():
    img = new_img(16, 16)
    d = ImageDraw.Draw(img)
    fill_rect(d, 2, 2, 12, 12, GOLD_DARK)
    fill_rect(d, 3, 3, 10, 10, GOLD)
    fill_rect(d, 5, 4, 6, 2, GOLD_BRIGHT)
    fill_rect(d, 6, 7, 4, 4, GOLD_DARK)  # stamp
    fill_rect(d, 7, 8, 2, 2, GOLD_BRIGHT)
    return save(img, "coin_single")


# ---------- BEASTS ----------
def gen_beast_dire_rat():
    img = new_img(80, 64)
    d = ImageDraw.Draw(img)
    # body
    fill_rect(d, 18, 28, 36, 18, MUD)
    fill_rect(d, 20, 30, 30, 8, MUD_LIGHT)
    # head
    fill_rect(d, 48, 24, 18, 16, MUD)
    fill_rect(d, 58, 30, 12, 8, MUD_LIGHT)  # snout
    # ears
    fill_rect(d, 50, 16, 6, 10, MUD)
    fill_rect(d, 58, 14, 6, 12, MUD)
    fill_rect(d, 52, 18, 3, 4, BLOOD)
    # eyes greedy yellow
    fill_rect(d, 52, 28, 4, 4, GOLD_BRIGHT)
    fill_rect(d, 53, 29, 2, 2, BLACK)
    # legs
    for x in (20, 30, 40, 48):
        fill_rect(d, x, 44, 5, 10, MUD_DARK)
    # tail
    fill_rect(d, 8, 34, 12, 4, MUD_LIGHT)
    fill_rect(d, 4, 30, 6, 4, MUD)
    # teeth
    fill_rect(d, 66, 34, 2, 3, WHITE)
    fill_rect(d, 69, 34, 2, 3, WHITE)
    return save(img, "beast_dire_rat")


def gen_beast_wolf():
    img = new_img(96, 72)
    d = ImageDraw.Draw(img)
    fill_rect(d, 16, 30, 50, 22, MUD_DARK)
    fill_rect(d, 20, 32, 40, 10, MUD)
    # head
    fill_rect(d, 58, 20, 22, 20, MUD_DARK)
    fill_rect(d, 70, 28, 16, 10, MUD)  # muzzle
    # ears pointed
    fill_rect(d, 60, 8, 6, 14, MUD_DARK)
    fill_rect(d, 72, 6, 6, 16, MUD_DARK)
    fill_rect(d, 62, 10, 3, 6, BLOOD)
    # eyes
    fill_rect(d, 64, 26, 4, 4, TORCH)
    fill_rect(d, 65, 27, 2, 2, BLACK)
    # legs
    for x in (20, 34, 48, 58):
        fill_rect(d, x, 50, 6, 14, MUD_DARK)
        fill_rect(d, x - 1, 62, 8, 4, BLACK)
    # tail bushy
    fill_rect(d, 6, 28, 14, 10, MUD)
    fill_rect(d, 4, 24, 8, 8, MUD_LIGHT)
    # fangs
    fill_rect(d, 78, 36, 3, 5, WHITE)
    fill_rect(d, 82, 36, 3, 5, WHITE)
    return save(img, "beast_wolf")


def gen_beast_war_boar():
    img = new_img(112, 80)
    d = ImageDraw.Draw(img)
    # bulky body
    fill_rect(d, 20, 28, 60, 32, MUD)
    fill_rect(d, 24, 32, 50, 12, MUD_LIGHT)
    # armor plates (war)
    fill_rect(d, 30, 26, 40, 6, MUD_DARK)
    fill_rect(d, 36, 22, 12, 6, GOLD_DARK)
    # head
    fill_rect(d, 70, 30, 28, 24, MUD)
    fill_rect(d, 88, 38, 16, 12, MUD_LIGHT)
    # tusks
    fill_rect(d, 92, 48, 4, 12, WHITE)
    fill_rect(d, 100, 46, 4, 14, WHITE)
    fill_rect(d, 93, 50, 2, 8, GOLD_BRIGHT)
    # eye
    fill_rect(d, 78, 36, 5, 5, TORCH_BRIGHT)
    fill_rect(d, 79, 37, 3, 3, BLACK)
    # legs thick
    for x in (24, 40, 56, 70):
        fill_rect(d, x, 56, 10, 16, MUD_DARK)
        fill_rect(d, x, 70, 10, 4, BLACK)
    # snout steam / snort
    fill_rect(d, 104, 34, 4, 4, (*WHITE[:3], 180))
    return save(img, "beast_war_boar")


def gen_beast_troll():
    img = new_img(96, 128)
    d = ImageDraw.Draw(img)
    # legs
    fill_rect(d, 28, 90, 14, 28, SLIME_DARK)
    fill_rect(d, 54, 90, 14, 28, SLIME_DARK)
    fill_rect(d, 26, 116, 18, 6, MUD_DARK)
    fill_rect(d, 52, 116, 18, 6, MUD_DARK)
    # body huge
    fill_rect(d, 22, 48, 52, 46, SLIME)
    fill_rect(d, 26, 52, 44, 16, SLIME_LIGHT)
    # belly
    fill_rect(d, 32, 68, 32, 20, SLIME_DARK)
    # arms
    fill_rect(d, 4, 52, 20, 14, SLIME)
    fill_rect(d, 72, 52, 20, 14, SLIME)
    fill_rect(d, 2, 64, 16, 20, SLIME_DARK)
    fill_rect(d, 78, 64, 16, 20, SLIME_DARK)
    # club
    fill_rect(d, 80, 70, 10, 36, MUD)
    fill_rect(d, 78, 66, 14, 10, MUD_DARK)
    # head
    fill_rect(d, 30, 18, 36, 32, SLIME)
    fill_rect(d, 34, 22, 28, 10, SLIME_LIGHT)
    # underbite
    fill_rect(d, 36, 42, 24, 8, SLIME_DARK)
    fill_rect(d, 40, 44, 4, 6, WHITE)
    fill_rect(d, 52, 44, 4, 6, WHITE)
    # eyes small dumb
    fill_rect(d, 38, 28, 6, 6, EYE_WHITE)
    fill_rect(d, 52, 28, 6, 6, EYE_WHITE)
    fill_rect(d, 40, 30, 3, 3, BLACK)
    fill_rect(d, 54, 30, 3, 3, BLACK)
    # wart
    fill_rect(d, 58, 36, 4, 4, SLIME_DARK)
    return save(img, "beast_troll")


def gen_beast_minotaur():
    img = new_img(112, 128)
    d = ImageDraw.Draw(img)
    # legs
    fill_rect(d, 32, 92, 16, 28, MUD_DARK)
    fill_rect(d, 64, 92, 16, 28, MUD_DARK)
    fill_rect(d, 28, 118, 22, 6, BLACK)
    fill_rect(d, 60, 118, 22, 6, BLACK)
    # body
    fill_rect(d, 28, 50, 56, 46, MUD)
    fill_rect(d, 32, 54, 48, 16, MUD_LIGHT)
    # loincloth gold greedy
    fill_rect(d, 40, 80, 32, 14, GOLD_DARK)
    fill_rect(d, 44, 82, 24, 6, GOLD)
    # arms
    fill_rect(d, 8, 54, 22, 16, MUD)
    fill_rect(d, 82, 54, 22, 16, MUD)
    fill_rect(d, 6, 68, 18, 24, MUD_DARK)
    fill_rect(d, 88, 68, 18, 24, MUD_DARK)
    # axe
    fill_rect(d, 92, 40, 8, 50, MUD_LIGHT)
    fill_rect(d, 84, 36, 24, 14, GOLD_DARK)
    fill_rect(d, 86, 38, 20, 8, GOLD)
    # head bull
    fill_rect(d, 34, 22, 44, 30, MUD)
    fill_rect(d, 40, 28, 32, 12, MUD_LIGHT)
    # snout
    fill_rect(d, 44, 40, 24, 14, MUD_DARK)
    fill_rect(d, 48, 48, 6, 4, BLACK)  # nostrils
    fill_rect(d, 58, 48, 6, 4, BLACK)
    # horns big
    fill_rect(d, 18, 8, 10, 24, GOLD_DARK)
    fill_rect(d, 20, 4, 8, 8, GOLD)
    fill_rect(d, 84, 8, 10, 24, GOLD_DARK)
    fill_rect(d, 86, 4, 8, 8, GOLD)
    fill_rect(d, 14, 14, 8, 6, GOLD_BRIGHT)
    fill_rect(d, 92, 14, 8, 6, GOLD_BRIGHT)
    # eyes angry
    fill_rect(d, 42, 30, 6, 6, TORCH)
    fill_rect(d, 64, 30, 6, 6, TORCH)
    fill_rect(d, 44, 32, 3, 3, BLACK)
    fill_rect(d, 66, 32, 3, 3, BLACK)
    return save(img, "beast_minotaur")


# ---------- CAMP BG ----------
def gen_camp_bg():
    w, h = 360, 640  # 9:16
    img = Image.new("RGBA", (w, h), CAMP_DIRT)
    d = ImageDraw.Draw(img)
    # night sky top
    for y in range(0, 220, 4):
        t = y / 220
        c = (
            int(30 + t * 40),
            int(16 + t * 30),
            int(40 + t * 20),
            255,
        )
        fill_rect(d, 0, y, w, 4, c)
    # mud ground bands
    for y in range(220, h, 8):
        shade = MUD_DARK if (y // 8) % 2 == 0 else MUD
        fill_rect(d, 0, y, w, 8, shade)
    # dirt patches
    for x, y in [(40, 400), (200, 450), (100, 500), (280, 380)]:
        fill_rect(d, x, y, 40, 20, MUD_LIGHT)
    # left torch
    fill_rect(d, 28, 180, 8, 80, MUD_DARK)
    fill_rect(d, 22, 160, 20, 24, TORCH)
    fill_rect(d, 26, 150, 12, 14, TORCH_BRIGHT)
    fill_rect(d, 18, 170, 28, 16, (*TORCH[:3], 100))
    # right torch
    fill_rect(d, 324, 180, 8, 80, MUD_DARK)
    fill_rect(d, 318, 160, 20, 24, TORCH)
    fill_rect(d, 322, 150, 12, 14, TORCH_BRIGHT)
    fill_rect(d, 314, 170, 28, 16, (*TORCH[:3], 100))
    # tent silhouette left
    fill_rect(d, 40, 320, 60, 40, MUD_DARK)
    pts_hint = [(40, 320), (70, 280), (100, 320)]
    fill_rect(d, 55, 290, 30, 30, MUD_DARK)
    # crate
    fill_rect(d, 280, 480, 40, 30, MUD)
    fill_rect(d, 282, 482, 36, 8, MUD_LIGHT)
    return save(img, "camp_bg")


# ---------- SPRITE SHEETS ----------
def stitch_h(paths: list[Path], out_name: str, frame_w: int | None = None):
    imgs = [Image.open(p).convert("RGBA") for p in paths]
    # normalize height; pad widths
    max_h = max(i.height for i in imgs)
    widths = []
    norm = []
    for i in imgs:
        canvas = new_img(frame_w or i.width, max_h)
        canvas.paste(i, (0, max_h - i.height))
        if frame_w and i.width < frame_w:
            pass
        norm.append(canvas if not frame_w else _pad_w(i, frame_w, max_h))
        widths.append(frame_w or i.width)
    if frame_w:
        total_w = frame_w * len(imgs)
        sheet = new_img(total_w, max_h)
        for idx, im in enumerate(norm):
            sheet.paste(im, (idx * frame_w, 0), im)
    else:
        total_w = sum(widths)
        sheet = new_img(total_w, max_h)
        x = 0
        for im, w in zip(norm, widths):
            sheet.paste(im, (x, 0), im)
            x += w
    sheets = REPO_ART / "sheets"
    sheets.mkdir(parents=True, exist_ok=True)
    outp = sheets / out_name
    sheet.save(outp, "PNG")
    # also copy sheet name into drawable? skip — sheets stay in assets/art/sheets
    print(f"  sheet {out_name} ({sheet.size[0]}x{sheet.size[1]}) frames={len(imgs)}")
    return sheet.size, len(imgs)


def _pad_w(im: Image.Image, fw: int, fh: int) -> Image.Image:
    canvas = new_img(fw, fh)
    ox = (fw - im.width) // 2
    oy = fh - im.height
    canvas.paste(im, (ox, oy), im)
    return canvas


def main():
    print("Generating Purple Gate pixel-art bank…")
    gen_portal()
    gen_goblin_idle()
    gen_goblin_cast()
    gen_grabber()
    gen_spells()
    for s in range(1, 6):
        gen_gold_pile(s)
    gen_coin_single()
    gen_beast_dire_rat()
    gen_beast_wolf()
    gen_beast_war_boar()
    gen_beast_troll()
    gen_beast_minotaur()
    gen_camp_bg()

    print("Stitching sheets…")
    stitch_h(
        [OUT / f"spell_{n}.png" for n in ("fire", "lightning", "ice")],
        "spells.png",
        frame_w=32,
    )
    stitch_h(
        [OUT / f"gold_pile_{i}.png" for i in range(1, 6)],
        "gold_piles.png",
        frame_w=128,
    )
    stitch_h(
        [
            OUT / "beast_dire_rat.png",
            OUT / "beast_wolf.png",
            OUT / "beast_war_boar.png",
            OUT / "beast_troll.png",
            OUT / "beast_minotaur.png",
        ],
        "beasts.png",
        frame_w=112,
    )
    print("Done.")


if __name__ == "__main__":
    main()
