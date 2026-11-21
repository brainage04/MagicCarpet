#!/usr/bin/env python3
"""Exports the magic carpet entity model to Blockbench .bbmodel files.

The model is built in code (MagicCarpetEntityModel.createBodyLayer) and animated in code
(MagicCarpetEntityModel.setupAnim), so this script repeats both: the cubes, pivots and UVs of
createBodyLayer, and setupAnim's arithmetic sampled every SAMPLE_TICKS ticks. The repetition is
checked by MagicCarpetBlockbenchExportTest, which bakes the Java model, runs setupAnim and compares
every cube corner against the .bbmodel it reads back; change the model and that test fails until
this script is rerun.

Coordinates. Blockbench's Java entity format draws the model mirrored in X and Y from Minecraft's
model space, so a Java point (x, y, z) is written as (-x, 24 - y, z). That map is a half turn about
Z, which conjugates the rotation Rz(zRot) Ry(yRot) Rx(xRot) that ModelPart.translateAndRotate
composes (ModelPart.java:163-168 in 26.3) into Rz(zRot) Ry(-yRot) Rx(-xRot): the x and y angles
negate and the z angle does not. Blockbench composes its ZYX Euler order the same way (three.js
ZYX is Rz Ry Rx, js/io/format.ts:717), so a Java rotation is written as (-xRot, -yRot, zRot). The
map preserves the geometry exactly.

    python3 tools/blockbench_export.py [--out DIR] [--repo-out DIR]
"""
import argparse
import json
import math
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "common/src/main/resources/assets/magic_carpet"
DEFAULT_OUT = Path.home() / "agent-work/magiccarpet-blockbench"

TEXTURE_WIDTH = 96
TEXTURE_HEIGHT = 48
SEGMENTS = 8
SEGMENT_LENGTH = 4.0
FRONT_Z = -16.0
TASSEL_X = [-6.0, -3.0, 0.0, 3.0, 6.0]
TASSEL_DROOP = 0.6
CURL_PITCH = 0.9
CURL_ROLL = 0.5
HOVER_HEIGHT = 1.0

# The clip is 4 seconds, sampled every 2 ticks. The motion repeats only every 628.3 ticks (31.4
# seconds) at both speeds: the ripple, the tassel flutter and the tassel sway run at 0.08 + 0.42 *
# speed, 0.12 + 0.6 * speed and 0.09 radians a tick, and their common period is not a whole number
# of ticks. A 4-second clip therefore cannot close the loop, so the animations play once rather than
# looping, which would snap the pose back every 4 seconds.
CYCLE_TICKS = 80
SAMPLE_TICKS = 2
TICKS_PER_SECOND = 20

ALL_SIDES = ("down", "up", "west", "north", "east", "south")
BORDER_SIDES = tuple(s for s in ALL_SIDES if s != "down")
BORDER_SIDES_BETWEEN = tuple(s for s in ALL_SIDES if s not in ("down", "west", "east"))
CURL_FLAP_SIDES = tuple(s for s in ALL_SIDES if s != "north")

# The tiers' model flags, from data/magic_carpet/magic_carpet/tier/<name>.json.
CARPETS = {
    "basic": {"tassels": False, "curls": False, "glow": False},
    "advanced": {"tassels": True, "curls": False, "glow": False},
    "legendary": {"tassels": True, "curls": True, "glow": True},
}

NAMESPACE = uuid.UUID("6f1a1d2e-0000-4000-8000-000000000000")


def stable_uuid(name):
    return str(uuid.uuid5(NAMESPACE, name))


# --- Minecraft's own arithmetic -------------------------------------------------------------

# Mth.sin reads a 65536-entry table (Mth.java:35-52 in 26.3), so the sampled values match the
# game's to the last bit rather than to a double-precision sine.
SIN_TABLE = [math.sin(i / 10430.378350470453) for i in range(65536)]


def mth_sin(value):
    return SIN_TABLE[int(value * 10430.378350470453) & 65535]


def mth_lerp(alpha, p0, p1):
    return p0 + alpha * (p1 - p0)


# --- The model ------------------------------------------------------------------------------


