"""Checks the top surface design and the committed textures against the generator.

    python3 -m unittest discover -s tools -p 'test_*.py'
"""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import generate_carpet_assets as assets


class TopSurfaceDesignTest(unittest.TestCase):
    def test_no_motif_pixel_reaches_the_inner_border(self):
        for tier in assets.PALETTES:
            with self.subTest(tier=tier):
                assets.check_design(tier)

    def test_committed_textures_match_the_generator(self):
        for tier in assets.PALETTES:
            tex = assets.paint(tier)
            png = tex.png()
            for kind in ("entity", "item"):
                path = assets.ASSETS / f"textures/{kind}/{tier}_magic_carpet.png"
                with self.subTest(tier=tier, kind=kind):
                    self.assertEqual(png, path.read_bytes(), f"{path} is stale; run the generator")
            if assets.GLOW[tier]:
                path = assets.ASSETS / f"textures/entity/{tier}_magic_carpet_glow.png"
                with self.subTest(tier=tier, kind="glow"):
                    self.assertEqual(assets.glow(tex, tier).png(), path.read_bytes(), f"{path} is stale")


if __name__ == "__main__":
    unittest.main()
