#!/usr/bin/env python3
"""Generates the magic carpet entity textures, item textures and item models.

The UV layout mirrors MagicCarpetEntityModel.createBodyLayer (96x48 texture units, drawn at 2x) and the item models
are flat copies of the entity geometry; change both together. Standard library only.

    python3 tools/generate_carpet_assets.py
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/magic_carpet"

TEX_W, TEX_H = 96, 48  # texture units, as declared by the entity model
SCALE = 2  # art pixels per texture unit
SEGMENTS = 8
TASSEL_X = [-6, -3, 0, 3, 6]


def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, factor):
    return tuple(max(0, min(255, round(c * factor))) for c in color[:3]) + (color[3],)


PALETTES = {
    "basic": {
        "dark": rgb("#300c1a"),
        "field": [rgb(c) for c in ("#611022", "#621123", "#631224", "#641325", "#651426", "#661527", "#671628", "#681729")],
        "trim": rgb("#e8b344"),
        "trim_shade": rgb("#a1671e"),
        "highlight": rgb("#ffe590"),
        "accent": rgb("#e45153"),
        "accent2": rgb("#a1671e"),
        # plain rolled wool edge with dark stitches
        "cord": "plain",
        "edge": [rgb("#4f0b1b"), rgb("#5a0e1f")],
        "edge_line": rgb("#300c1a"),
    },
    "advanced": {
        "dark": rgb("#0c1426"),
        "field": [rgb(c) for c in ("#132434", "#142535", "#152636", "#162737", "#172838", "#182939", "#192a3a", "#1a2b3b")],
        "trim": rgb("#f1913f"),
        "trim_shade": rgb("#994a1d"),
        "highlight": rgb("#9eecf7"),
        "accent": rgb("#9eecf7"),
        "accent2": rgb("#35a3c2"),
        # twisted gold cord
        "cord": "twist",
        "edge": [rgb("#e8b344"), rgb("#a1671e")],
        "edge_line": rgb("#ffe590"),
        "tassel": [rgb("#e8b344"), rgb("#a1671e")],
        "tassel_tip": rgb("#ffe590"),
    },
    "legendary": {
        "dark": rgb("#200a40"),
        "field": [rgb(c) for c in ("#3d136c", "#3e146d", "#3f156e", "#40166f", "#411770", "#421871", "#431972", "#441a73")],
        "trim": rgb("#b6e259"),
        "trim_shade": rgb("#517e1e"),
        "highlight": rgb("#fae1ff"),
        "accent": rgb("#fae1ff"),
        "accent2": rgb("#b270f4"),
        # dark cord with a glowing thread
        "cord": "thread",
        "edge": [rgb("#200a40"), rgb("#2d1052")],
        "edge_line": rgb("#b6e259"),
        "tassel": [rgb("#b6e259"), rgb("#517e1e")],
        "tassel_tip": rgb("#fae1ff"),
    },
}
TASSELS = {"basic": False, "advanced": True, "legendary": True}
CURLS = {"basic": False, "advanced": False, "legendary": True}
GLOW = {"basic": False, "advanced": False, "legendary": True}

CLEAR = (0, 0, 0, 0)


def noise(x, z):
    h = (x * 374761393 + z * 668265263) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return h ^ (h >> 16)


# --- the 24x32 top surface design, in art pixels (48x64); z = 0 is the front edge -------------------------------

DESIGN_W, DESIGN_L = 24 * SCALE, 32 * SCALE


def design(p, x, z):
    """Colour of the carpet's top surface at art pixel (x, z)."""
    # outermost two texture units sit under the raised border (or form the legendary corner flaps)
    edge = min(x, DESIGN_W - 1 - x, z, DESIGN_L - 1 - z)
    if edge < 4:
        if edge == 0:
            return p["dark"]
        return p["trim"] if (x + z) % 6 == 0 else p["trim_shade"]

    # border band just inside the raised edge
    ring = edge - 4
    along = x if min(z, DESIGN_L - 1 - z) - 4 == ring else z
    if ring == 0 or ring == 3:
        return p["dark"]
    if ring in (1, 2):
        return p["dark"] if along % 4 == 0 else p["trim"]
    if ring == 6:
        return p["trim_shade"] if along % 2 == 0 else field(p, x, z)

    cx, cz = (DESIGN_W - 1) / 2, (DESIGN_L - 1) / 2
    dx, dz = abs(x - cx), abs(z - cz)

    # central medallion: nested diamonds with dotted rays
    d = dx + dz
    if d <= 2:
        return p["highlight"]
    if d <= 4:
        return p["accent"]
    if d <= 6:
        return p["trim"]
    if d <= 7:
        return p["dark"]
    if d <= 9:
        return p["accent2"] if (x + z) % 2 else field(p, x, z)
    if 11 <= d <= 12:
        return p["trim"]
    if d == 13:
        return p["dark"]
    if (dx <= 0.5 or dz <= 0.5) and 14 <= d <= 19 and int(d) % 2 == 0:
        return p["trim"]

    # end motifs: a small diamond flanked by sparkles
    for mz in (14.5, DESIGN_L - 1 - 14.5):
        md = abs(x - cx) + abs(z - mz)
        if md <= 1:
            return p["highlight"]
        if md <= 3:
            return p["accent"]
        if md <= 4:
            return p["trim"]
        if md <= 5:
            return p["dark"]
        for sx in (cx - 11, cx + 11):
            if (abs(x - sx) <= 0.5 and abs(z - mz) <= 2.5) or (abs(z - mz) <= 0.5 and abs(x - sx) <= 2.5):
                return p["accent"] if abs(x - sx) + abs(z - mz) <= 1 else p["trim"]

    # scattered sparkles between the motifs
    for sx, sz in ((cx - 12, cz - 7), (cx + 12, cz + 7), (cx + 12, cz - 7), (cx - 12, cz + 7)):
        if abs(x - sx) + abs(z - sz) <= 0.5:
            return p["accent"]

    return field(p, x, z)