def box_uvs(width, height, depth, u, v, mirror):
    """Blockbench's box UV rectangles for a cube of this size at texture offset (u, v).

    This is Blockbench's own layout (js/outliner/types/cube.js, updateUV), which is mirrored from
    ModelPart.Cube's: its east rectangle is Java's west and its up is Java's down, the same mirror
    the coordinates get. The up and down rectangles run backwards, which is how Blockbench writes
    them, and a mirrored cube flips every rectangle's U and swaps east with west.
    """
    w, h, d = width, height, depth
    rects = {
        "east": (0, d, d, d + h),
        "west": (d + w, d, 2 * d + w, d + h),
        "up": (d + w, d, d, 0),
        "down": (d + 2 * w, 0, d + w, d),
        "south": (2 * d + w, d, 2 * d + 2 * w, d + h),
        "north": (d, d, d + w, d + h),
    }
    if mirror:
        flipped = {}
        for face, (x0, y0, x1, y1) in rects.items():
            flipped[face] = (x1, y0, x0, y1)
        rects["east"], rects["west"] = flipped["west"], flipped["east"]
        for face in ("up", "down", "south", "north"):
            rects[face] = flipped[face]
    return {face: [u + r[0], v + r[1], u + r[2], v + r[3]] for face, r in rects.items()}


class Cube:
    def __init__(self, name, tex, x, y, z, w, h, d, sides=ALL_SIDES, mirror=False):
        self.name = name
        self.tex = tex
        self.x, self.y, self.z = x, y, z
        self.w, self.h, self.d = w, h, d
        self.sides = sides
        self.mirror = mirror


class Group:
    def __init__(self, name, x, y, z, x_rot=0.0, y_rot=0.0, z_rot=0.0):
        self.name = name
        self.x, self.y, self.z = x, y, z
        self.x_rot, self.y_rot, self.z_rot = x_rot, y_rot, z_rot
        self.cubes = []
        self.children = []


def build_model(tassels, curls):
    """MagicCarpetEntityModel.createBodyLayer, part for part."""
    carpet = Group("carpet", 0.0, 0.0, 0.0)
    for i in range(SEGMENTS):
        first = i == 0
        last = i == SEGMENTS - 1
        cut_corners = first and curls
        segment = Group("segment_%d" % i, 0.0, 1.0, FRONT_Z + SEGMENT_LENGTH * i)

        if cut_corners:
            segment.cubes.append(Cube("slab", (56, 40), -8.0, -1.0, 0.0, 16.0, 1.0, 4.0))
        else:
            segment.cubes.append(Cube("slab", (0, 5 * i), -12.0, -1.0, 0.0, 24.0, 1.0, 4.0))

        if not cut_corners:
            segment.cubes.append(
                Cube("border_west", (56, 5 * i), -12.0, 0.0, 0.0, 2.0, 1.0, 4.0, BORDER_SIDES)
            )
            segment.cubes.append(
                Cube("border_east", (56, 5 * i), 10.0, 0.0, 0.0, 2.0, 1.0, 4.0, BORDER_SIDES)
            )
        if cut_corners:
            segment.cubes.append(
                Cube("border_front", (0, 43), -8.0, 0.0, 0.0, 16.0, 1.0, 2.0, BORDER_SIDES)
            )
        elif first:
            segment.cubes.append(
                Cube("border_front", (0, 40), -10.0, 0.0, 0.0, 20.0, 1.0, 2.0, BORDER_SIDES_BETWEEN)
            )
        elif last:
            segment.cubes.append(
                Cube("border_back", (0, 40), -10.0, 0.0, 2.0, 20.0, 1.0, 2.0, BORDER_SIDES_BETWEEN)
            )

        if tassels and first:
            for t, x in enumerate(TASSEL_X):
                tassel = Group("front_tassel_%d" % t, x, -0.5, 0.0)
                tassel.cubes.append(Cube("tassel", (44, 40), -0.5, -0.5, -3.0, 1.0, 1.0, 3.0))
                segment.children.append(tassel)
        if tassels and last:
            for t, x in enumerate(TASSEL_X):
                tassel = Group("back_tassel_%d" % t, x, -0.5, SEGMENT_LENGTH)
                tassel.cubes.append(Cube("tassel", (44, 40), -0.5, -0.5, 0.0, 1.0, 1.0, 3.0))
                segment.children.append(tassel)
        if cut_corners:
            segment.children.append(build_curl("left_curl", -1.0))
            segment.children.append(build_curl("right_curl", 1.0))

        carpet.children.append(segment)
    return carpet


