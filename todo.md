# MagicCarpet todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] Entity types now have English translations for `/summon`, `/kill` and `/ride` feedback.
- [x] Carpets are server-authoritative on the controlling rider's client as well as the server. This prevents client vehicle-position packets from overwriting server movement and reducing speed.
- [ ] Thomas: hold W and jump without moving the mouse on both loaders. The authority fix addresses the conflicting movement paths; the by-hand check is still needed.

## Asset history decision

- [x] Thomas: remove `texture-variants/original-resolution/variant-{a,b}/` and the old `magic-carpet-02-*` icon scenes (A-0308). The scene's render script stays, so `docs/icon/README.md` "Previous icon" can still say how it was made.