def field(p, x, z):
    # a faint horizontal weave
    variants = p["field"]
    base = (noise(x // 2, z) % len(variants))
    if z % 4 == 0:
        base = max(0, base - 3)
    return variants[base]


# --- painting boxes using Minecraft's box UV layout -------------------------------------------------------------

class Texture:
    def __init__(self):
        self.w, self.h = TEX_W * SCALE, TEX_H * SCALE
        self.pixels = [[CLEAR] * self.w for _ in range(self.h)]

    def fill(self, u, v, w, h, colour_at):
        """Fills texture units [u, u+w) x [v, v+h); colour_at receives art-pixel offsets (col, row)."""
        for row in range(h * SCALE):
            for col in range(w * SCALE):
                self.pixels[v * SCALE + row][u * SCALE + col] = colour_at(col, row)

    def box(self, u, v, w, h, d, up, down=None, sides=None):
        """
        Paints a box's faces. `up`/`down` receive (x, z) art pixels in model space measured from the box's min corner,
        so z = 0 is the front (-Z) edge. `sides` receives (col, row) of each side strip.
        """
        down = down or (lambda x, z: shade(up(x, z), 0.6))
        sides = sides or (lambda col, row: shade(up(0, 0), 0.7))
        dd = d * SCALE
        # top row: DOWN at u+d, UP at u+d+w; row 0 is the box's back (+Z) edge
        self.fill(u + d, v, w, d, lambda col, row: down(col, dd - 1 - row))
        self.fill(u + d + w, v, w, d, lambda col, row: up(col, dd - 1 - row))
        # side row: WEST, NORTH, EAST, SOUTH
        self.fill(u, v + d, 2 * (d + w), h, sides)

    def png(self):
        raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in self.pixels)

        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

        header = struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0)
        return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")