def build_curl(name, side):
    """MagicCarpetEntityModel.addCurl."""
    inner_x = -4.0 if side < 0 else 0.0
    curl = Group(
        name,
        8.0 * side,
        0.0,
        SEGMENT_LENGTH,
        x_rot=CURL_PITCH,
        z_rot=CURL_ROLL * side,
    )
    curl.cubes.append(
        Cube("flap", (68, 0), inner_x, -1.0, -3.0, 4.0, 1.0, 3.0, CURL_FLAP_SIDES, side > 0)
    )
    curl.cubes.append(Cube("roll", (68, 5), inner_x, -1.0, -5.0, 4.0, 2.0, 2.0, ALL_SIDES, side > 0))
    return curl


# --- The animation --------------------------------------------------------------------------


def sample_poses(curls, speed, ticks):
    """setupAnim's part poses at each sampled tick, in Java model space.

    Returns a list of (tick, {part name: (x, y, z, xRot, yRot, zRot)}). The phases advance the way
    CarpetAnimation.advancePhase does, one tick at a time, so a constant speed gives the same phase
    the game would.
    """
    ripple_phase = 0.0
    flutter_phase = 0.0
    ripple_frequency = 0.08 + 0.42 * speed
    flutter_frequency = 0.12 + 0.6 * speed
    calm = 1.0 - 0.6 * speed
    amplitude = 0.4 + 1.1 * speed
    samples = []

    for tick in range(0, ticks + 1, SAMPLE_TICKS):
        time = float(tick)
        poses = {}
        poses["carpet"] = (
            0.0,
            HOVER_HEIGHT + 0.5 * mth_sin(time * 0.1) * calm,
            0.0,
            0.015 * mth_sin(time * 0.05 + 1.0) * calm,
            0.0,
            0.03 * mth_sin(time * 0.07) * calm,
        )

        heights = []
        for i in range(SEGMENTS + 1):
            along = i / SEGMENTS
            z = SEGMENT_LENGTH * i
            heights.append(
                amplitude * (0.5 + along) * mth_sin(z * 0.22 - ripple_phase)
                + 1.4 * speed * along * along
            )
        for i in range(SEGMENTS):
            poses["segment_%d" % i] = (
                0.0,
                1.0 + heights[i],
                FRONT_Z + SEGMENT_LENGTH * i,
                -math.atan2(heights[i + 1] - heights[i], SEGMENT_LENGTH),
                0.0,
                0.0,
            )

        for i in range(len(TASSEL_X)):
            flutter = (0.12 + 0.3 * speed) * mth_sin(flutter_phase + i * 1.3)
            sway = 0.12 * mth_sin(time * 0.09 + i * 2.1) * calm
            poses["front_tassel_%d" % i] = (
                TASSEL_X[i],
                -0.5,
                0.0,
                -(mth_lerp(speed, TASSEL_DROOP, 2.2) + flutter),
                sway,
                0.0,
            )
            poses["back_tassel_%d" % i] = (
                TASSEL_X[i],
                -0.5,
                SEGMENT_LENGTH,
                mth_lerp(speed, TASSEL_DROOP, 0.2) + flutter,
                sway,
                0.0,
            )

        if curls:
            for name, side in (("left_curl", -1.0), ("right_curl", 1.0)):
                poses[name] = (
                    8.0 * side,
                    0.0,
                    SEGMENT_LENGTH,
                    CURL_PITCH + 0.05 * mth_sin(time * 0.1) + 0.1 * speed,
                    0.0,
                    CURL_ROLL * side,
                )

        samples.append((tick, poses))

        # The phases advance once a tick, as CarpetAnimation.advancePhase does it, so a sample
        # every SAMPLE_TICKS ticks advances them SAMPLE_TICKS times.
        for _ in range(SAMPLE_TICKS):
            ripple_phase = (ripple_phase + ripple_frequency) % (2 * math.pi)
            flutter_phase = (flutter_phase + flutter_frequency) % (2 * math.pi)

    return samples


