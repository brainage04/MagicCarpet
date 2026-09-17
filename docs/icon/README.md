# MagicCarpet icon

## What this is

`icon.png` — the MagicCarpet mod icon, 1024 x 1024 PNG, SHA-256
`17535071e0afecc04eec944dbbffe290c47a0fa8e01ad40eb596bafbfe90ac27`.

Copied byte-identically from
`.local-icon-variants/provenance/from-round3/blender-u/magic-carpet-02-native-b-ranked-stack.png`
(SHA-256 verified at the source, and again on the copy). The hash matches `png_sha256` in
`provenance/magic-carpet-02-native-b-ranked-stack-metadata.json`.

## How it was made

Blender render of the mod's own entity model and textures — not a screenshot, and not in-game particles.

* Blender 5.1.1, headless `--background -noaudio --threads 2`, Cycles CPU (no GPU), 96 samples,
  1024 x 1024, 104.080 s. No display server, no audio sink, no Minecraft shader pack.
* Scene: `provenance/sources/approved-stack.blend`, the packed approved three-tier stack scene
  (from `blender-r/magic-carpet-01-revamped-stack.blend`, SHA-256
  `109d7e4d54bf301427070988b8c6078d696381142e9dda5bbb5e9d91bbfa07c2`). Only the three carpet image
  bindings were replaced, with `Closest` filtering. Geometry, UVs, tier stack, camera, lights, material
  parameters, world, particle effects and compositor are unchanged: the source and result object digests
  are equal (`59a39e0e…`) and so are the shader digests (`6512cef2…`), as recorded in the metadata.
* Imagery — the carpet art is **generated pixel art**, not a photograph of a shipped texture.
  `author-native-textures.py` draws new 64 x 14 atlases on the integer grid from the mod's original
  64 x 14 carpets (`provenance/original/`, the `f08018a^` assets) and the approved palette; it never
  resamples the approved 128 x 28 revamp art. Variant B adds a dark rank badge on **both** end borders
  with one ivory 2 x 2 pip for basic / tier 1, two for advanced / tier 2 and three for legendary / tier 3
  (pips separated by four dark pixels); the body motifs are identical to variant A.
* The glow around the stack is vanilla Minecraft particle textures, used unmodified: `glint.png`,
  `sga_*.png` and `glitter_*.png` from the Minecraft **26.2** client jar
  (`~/.gradle/caches/fabric-loom/26.2/minecraft-client.jar`, SHA-256
  `40896ee9f1e2bec3c934daac7e93d41e9e3d9c2f8ae0ca366d52ffbfd1afa290`; the packed copies in the scene were
  verified byte-identical against that jar). Particle placement is artistic, not an in-game particle
  capture: 60 visible particles at `particle_size_factor` 2. The full packed-image list with SHA-256 is in
  the metadata. The jar itself is not shipped.
* The six variant-B PNGs that actually change the mod's look are shipped at
  `provenance/textures/variant-b/{entity,item}/<tier>_magic_carpet.png`; `texture-inventory.json` maps each
  one to its `install_path` in this repository. They are byte-identical to the copies already committed at
  `texture-variants/original-resolution/variant-b/` (commit `cc88d9b`, see `local-commit.json` and
  `repository-verification.json`). The icon itself is not committed.

## Provenance files

* `magic-carpet-02-native-b-ranked-stack.py` — entry point (`render-native.render('variant-b')`);
  `render-native.py` — the scene author (binds the variant textures, asserts the unchanged scene
  invariants, renders, packs and writes the metadata); `author-native-textures.py` — draws the atlases.
* `magic-carpet-02-native-b-ranked-stack.blend` (packed), `-metadata.json` (texture bindings, packed
  images, bounds, render settings, invariant digests).
* `sources/approved-stack.blend` — the immutable approved scene; `original/` — the mod's original 64 x 14
  entity/item textures; `textures/variant-b/` — the six generated variant-B textures.
* `texture-inventory.json`, `asset-verification.json` (source inventory + variant-B texture hashes + the
  two authoring/verification commands), `packed-scene-verification.json`, `verification.json`,
  `visual-review.json`, `manifest.json`, `plan-and-diagnosis.json`, `repository-verification.json`,
  `local-commit.json`, `cleanup-report.json`, `blockers.json`.
* `run-blender.py` (recorded cgroup-limited launcher), `verify-assets.py`, `verify-scenes.py`.
* `evidence/` — baseline git state, resource records, and the byte-comparison of a re-rendered variant-B
  PNG (only the PNG `tEXt` timestamp/render-time fields and Cycles timing metadata differ on a re-render).
* `CURATION.json` — what was copied, which rows were filtered out, and what was left in round-3.

## How to regenerate

From `<repo>/docs/icon/provenance`:

```sh
python3 author-native-textures.py          # redraws textures/variant-a/** and textures/variant-b/**
# optional sanity check of the generated PNGs:
python3 verify-assets.py

nix shell nixpkgs#blender --command blender --background -noaudio --threads 2 \
  --python-exit-code 1 --python magic-carpet-02-native-b-ranked-stack.py
python3 verify-scenes.py
```

`author-native-textures.py` writes the variant PNGs first and then builds the comparison sheet; the sheet
step needs `revamp-2x/` (the approved previous-round 128 x 28 art), which is deliberately not shipped, so
the script stops there and does not rewrite `texture-inventory.json`. The variant textures themselves are
already written at that point. The recorded launcher was
`python3 run-blender.py magic-carpet-02-native-b-ranked-stack.py`, which enters the pre-existing
`render-blender.service` cgroup (asserting CPUWeight 20 and quota <= 3 cores), pins two render threads,
writes `provenance/logs/<script>.log` (that log directory was deliberately not copied) and refreshes
`evidence/resources-<script>.json`.

## Notes

* Only the selected variant B is shipped. Variant A's textures and packed scene are not copied, nor is the
  `texture-comparison.png` evidence sheet or the `revamp-2x/` input used only by that sheet. Shared
  verification files that describe the whole folder were kept verbatim where they are summaries
  (`verification.json`, `packed-scene-verification.json`) and filtered where they contain per-variant rows
  (`texture-inventory.json`, `asset-verification.json`, `packed-scene-verification.json.scenes`,
  `visual-review.json`) — see `CURATION.json`.
* The variant textures are 64 x 14 texture alternatives, not a resource pack: installing one means copying
  the six PNGs over `common/src/main/resources/assets/magic_carpet/textures/{entity,item}/`. Nothing in the
  mod's own assets was changed by this icon work.
* Run logs (`logs/**`) were excluded; `blockers.json` is kept because its scope notes explain the variant
  selection and the install mapping.
* Nothing else in the mod repository was modified and nothing new was committed.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