def cord(p, along, across, width):
    """A raised border strip: `along` runs the length of the strip, `across` its width, both in art pixels."""
    style = p["cord"]
    if style == "twist":
        return p["edge"][((along + across) // 2) % 2]
    centre = across in (width // 2 - 1, width // 2)
    if style == "thread" and centre:
        return p["edge_line"]
    if style == "plain" and centre and along % 4 == 0:
        return p["edge_line"]
    return p["edge"][(along // 2 + across // 2) % 2]


def paint(tier):
    p = PALETTES[tier]
    tex = Texture()

    # carpet segments: 24x1x4 at (0, 5i); segment i covers design rows [8i, 8i + 8)
    for i in range(SEGMENTS):
        tex.box(0, 5 * i, 24, 1, 4,
                up=lambda x, z, i=i: design(p, x, 8 * i + z),
                sides=lambda col, row: p["dark"] if row == 0 else p["field"][0])

    # raised side borders: 2x1x4 at (56, 5i), running along z
    for i in range(SEGMENTS):
        tex.box(56, 5 * i, 2, 1, 4,
                up=lambda x, z, i=i: cord(p, 8 * i + z, x, 4),
                sides=lambda col, row: shade(p["edge"][0], 0.8))

    # raised front/back borders: 20x1x2 at (0, 40), and the legendary front border 16x1x2 at (0, 43)
    tex.box(0, 40, 20, 1, 2, up=lambda x, z: cord(p, x, z, 4), sides=lambda col, row: shade(p["edge"][0], 0.8))
    tex.box(0, 43, 16, 1, 2, up=lambda x, z: cord(p, x, z, 4), sides=lambda col, row: shade(p["edge"][0], 0.8))

    # the legendary front segment without its corners: 16x1x4 at (56, 40)
    tex.box(56, 40, 16, 1, 4, up=lambda x, z: design(p, 8 + x, z),
            sides=lambda col, row: p["dark"] if row == 0 else p["field"][0])

    if TASSELS[tier]:
        # 1x1x3 at (44, 40): striped cord with pale tips on the end faces
        def tassel_side(col, row):
            # WEST [0,6) NORTH [6,8) EAST [8,14) SOUTH [14,16) art pixels
            if 6 <= col < 8 or col >= 14:
                return p["tassel_tip"]
            return p["tassel"][(col // 2) % 2]

        tex.box(44, 40, 1, 1, 3, up=lambda x, z: p["tassel"][(z // 2) % 2], sides=tassel_side)

    if CURLS[tier]:
        # corner flap 4x1x4 at (68, 0) shows the left front corner of the design; the right flap is mirrored
        def flap(x, z):
            # the raised border continues round the lifted corner as a flat cord
            if x < 4 or z < 4:
                return cord(p, z if x < 4 else x, min(x, z), 4)
            return design(p, x, z)

        tex.box(68, 0, 4, 1, 4, up=flap, sides=lambda col, row: p["edge"][0])
        # roll 4x2x2 at (68, 5): the rolled-up tip, banded with the glowing thread
        tex.box(68, 5, 4, 2, 2,
                up=lambda x, z: p["edge_line"] if x % 4 < 2 else p["edge"][0],
                down=lambda x, z: p["edge"][0],
                sides=lambda col, row: p["edge_line"] if col % 4 < 2 else p["edge"][1])

    return tex


def glow(tex, tier):
    p = PALETTES[tier]
    lit = {p["trim"], p["highlight"], p["accent"], p["edge_line"], p["tassel_tip"], p["tassel"][0]}
    out = Texture()
    out.pixels = [[px if px in lit else CLEAR for px in row] for row in tex.pixels]
    return out


# --- item models: the entity geometry laid flat, in block model coordinates ---------------------------------------

def uv_rect(u, v, w, h):
    return [u * 16 / TEX_W, v * 16 / TEX_H, (u + w) * 16 / TEX_W, (v + h) * 16 / TEX_H]


def element(name, frm, to, u, v, w, h, d, mirror=False, rotation=None):
    """A block model element whose faces match a model box of size (w, h, d) at texture offset (u, v)."""
    up = uv_rect(u + d + w, v, w, d)
    down = uv_rect(u + d, v, w, d)
    # UP/DOWN rows run back-to-front in the texture; block models expect north (front) at the top for "up"
    up = [up[0], up[3], up[2], up[1]]
    faces = {
        "up": up,
        "down": down,
        "north": uv_rect(u + d, v + d, w, h),
        "south": uv_rect(u + d + w + d, v + d, w, h),
        "west": uv_rect(u, v + d, d, h),
        "east": uv_rect(u + d + w, v + d, d, h),
    }
    if mirror:
        faces = {k: [r[2], r[1], r[0], r[3]] for k, r in faces.items()}
    result = {
        "name": name,
        "from": frm,
        "to": to,
        "faces": {k: {"uv": [round(c, 5) for c in r], "texture": "#0"} for k, r in faces.items()},
    }
    if rotation:
        result["rotation"] = rotation
    return result


def item_model(tier):
    # block coordinates = entity pixels + (8, 11, 8)
    ox, oy, oz = 8, 11, 8
    elements = []
    curls = CURLS[tier]
    for i in range(SEGMENTS):
        z0 = -16 + 4 * i + oz
        if i == 0 and curls:
            elements.append(element("segment_0", [-8 + ox, oy, z0], [8 + ox, oy + 1, z0 + 4], 56, 40, 16, 1, 4))
            elements.append(element("front_edge", [-8 + ox, oy + 1, z0], [8 + ox, oy + 2, z0 + 2], 0, 43, 16, 1, 2))
            for side, x0 in (("left", -12), ("right", 8)):
                rotation = {"angle": 22.5, "axis": "x", "origin": [(-8 if side == "left" else 8) + ox, oy, z0 + 4]}
                elements.append(element(f"{side}_curl", [x0 + ox, oy, z0], [x0 + 4 + ox, oy + 1, z0 + 4],
                                        68, 0, 4, 1, 4, mirror=side == "right", rotation=rotation))
                elements.append(element(f"{side}_curl_roll", [x0 + ox, oy, z0 - 1], [x0 + 4 + ox, oy + 2, z0 + 1],
                                        68, 5, 4, 2, 2, mirror=side == "right", rotation=rotation))
            continue
        elements.append(element(f"segment_{i}", [-12 + ox, oy, z0], [12 + ox, oy + 1, z0 + 4], 0, 5 * i, 24, 1, 4))
        for x0 in (-12, 10):
            elements.append(element(f"edge_{i}", [x0 + ox, oy + 1, z0], [x0 + 2 + ox, oy + 2, z0 + 4], 56, 5 * i, 2, 1, 4))
        if i == 0:
            elements.append(element("front_edge", [-10 + ox, oy + 1, z0], [10 + ox, oy + 2, z0 + 2], 0, 40, 20, 1, 2))
        if i == SEGMENTS - 1:
            elements.append(element("back_edge", [-10 + ox, oy + 1, z0 + 2], [10 + ox, oy + 2, z0 + 4], 0, 40, 20, 1, 2))
    if TASSELS[tier]:
        for x in TASSEL_X:
            x0 = x - 0.5 + ox
            elements.append(element("front_tassel", [x0, oy, -19 + oz], [x0 + 1, oy + 1, -16 + oz], 44, 40, 1, 1, 3))
            elements.append(element("back_tassel", [x0, oy, 16 + oz], [x0 + 1, oy + 1, 19 + oz], 44, 40, 1, 1, 3))

    texture = f"magic_carpet:item/{tier}_magic_carpet"
    return {
        "texture_size": [TEX_W, TEX_H],
        "textures": {"0": texture, "particle": texture},
        "elements": elements,
        "display": DISPLAY,
    }


DISPLAY = {
    "thirdperson_righthand": {"rotation": [90, 0, 0], "translation": [0, 3.5, -1], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"rotation": [90, 0, 0], "translation": [0, 3.5, -1], "scale": [0.5, 0.5, 0.5]},
    "firstperson_righthand": {"scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"scale": [0.5, 0.5, 0.5]},
    "ground": {"scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [30, -60, 0], "translation": [0, -1.5, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"translation": [0, 3, 0]},
    "fixed": {"rotation": [-90, 0, 0], "translation": [0, 0, 1], "scale": [0.5, 0.5, 0.5]},
}


def main():
    for tier in PALETTES:
        tex = paint(tier)
        png = tex.png()
        (ASSETS / f"textures/entity/{tier}_magic_carpet.png").write_bytes(png)
        (ASSETS / f"textures/item/{tier}_magic_carpet.png").write_bytes(png)
        if GLOW[tier]:
            (ASSETS / f"textures/entity/{tier}_magic_carpet_glow.png").write_bytes(glow(tex, tier).png())
        model = json.dumps(item_model(tier), indent="\t") + "\n"
        (ASSETS / f"models/item/{tier}_magic_carpet.json").write_text(model)


if __name__ == "__main__":
    main()
