#!/usr/bin/env python3
"""Writes the showcase film's runway, a saved structure the film places under each flight.

The runway is 17 blocks wide and 409 long, one block thick, in stripes of four blocks of smooth quartz and black
concrete, so the ground shows the carpet's speed. MagicCarpetShowcase places it with its north-west corner at
z = -400, a multiple of eight, so the stripes fall on the same world blocks wherever the film places it. Standard
library only.

    python3 tools/generate_showcase_runway.py
"""
import gzip
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "fabric/src/recording/resources/magic_carpet-runway.nbt"

WIDTH, LENGTH = 17, 409
STRIPE = 4
# SharedConstants.WORLD_VERSION in 26.3 (net/minecraft/SharedConstants.java:16). StructureTemplate.load does not
# upgrade older data, so the file carries the version and the palette key ("id", NbtUtils.java:125) the film runs on.
DATA_VERSION = 5023
PALETTE = ["minecraft:smooth_quartz", "minecraft:black_concrete"]

TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 3, 8, 9, 10


def name(text):
    data = text.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def int_list(values):
    return bytes([TAG_INT]) + struct.pack(">i", len(values)) + b"".join(struct.pack(">i", v) for v in values)


def compound_list(compounds):
    return bytes([TAG_COMPOUND]) + struct.pack(">i", len(compounds)) + b"".join(compounds)


def compound(fields):
    """fields: (tag type, name, payload bytes) triples; returns the payload, ended by TAG_End."""
    return b"".join(bytes([kind]) + name(key) + payload for kind, key, payload in fields) + b"\x00"


def main():
    blocks = []
    for z in range(LENGTH):
        state = (z // STRIPE) % 2
        for x in range(WIDTH):
            blocks.append(compound([(TAG_LIST, "pos", int_list([x, 0, z])), (TAG_INT, "state", struct.pack(">i", state))]))
    palette = [compound([(TAG_STRING, "id", name(block))]) for block in PALETTE]
    root = compound([
        (TAG_INT, "DataVersion", struct.pack(">i", DATA_VERSION)),
        (TAG_LIST, "size", int_list([WIDTH, 1, LENGTH])),
        (TAG_LIST, "palette", compound_list(palette)),
        (TAG_LIST, "blocks", compound_list(blocks)),
        (TAG_LIST, "entities", bytes([TAG_COMPOUND]) + struct.pack(">i", 0)),
    ])
    OUT.write_bytes(gzip.compress(bytes([TAG_COMPOUND]) + name("") + root, mtime=0))
    print(f"{OUT.relative_to(ROOT)}: {len(blocks)} blocks, {OUT.stat().st_size} bytes")


if __name__ == "__main__":
    main()