def rest_poses(curls):
    """The poses createBodyLayer gives each part, which the animation's deltas are added to."""
    poses = {"carpet": (0.0, 0.0, 0.0, 0.0, 0.0, 0.0)}
    for i in range(SEGMENTS):
        poses["segment_%d" % i] = (0.0, 1.0, FRONT_Z + SEGMENT_LENGTH * i, 0.0, 0.0, 0.0)
    for i in range(len(TASSEL_X)):
        poses["front_tassel_%d" % i] = (TASSEL_X[i], -0.5, 0.0, 0.0, 0.0, 0.0)
        poses["back_tassel_%d" % i] = (TASSEL_X[i], -0.5, SEGMENT_LENGTH, 0.0, 0.0, 0.0)
    if curls:
        for name, side in (("left_curl", -1.0), ("right_curl", 1.0)):
            poses[name] = (8.0 * side, 0.0, SEGMENT_LENGTH, CURL_PITCH, 0.0, CURL_ROLL * side)
    return poses


def animation(name, curls, speed, ticks):
    """One Blockbench animation: setupAnim's deltas from the rest pose, sampled every SAMPLE_TICKS.

    Blockbench adds a position keyframe to the bone's rest position and a rotation keyframe to its
    rest rotation (js/animations/timeline_animators.js, displayPosition and displayRotation), so
    the keyframes are the difference between setupAnim's pose and the rest pose, mirrored into
    Blockbench's space.
    """
    rest = rest_poses(curls)
    samples = sample_poses(curls, speed, ticks)
    animators = {}

    for part, rest_pose in rest.items():
        position = []
        rotation = []
        for tick, poses in samples:
            pose = poses[part]
            dx = pose[0] - rest_pose[0]
            dy = pose[1] - rest_pose[1]
            dz = pose[2] - rest_pose[2]
            rx = pose[3] - rest_pose[3]
            ry = pose[4] - rest_pose[4]
            rz = pose[5] - rest_pose[5]
            time = tick / TICKS_PER_SECOND
            position.append(keyframe(name, part, time, "position", -dx, -dy, dz))
            rotation.append(
                keyframe(
                    name,
                    part,
                    time,
                    "rotation",
                    -math.degrees(rx),
                    -math.degrees(ry),
                    math.degrees(rz),
                )
            )

        animators[stable_uuid("group/" + part)] = {
            "name": part,
            "type": "bone",
            "keyframes": position + rotation,
        }

    return {
        "uuid": stable_uuid("animation/" + name),
        "name": name,
        "loop": "once",
        "override": False,
        "length": ticks / TICKS_PER_SECOND,
        "snapping": TICKS_PER_SECOND,
        "selected": False,
        "animators": animators,
    }


def keyframe(animation, part, time, channel, x, y, z):
    return {
        "uuid": stable_uuid("keyframe/%s/%s/%s/%.4f" % (animation, part, channel, time)),
        "channel": channel,
        "time": round(time, 6),
        "color": -1,
        "interpolation": "linear",
        "data_points": [{"x": num(x), "y": num(y), "z": num(z)}],
    }


def num(value):
    text = "%.6f" % value
    text = text.rstrip("0").rstrip(".")
    return "0" if text in ("", "-0") else text


# --- Writing the project --------------------------------------------------------------------


def to_blockbench(x, y, z):
    return [-x, 24.0 - y, z]


def write_group(group, parent, elements, groups):
    """One group and its cubes, in Blockbench's absolute coordinates.

    Blockbench stores a group's origin in absolute model space and places it at that origin minus
    its parent's (js/outliner/outliner.js, updateTransform), so a part's Java offset from its
    parent is added to the parent's absolute origin here.
    """
    x = group.x + (0.0 if parent is None else parent[0])
    y = group.y + (0.0 if parent is None else parent[1])
    z = group.z + (0.0 if parent is None else parent[2])
    origin = to_blockbench(x, y, z)
    group_uuid = stable_uuid("group/" + group.name)
    groups.append(
        {
            "uuid": group_uuid,
            "name": group.name,
            "origin": origin,
            "rotation": [
                -math.degrees(group.x_rot),
                -math.degrees(group.y_rot),
                math.degrees(group.z_rot),
            ],
            "color": 0,
            "export": True,
            "mirror_uv": False,
            "isOpen": True,
            "locked": False,
            "visibility": True,
            "autouv": 0,
        }
    )

    children = []
    for cube in group.cubes:
        cube_uuid = stable_uuid("cube/%s/%s" % (group.name, cube.name))
        # Blockbench stores a cube's corners in absolute coordinates and its origin at the parent
        # group's origin (js/formats/java/modded_entity.ts, convertPositionsFromLocalToGlobal).
        low = to_blockbench(x + cube.x + cube.w, y + cube.y + cube.h, z + cube.z)
        high = to_blockbench(x + cube.x, y + cube.y, z + cube.z + cube.d)
        uvs = box_uvs(cube.w, cube.h, cube.d, cube.tex[0], cube.tex[1], cube.mirror)
        faces = {}
        for face in ALL_SIDES:
            faces[face] = {
                "uv": uvs[face],
                "texture": 0 if face in cube.sides else None,
            }
        elements.append(
            {
                "uuid": cube_uuid,
                "name": cube.name,
                "type": "cube",
                "from": low,
                "to": high,
                "origin": origin,
                "uv_offset": [cube.tex[0], cube.tex[1]],
                "box_uv": True,
                "mirror_uv": cube.mirror,
                "inflate": 0,
                "color": 0,
                "autouv": 0,
                "visibility": True,
                "export": True,
                "faces": faces,
            }
        )
        children.append(cube_uuid)

    for child in group.children:
        children.append(write_group(child, (x, y, z), elements, groups))

    return {"uuid": group_uuid, "isOpen": True, "children": children}


