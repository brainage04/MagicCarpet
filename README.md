# MagicCarpet

MagicCarpet is a Minecraft 26.3 mod for Fabric and NeoForge that adds three craftable, rideable flying carpets. Install it on both the client and server.

Landing a carpet costs its riders nothing: it neither hurts them nor counts towards their distance fallen, and it stays silent, as a happy ghast's landing does. Getting off in mid-air is a normal fall, though, so step off close to the ground. Each carpet can carry two passengers. A player steers when sharing a carpet with a mob.

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

Sitting down on a carpet and getting off it play the wool place and step sounds, which every player nearby hears. In flight the rider hears the elytra's wind, louder and higher the faster the carpet flies, as an elytra sounds at the same speed.

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

Each tier's flight and looks come from a data file, so a datapack can change them. A tier's file is `data/magic_carpet/magic_carpet/tier/<carpet>.json`, where `<carpet>` is `basic_magic_carpet`, `advanced_magic_carpet` or `legendary_magic_carpet`. A datapack file at the same path replaces the mod's; copy the mod's own file from the jar as a starting point. The server reads the files when the world starts (not on `/reload`) and sends them to every player who joins.

| Field | Meaning |
|---|---|
| `max_speed` | Top speed in blocks per tick (20 ticks a second), above 0. Climbing and descending go at half of it. |
| `acceleration_time` | Seconds to reach 99 % of a new speed, above 0. Coasting to a stop takes twice as long. |
| `drop_item` | The item a broken carpet drops, such as `magic_carpet:basic_magic_carpet`. |
| `particles` | Optional. The particles the carpet sheds, one picked by `weight` each time. Each has a `particle` (`{"type": "minecraft:end_rod"}`), a `velocity` and a `velocity_spread` (`[x, y, z]` in blocks per tick, a random share of the spread is added on each axis), and a `trail`, the extra speed towards the carpet's back at full speed. An empty list sheds none. |
| `model` | Optional. `tassels`, `curls` (curled front corners) and `glow` (draws `textures/entity/<carpet>_glow.png` at full brightness; only the legendary carpet ships one, so a resource pack has to add it for the others). Each is `true` or `false`, and missing ones are `false`. |

The three carpets, their recipes and their textures stay as they are; the files change only how each one flies and looks.

## Compatibility

No known incompatibilities.

## Documentation

- [Development](docs/DEVELOPMENT.md) - building, GameTests, and generated assets.
- [Icon](docs/icon/README.md) - icon sources and regeneration.
- [Release automation](docs/RELEASE.md)
- [Optional Modrinth publishing](docs/MODRINTH.md)
