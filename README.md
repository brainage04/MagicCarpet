# MagicCarpet

MagicCarpet is a Minecraft 26.3 mod for Fabric and NeoForge that adds three craftable, rideable flying carpets. Install it on both the client and server.

A carpet's landing does not hurt its riders, does not add to their distance fallen and makes no sound, like a happy ghast's landing. A rider who gets off in mid-air falls as usual and can take fall damage, so get off close to the ground. Each carpet can carry two passengers. A player steers when sharing a carpet with a mob.

## Features

### Carpet tiers

Acceleration times are to 99% of top speed; coasting times are to 1%.

- Basic: 6 wool of any colour and 3 phantom membranes; 12 blocks/s horizontally, 6 blocks/s vertically; 2 seconds accelerating, 4 seconds coasting.
- Advanced: 1 basic carpet, 2 blaze rods, 2 breeze rods, and 4 gold blocks; 24 blocks/s horizontally, 12 blocks/s vertically; 1 second accelerating, 2 seconds coasting.
- Legendary: 1 advanced carpet, 1 nether star, 2 dragon's breath, 2 diamond blocks, and 3 shulker shells; 48 blocks/s horizontally, 24 blocks/s vertically; 0.5 seconds accelerating, 1 second coasting.

Horizontal and vertical speeds are independent, so climbing or descending while moving at full horizontal speed is about 12% faster overall. Releasing the controls, or dismounting, lets the carpet coast to a stop.

Collecting wool unlocks the basic carpet recipe. Each crafted carpet unlocks the next tier's advancement path.

### Appearance

Each tier looks different: basic carpets have a plain rolled edge, advanced carpets add a gold cord and tassels, and legendary carpets add curled front corners and threads that glow in the dark. Carpets ripple, bob, and sway while hovering, stream their tassels behind them in flight, and shed tier-specific particles.

### Sound

The wool place sound plays when the first rider sits down on a carpet, and the wool step sound plays when the last rider gets off. Every player nearby hears both. In flight the rider hears the elytra's wind at the volume and pitch an elytra has at the same speed, so a faster carpet sounds louder.

## Getting started

- Minecraft 26.3
- Java 25 or newer
- Fabric Loader 0.19.5 or newer and Fabric API, or NeoForge 26.3.0.99 or newer
- Install it on both the client and server.
- Right-click a block with a carpet item to place the carpet.
- Right-click the placed carpet to ride it.

- Use the normal movement keys for horizontal motion.
- Hold jump while looking forward or upward to ascend.
- Hold jump while looking steeply downward to descend.
- Sneak to dismount.

## Commands

No commands yet.

## Configuration

A data file sets each tier's flight and looks, so a datapack can change them. The file for a tier is `data/magic_carpet/magic_carpet/tier/<carpet>.json`, where `<carpet>` is `basic_magic_carpet`, `advanced_magic_carpet` or `legendary_magic_carpet`. A datapack file at the same path replaces the mod's file. The mod's own files in the jar are a good starting point. The server reads the files when the world starts, not on `/reload`, and sends them to every player who joins. The speeds under Carpet tiers are the values in the mod's own files.

| Field | Meaning |
|---|---|
| `max_speed` | Top speed in blocks per tick (20 ticks a second), above 0. Climbing and descending go at half this speed. |
| `acceleration_time` | Seconds to reach 99 % of a new speed, above 0. Coasting to a stop takes twice as long. |
| `drop_item` | The item a broken carpet drops, such as `magic_carpet:basic_magic_carpet`. |
| `particles` | Optional. A list of the particles the carpet sheds. Each spawn picks one entry, weighted by its `weight`. `particle` names the particle, such as `{"type": "minecraft:end_rod"}`. `velocity` is its speed as `[x, y, z]` in blocks per tick. `velocity_spread` is also `[x, y, z]`, and a random part of it is added on each axis. `trail` is the extra speed towards the carpet's back when the carpet flies at full speed. `velocity`, `velocity_spread` and `trail` are optional and default to 0. An empty list sheds no particles. |
| `model` | Optional. `tassels` adds tassels, `curls` adds curled front corners, and `glow` draws `textures/entity/<carpet>_glow.png` at full brightness. Each is `true` or `false`, and a missing one is `false`. Only the legendary carpet ships a glow texture, so a resource pack has to add one before another tier can glow. |

The files cannot add a carpet or change a carpet's recipe or texture.

## Compatibility

No known incompatibilities.

## Documentation

- [Development](docs/DEVELOPMENT.md) - building, GameTests, and generated assets.
- [Icon](docs/icon/README.md) - icon sources and regeneration.
- [Release automation](docs/RELEASE.md)
- [Optional Modrinth publishing](docs/MODRINTH.md)