def data_url(path):
    import base64

    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")


def build_project(carpet, flags):
    elements = []
    groups = []
    outliner = []
    root = build_model(flags["tassels"], flags["curls"])
    outliner.append(write_group(root, None, elements, groups))

    textures = [
        {
            "path": "",
            "name": "%s_magic_carpet.png" % carpet,
            "folder": "entity",
            "namespace": "magic_carpet",
            "id": "0",
            "width": TEXTURE_WIDTH * 2,
            "height": TEXTURE_HEIGHT * 2,
            "uv_width": TEXTURE_WIDTH,
            "uv_height": TEXTURE_HEIGHT,
            "particle": False,
            "use_as_default": True,
            "render_mode": "default",
            "render_sides": "auto",
            "wrap_mode": "limited",
            "pbr_channel": "color",
            "internal": True,
            "saved": False,
            "visible": True,
            "mode": "bitmap",
            "source": data_url(ASSETS / ("textures/entity/%s_magic_carpet.png" % carpet)),
        }
    ]
    if flags["glow"]:
        textures.append(
            {
                "path": "",
                "name": "%s_magic_carpet_glow.png" % carpet,
                "folder": "entity",
                "namespace": "magic_carpet",
                "id": "1",
                "width": TEXTURE_WIDTH * 2,
                "height": TEXTURE_HEIGHT * 2,
                "uv_width": TEXTURE_WIDTH,
                "uv_height": TEXTURE_HEIGHT,
                "particle": False,
                "use_as_default": False,
                "render_mode": "default",
                "render_sides": "auto",
                "wrap_mode": "limited",
                "pbr_channel": "color",
                "internal": True,
                "saved": False,
                "visible": True,
                "mode": "bitmap",
                "source": data_url(ASSETS / ("textures/entity/%s_magic_carpet_glow.png" % carpet)),
            }
        )

    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "modded_entity",
            "box_uv": True,
        },
        "name": "%s_magic_carpet" % carpet,
        "model_identifier": "%s_magic_carpet" % carpet,
        "modded_entity_entity_class": "MagicCarpetEntity",
        "modded_entity_version": "1.17",
        "modded_entity_flip_y": True,
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": TEXTURE_WIDTH, "height": TEXTURE_HEIGHT},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": textures,
        "animations": [
            animation("idle", flags["curls"], 0.0, CYCLE_TICKS),
            animation("flying", flags["curls"], 1.0, CYCLE_TICKS),
        ],
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, default=DEFAULT_OUT)
    parser.add_argument(
        "--repo-out",
        type=Path,
        default=None,
        help="also write the files here, for the fidelity test to read",
    )
    args = parser.parse_args()

    for carpet, flags in CARPETS.items():
        project = build_project(carpet, flags)
        text = json.dumps(project, separators=(",", ":"))
        for directory in (args.out, args.repo_out):
            if directory is None:
                continue
            directory.mkdir(parents=True, exist_ok=True)
            path = directory / ("%s.bbmodel" % carpet)
            path.write_text(text)
            print("%s (%d bytes)" % (path, len(text)))


if __name__ == "__main__":
    main()
