# MagicCarpet todo

## Loader parity findings (2026-09-29)

From running the release NeoForge jar on a real NeoForge 26.2.0.41-beta server and client. Items marked *both loaders* come from shared code.

- [ ] Low, both loaders: entity types have no translations, so `/summon`, `/kill` and `/ride` feedback shows `entity.magic_carpet.*_magic_carpet`.
- [ ] Low, both loaders: the legendary carpet tops out around 30 blocks/s (basic around 10.5) instead of the README's 48 (12); `getMaxSpeed()` and the README agree, the in-game speed doesn't.
- Unconfirmed, both loaders: holding W or space without moving the mouse makes the carpet coast to a stop after about 3 s. Possibly an Xvfb/xdotool artefact; check by hand.
