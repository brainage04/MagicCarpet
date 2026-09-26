# MagicCarpet icon

## What this is

`icon.png` — the MagicCarpet mod icon, 1024 x 1024 PNG, SHA-256
`52f52a9ab6a20315c47b65fab1c5bd61760cb2287046af641b390fb59ba15a84` (`png_sha256` in
`provenance/magic-carpet-03-segmented-stack-metadata.json`). The mod ships it downscaled to 128 x 128 (Lanczos) as
`common/src/main/resources/assets/magic_carpet/icon.png`.

## How it was made

Blender render of the mod's own carpet models and textures — not a screenshot.

* `provenance/render-segmented.py` opens the approved tier-stack scene, `provenance/sources/approved-stack.blend`,
  and keeps its camera, lights, world, floor, 60 floating vanilla glyph particles, compositor and carpet material
  styling.
* It replaces each tier's five flat cuboids with the geometry of `MagicCarpetEntityModel.createBodyLayer`, transcribed
  box by box with Minecraft's box UV layout: eight segments, raised border, advanced/legendary tassels and the
  legendary curled corners. The carpets are posed with the model's `setupAnim` at 35% speed, so they ripple, lift at
  the back edge and trail their tassels.
* Textures are the shipped `textures/entity/<tier>_magic_carpet.png` (from `tools/generate_carpet_assets.py`), bound
  with `Closest` filtering; the legendary glow texture is added as emission, like its in-game full-bright pass.
* The luminous perimeter overlays are re-traced around the posed carpets' undersides (following the legendary
  corner notch). The glow lines and glyphs are presentation styling, not in-game behaviour.
* Blender 5.1.1, headless, Cycles CPU, 96 samples, 1024 x 1024. Texture hashes, pose, bounds and timings are in the
  metadata; `magic-carpet-03-segmented-stack.blend` is the packed scene as rendered.

## How to regenerate

After changing the model or textures, update the transcription at the top of `render-segmented.py` to match
`MagicCarpetEntityModel`, then from `docs/icon/provenance`:

```sh
nix shell nixpkgs#blender --command blender --background -noaudio --python-exit-code 1 --python render-segmented.py
mv magic-carpet-03-segmented-stack.png ../icon.png
```

`ICON_RENDER_THREADS` (default 2) and `ICON_SAMPLES` (default 96) override the render settings; use a low sample
count for quick previews. Downscale `icon.png` to 128 x 128 for the mod's `icon.png`.

## Previous icon

The other files in `provenance/` (`render-native.py`, `author-native-textures.py`, the `magic-carpet-02-*` scene and
metadata, `textures/`, `original/`, the verification JSON files and `evidence/`) document the previous icon, rendered
from the old flat 64 x 14 carpet art ("variant B"). They are kept for history; `sources/approved-stack.blend` is
shared by both renders.
