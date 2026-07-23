# MagicCarpet

MagicCarpet is a Fabric mod for Minecraft 26.2 that adds three craftable, rideable flying carpets. Install it on both the client and server.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API
- Java 25 or newer

## Carpet tiers

| Tier | Recipe | Maximum horizontal speed | Acceleration time |
| --- | --- | ---: | ---: |
| Basic | 4 yellow wool, 2 red wool, 2 string, and 1 stick | 12 blocks/s | 2 seconds |
| Advanced | 1 basic carpet, 4 blaze rods, and 4 gold blocks | 24 blocks/s | 1 second |
| Legendary | 1 advanced carpet, 1 nether star, 2 ender pearls, 2 diamond blocks, and 3 emerald blocks | 48 blocks/s | 0.5 seconds |

Collecting wool unlocks the basic carpet recipe. Each crafted carpet unlocks the next tier's advancement path.

## Controls

- Right-click a block with a carpet item to place the carpet.
- Right-click the placed carpet to ride it.
- Use the normal movement keys for horizontal motion.
- Hold jump while looking forward or upward to ascend.
- Hold jump while looking steeply downward to descend.
- Sneak to dismount.

Carpet riders do not take fall damage while mounted. Each carpet can carry two passengers.

## Development

```shell
./gradlew build
./gradlew runClientGameTest
```

The client GameTest validates dedicated-server entity registration, recipe loading, spawning, and rendering for all three carpet tiers.

Release automation is documented in [docs/RELEASE.md](docs/RELEASE.md). Optional Modrinth publishing is documented in [docs/MODRINTH.md](docs/MODRINTH.md).
