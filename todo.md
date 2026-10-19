# MagicCarpet todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [x] Entity types now have English translations for `/summon`, `/kill` and `/ride` feedback.
- [x] Carpets are server-authoritative on the controlling rider's client as well as the server. This prevents client vehicle-position packets from overwriting server movement and reducing speed.
- [ ] Thomas: hold W and jump without moving the mouse on both loaders. The authority fix addresses the conflicting movement paths; the by-hand check is still needed.

## Asset history decision

- [ ] Thomas: decide whether to remove `texture-variants/original-resolution/variant-{a,b}/` and the old `magic-carpet-02-*` icon scenes. Recommendation: keep them, because the icon provenance records reference them.
