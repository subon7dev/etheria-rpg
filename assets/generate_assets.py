#!/usr/bin/env python3
"""
ETHERIA – Echoes of Eternity Asset Generator
Generates pixel-art sprite sheets, tilesets, UI graphics, app icons, and synthesized WAV audio.
Uses pure Python standard library (bytearray + zlib + wave + math) for fast execution.
"""
import math
import os
import random
import struct
import wave
import zlib

ROOT = "/data/user/0/ai.antigem/files/home/projects/Etheria"
ASSETS = f"{ROOT}/assets"
ANDROID_RES = f"{ROOT}/android/src/main/res"


def save_png(path: str, w: int, h: int, buf: bytearray) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    stride = w * 4
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        start = y * stride
        raw.extend(buf[start:start + stride])
    compressed = zlib.compress(bytes(raw), 6)

    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    png = (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", compressed)
        + chunk(b"IEND", b"")
    )
    with open(path, "wb") as f:
        f.write(png)


class Canvas:
    def __init__(self, w: int, h: int):
        self.w = w
        self.h = h
        self.buf = bytearray(w * h * 4)

    def px(self, x: int, y: int, rgba: tuple[int, int, int, int]) -> None:
        if 0 <= x < self.w and 0 <= y < self.h:
            i = (y * self.w + x) * 4
            a = rgba[3]
            if a == 255:
                self.buf[i] = rgba[0]
                self.buf[i + 1] = rgba[1]
                self.buf[i + 2] = rgba[2]
                self.buf[i + 3] = 255
            elif a > 0:
                inv = 255 - a
                self.buf[i] = (rgba[0] * a + self.buf[i] * inv) // 255
                self.buf[i + 1] = (rgba[1] * a + self.buf[i + 1] * inv) // 255
                self.buf[i + 2] = (rgba[2] * a + self.buf[i + 2] * inv) // 255
                self.buf[i + 3] = min(255, self.buf[i + 3] + a)

    def rect(self, x0: int, y0: int, rw: int, rh: int, rgba: tuple[int, int, int, int]) -> None:
        for yy in range(max(0, y0), min(self.h, y0 + rh)):
            for xx in range(max(0, x0), min(self.w, x0 + rw)):
                self.px(xx, yy, rgba)

    def circle(self, cx: int, cy: int, r: int, rgba: tuple[int, int, int, int]) -> None:
        r2 = r * r
        for yy in range(max(0, cy - r), min(self.h, cy + r + 1)):
            dy = yy - cy
            for xx in range(max(0, cx - r), min(self.w, cx + r + 1)):
                dx = xx - cx
                if dx * dx + dy * dy <= r2:
                    self.px(xx, yy, rgba)

    def line(self, x0: int, y0: int, x1: int, y1: int, rgba: tuple[int, int, int, int], thickness: int = 1) -> None:
        steps = max(abs(x1 - x0), abs(y1 - y0), 1)
        for s in range(steps + 1):
            t = s / steps
            x = int(round(x0 + (x1 - x0) * t))
            y = int(round(y0 + (y1 - y0) * t))
            if thickness <= 1:
                self.px(x, y, rgba)
            else:
                self.rect(x - thickness // 2, y - thickness // 2, thickness, thickness, rgba)


# ── HERO SPRITE SHEETS (12 rows x 4 cols, each frame 48x48) ─────────────────
# Rows: 0=idle, 1=walk, 2=run, 3=sprint, 4=jump, 5=dash,
#       6=attack, 7=heavy, 8=skill, 9=ultimate, 10=hurt, 11=death
def gen_hero_sheet(
    filename: str,
    hair_col: tuple,
    tunic_col: tuple,
    cape_col: tuple,
    accent_col: tuple,
    weapon_kind: str,
) -> None:
    FW, FH = 48, 48
    COLS, ROWS = 4, 12
    c = Canvas(FW * COLS, FH * ROWS)
    skin = (248, 212, 182, 255)
    outline = (24, 20, 32, 255)
    dark_pants = (45, 48, 68, 255)
    boots = (72, 48, 36, 255)
    steel = (205, 220, 238, 255)

    for row in range(ROWS):
        for col in range(COLS):
            ox = col * FW
            oy = row * FH

            if row == 11:
                drop = min(col * 4, 10)
                if col < 3:
                    c.rect(ox + 14, oy + 26 + drop, 20, 10, tunic_col)
                    c.circle(ox + 14, oy + 28 + drop, 6, skin)
                    c.circle(ox + 13, oy + 25 + drop, 6, hair_col)
                else:
                    c.rect(ox + 10, oy + 36, 28, 7, cape_col)
                    c.circle(ox + 14, oy + 36, 5, hair_col)
                continue

            bob = 1 if (row == 0 and col in (1, 2)) else 0
            if row in (1, 2, 3):
                bob = -2 if (col % 2 == 1) else 0
            elif row == 4:
                bob = -6 if col in (1, 2) else -2
            elif row == 5:
                bob = -2

            cx = ox + 24 + (4 if row == 5 else 0)
            cy = oy + 24 + bob

            body_c = (255, 160, 160, 255) if (row == 10 and col % 2 == 0) else tunic_col
            cape_c = (255, 190, 190, 255) if (row == 10 and col % 2 == 0) else cape_col

            if row in (8, 9):
                ar = 18 if row == 8 else 22
                c.circle(cx, cy + 2, ar, (accent_col[0], accent_col[1], accent_col[2], 70 + (col * 25) % 80))

            cape_sway = (col - 1) * 2 if row in (2, 3, 5) else (col % 2)
            c.rect(cx - 10 - cape_sway, cy - 2, 14, 17, cape_c)

            stride = 0
            if row == 1:
                stride = (-3, 0, 3, 0)[col]
            elif row in (2, 3):
                stride = (-5, 1, 5, -1)[col]
            elif row == 4:
                stride = -2

            c.rect(cx - 6 + stride, cy + 9, 5, 8, dark_pants)
            c.rect(cx + 1 - stride, cy + 9, 5, 8, dark_pants)
            c.rect(cx - 6 + stride, cy + 15, 5, 3, boots)
            c.rect(cx + 1 - stride, cy + 15, 5, 3, boots)

            c.rect(cx - 8, cy - 3, 16, 13, outline)
            c.rect(cx - 7, cy - 2, 14, 11, body_c)
            c.rect(cx - 7, cy + 5, 14, 2, accent_col)

            c.circle(cx, cy - 10, 8, outline)
            c.circle(cx, cy - 10, 7, skin)
            c.rect(cx + 1, cy - 11, 2, 3, (30, 30, 60, 255))
            c.rect(cx + 5, cy - 11, 2, 3, (30, 30, 60, 255))
            c.px(cx + 1, cy - 11, (255, 255, 255, 255))
            c.px(cx + 5, cy - 11, (255, 255, 255, 255))
            c.circle(cx - 1, cy - 14, 7, hair_col)
            c.rect(cx - 8, cy - 15, 4, 8, hair_col)
            c.rect(cx - 4, cy - 18, 9, 4, hair_col)
            c.px(cx + 5, cy - 14, hair_col)

            swing = row in (6, 7, 8, 9)
            if weapon_kind == "sword":
                if swing:
                    ang = (-0.9, -0.2, 0.5, 1.0)[col]
                    wx = cx + int(math.cos(ang) * 16)
                    wy = cy + int(math.sin(ang) * 16)
                    c.line(cx + 5, cy + 2, wx, wy, steel, 3)
                    c.line(cx + 6, cy + 2, wx, wy, accent_col, 1)
                else:
                    c.line(cx + 7, cy + 4, cx + 14, cy - 9, steel, 2)
                    c.rect(cx + 6, cy + 3, 4, 2, accent_col)
            elif weapon_kind == "bow":
                bx = cx + 8
                c.line(bx, cy - 9, bx + 4, cy - 2, (160, 100, 50, 255), 2)
                c.line(bx + 4, cy - 2, bx, cy + 7, (160, 100, 50, 255), 2)
                c.line(bx, cy - 8, bx, cy + 6, (230, 240, 255, 255), 1)
                if swing:
                    c.line(bx + 2, cy - 1, bx + 15, cy - 1, accent_col, 2)
            elif weapon_kind == "staff":
                sx = cx + 8
                c.line(sx, cy + 12, sx + (4 if swing else 0), cy - 12, (120, 75, 45, 255), 2)
                c.circle(sx + (4 if swing else 0), cy - 13, 4, accent_col)
                c.circle(sx + (4 if swing else 0), cy - 13, 2, (255, 255, 255, 255))
            elif weapon_kind == "hammer":
                if swing:
                    hx = cx + 14
                    hy = cy + (col * 4 - 4)
                    c.line(cx + 4, cy + 2, hx, hy, (110, 70, 40, 255), 2)
                    c.rect(hx - 4, hy - 5, 9, 10, (120, 125, 140, 255))
                    c.rect(hx - 3, hy - 4, 7, 3, accent_col)
                else:
                    c.line(cx + 7, cy + 8, cx + 11, cy - 8, (110, 70, 40, 255), 2)
                    c.rect(cx + 7, cy - 12, 9, 7, (120, 125, 140, 255))

    save_png(f"{ASSETS}/sprites/player/{filename}", FW * COLS, FH * ROWS, c.buf)


# ── ENEMY SPRITE SHEETS (4 frames x 5 rows: idle, move, attack, hurt, death) ─
def gen_enemy_sheet(filename: str, kind: str, size: int = 48) -> None:
    COLS, ROWS = 4, 5
    c = Canvas(size * COLS, size * ROWS)

    for row in range(ROWS):
        for col in range(COLS):
            ox = col * size
            oy = row * size
            cx = ox + size // 2
            cy = oy + size // 2
            bob = -2 if (row == 1 and col % 2 == 1) else (1 if (row == 0 and col in (1, 2)) else 0)
            cy += bob

            if row == 4:
                alpha = max(40, 220 - col * 55)
                c.rect(cx - size // 4, oy + size - 12, size // 2, 6, (160, 160, 180, alpha))
                continue

            hurt_tint = row == 3 and col % 2 == 0

            if kind == "slime":
                col_body = (255, 130, 130, 255) if hurt_tint else (55, 165, 235, 235)
                sq = 3 if col % 2 == 1 else 0
                c.circle(cx, cy + 4 + sq // 2, size // 3 - 2, (20, 60, 110, 255))
                c.circle(cx, cy + 4 + sq // 2, size // 3 - 4, col_body)
                c.circle(cx - 5, cy - 2, 3, (190, 240, 255, 220))
                c.rect(cx - 6, cy + 3, 3, 4, (20, 20, 35, 255))
                c.rect(cx + 3, cy + 3, 3, 4, (20, 20, 35, 255))
            elif kind == "goblin":
                skin = (255, 150, 150, 255) if hurt_tint else (95, 185, 72, 255)
                c.rect(cx - 7, cy, 14, 13, (120, 80, 48, 255))
                c.circle(cx, cy - 6, 7, skin)
                c.line(cx - 7, cy - 8, cx - 13, cy - 12, skin, 2)
                c.line(cx + 7, cy - 8, cx + 13, cy - 12, skin, 2)
                c.rect(cx - 4, cy - 7, 2, 2, (255, 60, 40, 255))
                c.rect(cx + 2, cy - 7, 2, 2, (255, 60, 40, 255))
                if row == 2:
                    c.line(cx + 8, cy + 2, cx + 16, cy - 6, (210, 210, 220, 255), 2)
            elif kind == "skeleton":
                bone = (255, 160, 160, 255) if hurt_tint else (232, 228, 212, 255)
                c.rect(cx - 6, cy - 1, 12, 14, (65, 60, 70, 255))
                for r in range(3):
                    c.rect(cx - 5, cy + 1 + r * 4, 10, 2, bone)
                c.circle(cx, cy - 8, 7, bone)
                c.rect(cx - 4, cy - 9, 3, 3, (255, 60, 50, 255))
                c.rect(cx + 1, cy - 9, 3, 3, (255, 60, 50, 255))
                if row == 2:
                    c.line(cx + 7, cy + 3, cx + 16, cy - 8, (200, 215, 230, 255), 2)
            elif kind == "archer_enemy":
                robe = (255, 150, 150, 255) if hurt_tint else (130, 55, 85, 255)
                c.rect(cx - 7, cy - 2, 14, 15, robe)
                c.circle(cx, cy - 9, 6, (190, 160, 140, 255))
                c.circle(cx, cy - 12, 7, (70, 40, 55, 255))
                c.line(cx + 8, cy - 8, cx + 11, cy + 6, (150, 95, 50, 255), 2)
            elif kind == "flying_monster":
                wing_col = (255, 140, 140, 255) if hurt_tint else (110, 55, 145, 255)
                flap = (-6, -2, 6, 2)[col]
                c.line(cx - 4, cy, cx - 18, cy + flap, wing_col, 4)
                c.line(cx + 4, cy, cx + 18, cy + flap, wing_col, 4)
                c.circle(cx, cy, 7, (70, 35, 95, 255))
                c.rect(cx - 4, cy - 2, 2, 2, (255, 220, 60, 255))
                c.rect(cx + 2, cy - 2, 2, 2, (255, 220, 60, 255))
            elif kind == "dark_knight":
                armor = (255, 140, 140, 255) if hurt_tint else (48, 36, 78, 255)
                trim = (180, 75, 220, 255)
                c.rect(cx - 10, cy - 4, 20, 19, armor)
                c.rect(cx - 10, cy + 2, 20, 3, trim)
                c.circle(cx, cy - 11, 8, armor)
                c.line(cx - 6, cy - 16, cx - 11, cy - 22, trim, 2)
                c.line(cx + 6, cy - 16, cx + 11, cy - 22, trim, 2)
                c.rect(cx - 4, cy - 12, 8, 2, (255, 70, 50, 255))
                c.line(cx + 12, cy + 8, cx + 16, cy - 14, (200, 180, 230, 255), 3)
            elif kind == "elite_golem":
                stone = (255, 140, 140, 255) if hurt_tint else (115, 110, 105, 255)
                rune = (80, 220, 255, 255)
                c.rect(cx - 13, cy - 6, 26, 22, stone)
                c.rect(cx - 9, cy - 16, 18, 11, stone)
                c.rect(cx - 5, cy - 12, 10, 3, rune)
                c.circle(cx, cy + 3, 4, rune)
            elif kind == "shadow_lord":
                cloak = (255, 130, 130, 255) if hurt_tint else (68, 20, 95, 255)
                aura = (195, 65, 255, 85)
                c.circle(cx, cy, size // 2 - 4, aura)
                c.rect(cx - 16, cy - 8, 32, 28, cloak)
                c.rect(cx - 16, cy + 4, 32, 4, (235, 185, 60, 255))
                c.circle(cx, cy - 16, 12, (32, 16, 48, 255))
                for sp in (-9, -3, 3, 9):
                    c.line(cx + sp, cy - 24, cx + sp, cy - 33, (240, 190, 55, 255), 3)
                c.rect(cx - 6, cy - 17, 4, 3, (255, 70, 50, 255))
                c.rect(cx + 2, cy - 17, 4, 3, (255, 70, 50, 255))
                c.line(cx + 18, cy + 12, cx + 22, cy - 22, (205, 90, 255, 255), 4)

    save_png(f"{ASSETS}/sprites/enemies/{filename}", size * COLS, size * ROWS, c.buf)


# ── TILESET (8 cols x 4 rows of 32x32 tiles) ─────────────────────────────────
def gen_tileset() -> None:
    T = 32
    COLS, ROWS = 8, 4
    c = Canvas(T * COLS, T * ROWS)
    rng = random.Random(42)

    def draw_tile(idx: int, base: tuple, accent: tuple, kind: str) -> None:
        tx = (idx % COLS) * T
        ty = (idx // COLS) * T
        c.rect(tx, ty, T, T, base)
        if kind == "grass":
            for _ in range(18):
                gx = tx + rng.randint(1, T - 3)
                gy = ty + rng.randint(1, T - 4)
                c.px(gx, gy, accent)
                c.px(gx, gy + 1, accent)
        elif kind == "water":
            for wy in (6, 15, 24):
                c.line(tx + 3, ty + wy, tx + 13, ty + wy, accent, 2)
                c.line(tx + 18, ty + wy + 2, tx + 28, ty + wy + 2, accent, 2)
        elif kind == "brick":
            c.rect(tx, ty + 15, T, 2, accent)
            c.rect(tx + 15, ty, 2, 16, accent)
            c.rect(tx + 7, ty + 16, 2, 16, accent)
            c.rect(tx + 23, ty + 16, 2, 16, accent)
        elif kind == "tree":
            c.rect(tx + 12, ty + 20, 8, 12, (105, 68, 38, 255))
            c.circle(tx + 16, ty + 13, 12, (28, 92, 38, 255))
            c.circle(tx + 14, ty + 11, 9, accent)
        elif kind == "rock":
            c.circle(tx + 16, ty + 18, 11, (75, 78, 86, 255))
            c.circle(tx + 14, ty + 15, 8, accent)
        elif kind == "chest_closed":
            c.rect(tx + 5, ty + 9, 22, 16, (165, 112, 32, 255))
            c.rect(tx + 5, ty + 14, 22, 3, (235, 195, 55, 255))
            c.rect(tx + 14, ty + 13, 4, 5, (255, 235, 120, 255))
        elif kind == "chest_open":
            c.rect(tx + 5, ty + 14, 22, 11, (145, 95, 25, 255))
            c.rect(tx + 5, ty + 7, 22, 5, (90, 58, 20, 255))
        elif kind == "waypoint":
            c.circle(tx + 16, ty + 16, 12, (40, 140, 175, 200))
            c.circle(tx + 16, ty + 16, 7, accent)
            c.circle(tx + 16, ty + 16, 3, (255, 255, 255, 255))
        elif kind == "dungeon":
            c.circle(tx + 16, ty + 16, 13, (40, 15, 60, 255))
            c.circle(tx + 16, ty + 16, 9, accent)
            c.circle(tx + 16, ty + 16, 4, (15, 5, 25, 255))
        elif kind == "herb":
            c.line(tx + 16, ty + 24, tx + 11, ty + 12, accent, 2)
            c.line(tx + 16, ty + 24, tx + 16, ty + 9, accent, 2)
            c.line(tx + 16, ty + 24, tx + 21, ty + 13, accent, 2)
            c.circle(tx + 16, ty + 9, 2, (240, 235, 110, 255))
        elif kind == "waterfall":
            for wx in range(4, 29, 5):
                c.line(tx + wx, ty + 2, tx + wx, ty + 30, accent, 2)
        elif kind == "cliff":
            c.rect(tx, ty + 22, T, 10, accent)
            for cx in range(4, 29, 7):
                c.rect(tx + cx, ty + 4, 2, 18, accent)

    draw_tile(0, (66, 148, 68, 255), (92, 178, 84, 255), "grass")
    draw_tile(1, (138, 102, 66, 255), (160, 122, 82, 255), "grass")
    draw_tile(2, (118, 122, 130, 255), (86, 90, 98, 255), "brick")
    draw_tile(3, (42, 108, 198, 255), (130, 195, 250, 255), "water")
    draw_tile(4, (218, 198, 124, 255), (195, 172, 102, 255), "grass")
    draw_tile(5, (224, 238, 250, 255), (190, 214, 235, 255), "grass")
    draw_tile(6, (212, 74, 24, 255), (255, 175, 45, 255), "water")
    draw_tile(7, (66, 148, 68, 255), (48, 136, 56, 255), "tree")
    draw_tile(8, (66, 148, 68, 255), (122, 126, 136, 255), "rock")
    draw_tile(9, (74, 64, 72, 255), (48, 42, 48, 255), "brick")
    draw_tile(10, (66, 148, 68, 255), (235, 195, 55, 255), "chest_closed")
    draw_tile(11, (66, 148, 68, 255), (90, 245, 210, 255), "waypoint")
    draw_tile(12, (74, 64, 72, 255), (155, 55, 220, 255), "dungeon")
    draw_tile(13, (66, 148, 68, 255), (75, 225, 120, 255), "herb")
    draw_tile(14, (58, 132, 225, 255), (220, 245, 255, 255), "waterfall")
    draw_tile(15, (66, 148, 68, 255), (145, 95, 25, 255), "chest_open")
    draw_tile(16, (158, 114, 68, 255), (120, 82, 46, 255), "brick")
    draw_tile(17, (112, 92, 76, 255), (76, 60, 48, 255), "cliff")

    save_png(f"{ASSETS}/sprites/world/tileset.png", T * COLS, T * ROWS, c.buf)


# ── EFFECTS / ICONS / SPLASH ─────────────────────────────────────────────────
def gen_misc() -> None:
    c = Canvas(64, 64)
    for y in range(64):
        for x in range(64):
            d = math.hypot(x - 32, y - 32)
            a = int(255 * (max(0.0, 1.0 - d / 32.0) ** 1.6))
            c.px(x, y, (255, 255, 255, a))
    save_png(f"{ASSETS}/sprites/effects/particles.png", 64, 64, c.buf)

    sw, sh = 640, 360
    sp = Canvas(sw, sh)
    for y in range(sh):
        t = y / sh
        sp.rect(0, y, sw, 1, (int(10 + 18 * t), int(14 + 36 * t), int(38 + 68 * t), 255))
    sp.circle(sw // 2, sh // 2, 110, (70, 160, 255, 45))
    sp.circle(sw // 2, sh // 2, 65, (140, 220, 255, 65))
    save_png(f"{ASSETS}/sprites/ui/splash.png", sw, sh, sp.buf)

    bg = Canvas(320, 180)
    for y in range(180):
        t = y / 180
        bg.rect(0, y, 320, 1, (int(18 + 30 * t), int(28 + 45 * t), int(64 + 80 * t), 255))
    save_png(f"{ASSETS}/sprites/world/background.png", 320, 180, bg.buf)

    hud = Canvas(256, 64)
    hud.rect(0, 0, 256, 64, (14, 18, 34, 200))
    hud.rect(0, 0, 256, 2, (95, 165, 240, 220))
    hud.rect(0, 62, 256, 2, (95, 165, 240, 220))
    save_png(f"{ASSETS}/sprites/ui/hud.png", 256, 64, hud.buf)

    for density, size in [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]:
        ic = Canvas(size, size)
        for y in range(size):
            t = y / size
            ic.rect(0, y, size, 1, (int(16 + 24 * t), int(26 + 50 * t), int(68 + 100 * t), 255))
        ic.circle(size // 2, size // 2, int(size * 0.36), (60, 180, 255, 70))
        pad = size // 4
        thick = max(3, size // 12)
        ic.rect(pad, pad, thick, size - 2 * pad, (130, 230, 255, 255))
        ic.rect(pad, pad, size - 2 * pad, thick, (130, 230, 255, 255))
        ic.rect(pad, size // 2 - thick // 2, int(size * 0.42), thick, (130, 230, 255, 255))
        ic.rect(pad, size - pad - thick, size - 2 * pad, thick, (130, 230, 255, 255))
        save_png(f"{ANDROID_RES}/mipmap-{density}/ic_launcher.png", size, size, ic.buf)
        save_png(f"{ANDROID_RES}/mipmap-{density}/ic_launcher_round.png", size, size, ic.buf)


# ── SYNTHESIZED WAV AUDIO (SFX + MUSIC) ─────────────────────────────────────
def write_wav(path: str, samples: list[float], sample_rate: int = 22050) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = bytearray()
    for s in samples:
        v = max(-32767, min(32767, int(s * 32767)))
        pcm.extend(struct.pack("<h", v))
    with wave.open(path, "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(sample_rate)
        wf.writeframes(bytes(pcm))


def synth_tone(freq_start: float, freq_end: float, dur: float, wave_type: str = "sine", vol: float = 0.35) -> list[float]:
    sr = 22050
    n = int(sr * dur)
    out = []
    phase = 0.0
    rng = random.Random(1)
    for i in range(n):
        t = i / n
        f = freq_start + (freq_end - freq_start) * t
        phase += 2 * math.pi * f / sr
        env = (1.0 - t) ** 1.4
        if wave_type == "sine":
            val = math.sin(phase)
        elif wave_type == "square":
            val = 1.0 if math.sin(phase) >= 0 else -1.0
        elif wave_type == "saw":
            val = 2.0 * ((phase / (2 * math.pi)) % 1.0) - 1.0
        else:
            val = rng.uniform(-1.0, 1.0) * 0.6 + math.sin(phase) * 0.4
        out.append(val * env * vol)
    return out


def gen_audio() -> None:
    sfx_dir = f"{ASSETS}/sounds/sfx"
    mus_dir = f"{ASSETS}/sounds/music"

    write_wav(f"{sfx_dir}/attack_sword.wav", synth_tone(620, 180, 0.14, "noise", 0.35))
    write_wav(f"{sfx_dir}/attack_bow.wav", synth_tone(920, 340, 0.12, "sine", 0.32))
    write_wav(f"{sfx_dir}/attack_magic.wav", synth_tone(520, 1180, 0.22, "sine", 0.32))
    write_wav(f"{sfx_dir}/attack_heavy.wav", synth_tone(240, 65, 0.28, "saw", 0.42))
    write_wav(f"{sfx_dir}/skill.wav", synth_tone(380, 880, 0.35, "saw", 0.35))
    write_wav(f"{sfx_dir}/ultimate.wav", synth_tone(180, 960, 0.55, "saw", 0.45))
    write_wav(f"{sfx_dir}/enemy_hit.wav", synth_tone(280, 110, 0.10, "square", 0.28))
    write_wav(f"{sfx_dir}/enemy_death.wav", synth_tone(320, 55, 0.30, "noise", 0.35))
    write_wav(f"{sfx_dir}/player_hurt.wav", synth_tone(220, 95, 0.18, "square", 0.35))
    write_wav(f"{sfx_dir}/chest_open.wav", synth_tone(440, 880, 0.32, "sine", 0.38))
    write_wav(f"{sfx_dir}/level_up.wav",
              synth_tone(523, 523, 0.12, "sine", 0.35)
              + synth_tone(659, 659, 0.12, "sine", 0.35)
              + synth_tone(784, 1046, 0.28, "sine", 0.38))
    write_wav(f"{sfx_dir}/footstep.wav", synth_tone(140, 70, 0.06, "noise", 0.18))
    write_wav(f"{sfx_dir}/dash.wav", synth_tone(480, 190, 0.13, "noise", 0.26))
    write_wav(f"{sfx_dir}/jump.wav", synth_tone(260, 520, 0.14, "sine", 0.26))
    write_wav(f"{sfx_dir}/ui_click.wav", synth_tone(700, 900, 0.06, "sine", 0.24))
    write_wav(f"{sfx_dir}/puzzle_solve.wav",
              synth_tone(440, 440, 0.1, "sine", 0.35) + synth_tone(660, 880, 0.25, "sine", 0.38))

    sr = 22050
    notes = [261.63, 329.63, 392.00, 493.88, 392.00, 329.63, 293.66, 349.23, 440.00, 523.25, 440.00, 349.23]
    exp = []
    for f in notes:
        n = int(sr * 0.5)
        for i in range(n):
            t = i / n
            env = math.sin(math.pi * t) ** 0.6
            val = math.sin(2 * math.pi * f * i / sr) * 0.7 + math.sin(2 * math.pi * f * 0.5 * i / sr) * 0.3
            exp.append(val * env * 0.18)
    write_wav(f"{mus_dir}/exploration.wav", exp, sr)

    bnotes = [146.83, 146.83, 174.61, 164.81, 146.83, 196.00, 220.00, 174.61]
    boss = []
    for f in bnotes:
        n = int(sr * 0.5)
        for i in range(n):
            t = i / n
            env = 1.0 - 0.5 * t
            val = math.sin(2 * math.pi * f * i / sr) * 0.6 + math.sin(2 * math.pi * f * 2 * i / sr) * 0.25
            boss.append(val * env * 0.22)
    write_wav(f"{mus_dir}/boss_battle.wav", boss, sr)


def main() -> None:
    print("Generating ETHERIA sprite sheets, tileset, icons, and audio...")
    gen_hero_sheet("kai.png", (215, 72, 32, 255), (210, 62, 42, 255), (125, 32, 28, 255), (255, 165, 55, 255), "sword")
    gen_hero_sheet("lyra.png", (240, 215, 95, 255), (56, 158, 92, 255), (34, 96, 56, 255), (140, 255, 160, 255), "bow")
    gen_hero_sheet("zara.png", (115, 165, 250, 255), (64, 108, 198, 255), (38, 62, 136, 255), (120, 235, 255, 255), "staff")
    gen_hero_sheet("ragna.png", (62, 54, 52, 255), (156, 104, 52, 255), (96, 62, 32, 255), (235, 185, 85, 255), "hammer")

    gen_enemy_sheet("slime.png", "slime", 48)
    gen_enemy_sheet("goblin.png", "goblin", 48)
    gen_enemy_sheet("skeleton.png", "skeleton", 48)
    gen_enemy_sheet("archer_enemy.png", "archer_enemy", 48)
    gen_enemy_sheet("flying_monster.png", "flying_monster", 48)
    gen_enemy_sheet("darkKnight.png", "dark_knight", 56)
    gen_enemy_sheet("elite_golem.png", "elite_golem", 56)
    gen_enemy_sheet("shadow_lord.png", "shadow_lord", 72)

    gen_tileset()
    gen_misc()
    gen_audio()
    print("Asset generation complete!")


if __name__ == "__main__":
    main()
