package io.github.brainage04.magic_carpet;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.CarpetTier;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

public final class MagicCarpetGameTests {
    private MagicCarpetGameTests() {}

    public static void sustainedMovement(GameTestHelper helper) {
        ServerPlayer rider = mockRider(helper);
        try {
            EntityType<?>[] types = {
                ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()
            };
            double[] speeds = {0.6, 1.2, 2.4};
            for (int tier = 0; tier < types.length; tier++) {
                MagicCarpetEntity carpet =
                        (MagicCarpetEntity)
                                types[tier].create(helper.getLevel(), EntitySpawnReason.COMMAND);
                Vec3 origin = helper.absoluteVec(new Vec3(3, 16, 3));
                carpet.setPos(origin);
                assertTrue(rider.startRiding(carpet), "Rider could not mount " + types[tier]);
                rider.setYRot(0);
                rider.setXRot(0);
                rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
                double distance = 0;
                for (int tick = 0; tick < 160; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                    if (tick >= 140) distance += carpet.getZ() - origin.z;
                }
                assertNear(
                        speeds[tier] * 20,
                        distance,
                        0.02,
                        "Sustained horizontal blocks per second");
                rider.setLastClientInput(new Input(true, false, true, false, true, false, false));
                for (int tick = 0; tick < 100; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(
                        speeds[tier],
                        carpet.getDeltaMovement().horizontalDistance(),
                        0.002,
                        "Diagonal input must not boost speed");
                assertNear(speeds[tier] / 2, carpet.getDeltaMovement().y, 0.002, "Climb speed");
                rider.setXRot(60);
                for (int tick = 0; tick < 100; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(-speeds[tier] / 2, carpet.getDeltaMovement().y, 0.002, "Descent speed");
                rider.stopRiding();
                for (int tick = 0; tick < 200; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(
                        0,
                        carpet.getDeltaMovement().length(),
                        0,
                        "Dismounted carpet must coast to a complete stop");
                carpet.discard();
            }
        } finally {
            rider.stopRiding();
            rider.discard();
        }
        helper.succeed();
    }

    public static void passengersAndFallDamage(GameTestHelper helper) {
        MagicCarpetEntity carpet =
                ModEntities.basic().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Sheep first = helper.spawn(EntityTypes.SHEEP, new BlockPos(2, 2, 2));
        Sheep second = helper.spawn(EntityTypes.SHEEP, new BlockPos(4, 2, 2));
        Sheep third = helper.spawn(EntityTypes.SHEEP, new BlockPos(6, 2, 2));
        carpet.setPos(first.position());
        assertTrue(first.startRiding(carpet), "First passenger must mount");
        assertTrue(second.startRiding(carpet), "Second passenger must mount");
        assertTrue(!third.startRiding(carpet), "A third passenger must be rejected");
        assertTrue(
                carpet.getControllingPassenger() == first,
                "First passenger must control the carpet");
        float health = first.getHealth();
        first.hurtServer(helper.getLevel(), first.damageSources().fall(), 5);
        assertNear(health, first.getHealth(), 0, "Mounted passenger must not take fall damage");
        first.hurtServer(helper.getLevel(), first.damageSources().generic(), 2);
        assertNear(health - 2, first.getHealth(), 0, "Other damage must still apply");
        first.damageCooldownTime = 0;
        first.stopRiding();
        assertTrue(
                carpet.getControllingPassenger() == second, "Remaining passenger must take over");
        first.hurtServer(helper.getLevel(), first.damageSources().fall(), 5);
        assertNear(health - 7, first.getHealth(), 0, "Dismounted passenger must take fall damage");
        carpet.ejectPassengers();
        carpet.discard();
        helper.succeed();
    }

    public static void collision(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        MagicCarpetEntity carpet =
                ModEntities.legendary().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 2, 1.5));
        carpet.setPos(start);
        carpet.setDeltaMovement(0, 0, 2.4);
        carpet.tick();
        assertTrue(
                carpet.getBoundingBox().maxZ <= helper.absolutePos(wall).getZ() + 0.0001,
                "Fast carpet must not pass through a wall");
        assertTrue(carpet.horizontalCollision, "Wall contact must register as a collision");
        assertNear(
                0, carpet.getDeltaMovement().z, 0, "Collision must cancel velocity into the wall");
        carpet.discard();
        helper.succeed();
    }

    /**
     * The three tiers ship as data with the values their subclasses had (before the 26.3 cleanup:
     * basic 0.6 blocks a tick and 2 s, advanced 1.2 and 1 s, legendary 2.4 and 0.5 s), every carpet
     * reads the entry named like its entity type, a datapack file in the documented format parses,
     * and a top speed of 0 is refused.
     */
    public static void tierData(GameTestHelper helper) {
        Registry<CarpetTier> tiers =
                helper.getLevel().registryAccess().lookupOrThrow(CarpetTier.REGISTRY);
        assertTier(
                helper,
                tiers,
                ModEntities.basic(),
                0.6,
                2.0,
                ModItems.basic(),
                new CarpetTier.Model(false, false, false),
                List.of(
                        new Weighted<>(
                                new CarpetTier.Particle(
                                        ParticleTypes.ENCHANT,
                                        new Vec3(-0.75, 0.2, -0.75),
                                        new Vec3(1.5, 0.8, 1.5),
                                        0.0),
                                1)));
        assertTier(
                helper,
                tiers,
                ModEntities.advanced(),
                1.2,
                1.0,
                ModItems.advanced(),
                new CarpetTier.Model(true, false, false),
                List.of(
                        new Weighted<>(
                                new CarpetTier.Particle(
                                        ParticleTypes.SMALL_FLAME,
                                        new Vec3(0.0, 0.005, 0.0),
                                        new Vec3(0.0, 0.01, 0.0),
                                        0.04),
                                1)));
        assertTier(
                helper,
                tiers,
                ModEntities.legendary(),
                2.4,
                0.5,
                ModItems.legendary(),
                new CarpetTier.Model(true, true, true),
                List.of(
                        new Weighted<>(
                                new CarpetTier.Particle(
                                        ParticleTypes.PORTAL,
                                        new Vec3(-0.3, 0.0, -0.3),
                                        new Vec3(0.6, 0.3, 0.6),
                                        0.0),
                                3),
                        new Weighted<>(
                                new CarpetTier.Particle(
                                        ParticleTypes.END_ROD,
                                        new Vec3(-0.01, 0.01, -0.01),
                                        new Vec3(0.02, 0.0, 0.02),
                                        0.05),
                                7)));

        CarpetTier parsed =
                CarpetTier.CODEC
                        .parse(
                                JsonOps.INSTANCE,
                                JsonParser.parseString(
                                        """
                                        {
                                          "max_speed": 1.0,
                                          "acceleration_time": 0.25,
                                          "drop_item": "minecraft:stick",
                                          "particles": [
                                            {"weight": 2, "particle": {"type": "minecraft:flame"}}
                                          ],
                                          "model": {"curls": true}
                                        }
                                        """))
                        .getOrThrow(message -> new AssertionError("Tier file: " + message));
        assertTrue(
                parsed.maxSpeed() == 1.0
                        && parsed.accelerationTime() == 0.25
                        && parsed.dropItem() == Items.STICK
                        && parsed.model().equals(new CarpetTier.Model(false, true, false))
                        && parsed.particles()
                                .unwrap()
                                .equals(
                                        List.of(
                                                new Weighted<>(
                                                        new CarpetTier.Particle(
                                                                ParticleTypes.FLAME,
                                                                Vec3.ZERO,
                                                                Vec3.ZERO,
                                                                0.0),
                                                        2))),
                "A tier file must parse with its optional fields defaulted: " + parsed);
        assertTrue(
                CarpetTier.CODEC
                        .parse(
                                JsonOps.INSTANCE,
                                JsonParser.parseString(
                                        """
                                        {"max_speed": 0, "acceleration_time": 1,
                                         "drop_item": "minecraft:stick"}
                                        """))
                        .isError(),
                "A top speed of 0 must be refused");
        helper.succeed();
    }

    private static void assertTier(
            GameTestHelper helper,
            Registry<CarpetTier> tiers,
            EntityType<MagicCarpetEntity> type,
            double maxSpeed,
            double accelerationTime,
            Item dropItem,
            CarpetTier.Model model,
            List<Weighted<CarpetTier.Particle>> particles) {
        CarpetTier tier = tiers.getValueOrThrow(CarpetTier.key(EntityType.getKey(type)));
        assertTrue(
                tier.maxSpeed() == maxSpeed
                        && tier.accelerationTime() == accelerationTime
                        && tier.dropItem() == dropItem
                        && tier.model().equals(model)
                        && tier.particles().unwrap().equals(particles),
                "The shipped tier of " + type + " changed: " + tier);
        MagicCarpetEntity carpet = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        assertTrue(carpet.tier() == tier, "A " + type + " must fly by its own tier entry");
        carpet.discard();
    }

    public static void crafting(GameTestHelper helper) {
        Item wool = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("red_wool"));
        assertCrafts(
                helper,
                ModItems.basic(),
                wool,
                wool,
                wool,
                Items.PHANTOM_MEMBRANE,
                Items.PHANTOM_MEMBRANE,
                Items.PHANTOM_MEMBRANE,
                wool,
                wool,
                wool);
        assertCrafts(
                helper,
                ModItems.advanced(),
                Items.BLAZE_ROD,
                Items.GOLD_BLOCK,
                Items.BREEZE_ROD,
                Items.GOLD_BLOCK,
                ModItems.basic(),
                Items.GOLD_BLOCK,
                Items.BREEZE_ROD,
                Items.GOLD_BLOCK,
                Items.BLAZE_ROD);
        assertCrafts(
                helper,
                ModItems.legendary(),
                Items.DRAGON_BREATH,
                Items.NETHER_STAR,
                Items.DRAGON_BREATH,
                Items.DIAMOND_BLOCK,
                ModItems.advanced(),
                Items.DIAMOND_BLOCK,
                Items.SHULKER_SHELL,
                Items.SHULKER_SHELL,
                Items.SHULKER_SHELL);
        CraftingInput incomplete =
                CraftingInput.of(
                        3,
                        3,
                        Arrays.asList(
                                new ItemStack(wool),
                                new ItemStack(wool),
                                new ItemStack(wool),
                                ItemStack.EMPTY,
                                ItemStack.EMPTY,
                                ItemStack.EMPTY,
                                new ItemStack(wool),
                                new ItemStack(wool),
                                new ItemStack(wool)));
        assertTrue(
                helper.getLevel()
                        .getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, incomplete, helper.getLevel())
                        .isEmpty(),
                "Wool without phantom membranes must not craft a carpet");
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, Item result, Item... ingredients) {
        CraftingInput input =
                CraftingInput.of(3, 3, Arrays.stream(ingredients).map(ItemStack::new).toList());
        var recipe =
                helper.getLevel()
                        .getServer()
                        .getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel())
                        .orElseThrow(() -> new AssertionError("No crafting recipe for " + result));
        ItemStack crafted = recipe.value().assemble(input);
        assertTrue(
                crafted.is(result) && crafted.getCount() == 1,
                "Crafting must produce one " + result);
    }

    public static void placement(GameTestHelper helper) {
        Item[] items = {ModItems.basic(), ModItems.advanced(), ModItems.legendary()};
        EntityType<?>[] types = {
            ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()
        };
        helper.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
        for (int tier = 0; tier < items.length; tier++) {
            for (GameType mode : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
                Player player = helper.makeMockPlayer(mode);
                mode.updatePlayerAbilities(player.getAbilities());
                player.setPos(helper.absoluteVec(new Vec3(3.5, 3, 3.5)));
                player.setXRot(90);
                ItemStack held = new ItemStack(items[tier], 2);
                held.set(DataComponents.CUSTOM_NAME, Component.literal("Flying rug"));
                player.setItemInHand(InteractionHand.MAIN_HAND, held);
                assertTrue(
                        items[tier].use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                                == InteractionResult.SUCCESS,
                        "Carpet placement must succeed in " + mode);
                var placed =
                        helper.getLevel()
                                .getEntitiesOfClass(
                                        MagicCarpetEntity.class,
                                        helper.absoluteAABB(
                                                new net.minecraft.world.phys.AABB(
                                                        2, 2, 2, 5, 3, 5)));
                assertTrue(
                        placed.size() == 1 && placed.getFirst().getType() == types[tier],
                        "Placement must spawn the matching tier once");
                assertTrue(
                        Component.literal("Flying rug").equals(placed.getFirst().getCustomName()),
                        "Placement must preserve the item's name");
                assertNear(
                        mode == GameType.CREATIVE ? 2 : 1,
                        held.getCount(),
                        0,
                        "Placement consumption");
                placed.getFirst().discard();
                player.discard();
            }
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(3.5, 3, 3.5)));
        player.setXRot(90);
        ItemStack held = new ItemStack(ModItems.basic(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        helper.setBlock(new BlockPos(2, 2, 3), Blocks.STONE);
        assertTrue(
                ModItems.basic().use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        == InteractionResult.FAIL,
                "Placement overlapping an adjacent wall must fail");
        assertNear(2, held.getCount(), 0, "Failed placement must not consume the item");
        assertTrue(
                helper.getEntities(ModEntities.basic()).isEmpty(),
                "Failed placement must not spawn a carpet");
        player.discard();
        helper.succeed();
    }

    public static void persistenceAndDrops(GameTestHelper helper) {
        EntityType<?>[] types = {
            ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()
        };
        Item[] items = {ModItems.basic(), ModItems.advanced(), ModItems.legendary()};
        for (int tier = 0; tier < types.length; tier++) {
            MagicCarpetEntity original =
                    (MagicCarpetEntity)
                            types[tier].create(helper.getLevel(), EntitySpawnReason.COMMAND);
            original.setPos(helper.absoluteVec(new Vec3(3.5, 3, 3.5)));
            original.setCustomName(Component.literal("Saved rug"));
            original.setDeltaMovement(0.2, 0.1, -0.3);
            var output =
                    TagValueOutput.createWithContext(
                            ProblemReporter.DISCARDING, helper.getLevel().registryAccess());
            assertTrue(original.save(output), "Carpet must be saved");
            MagicCarpetEntity restored =
                    (MagicCarpetEntity)
                            types[tier].create(helper.getLevel(), EntitySpawnReason.LOAD);
            restored.load(
                    TagValueInput.create(
                            ProblemReporter.DISCARDING,
                            helper.getLevel().registryAccess(),
                            output.buildResult()));
            assertTrue(
                    original.position().equals(restored.position()),
                    "Position must survive reload");
            assertTrue(
                    original.getDeltaMovement().equals(restored.getDeltaMovement()),
                    "Coasting motion must survive reload");
            assertTrue(
                    original.getCustomName().equals(restored.getCustomName()),
                    "Name must survive reload");
            restored.hurtServer(helper.getLevel(), restored.damageSources().generic(), 5);
            assertTrue(restored.isRemoved(), "Enough damage must destroy the carpet");
            var drops =
                    helper.getLevel()
                            .getEntitiesOfClass(
                                    ItemEntity.class, restored.getBoundingBox().inflate(2));
            assertTrue(
                    drops.size() == 1
                            && drops.getFirst().getItem().is(items[tier])
                            && drops.getFirst().getItem().getCount() == 1,
                    "Breaking a carpet must drop exactly its own tier");
            assertTrue(
                    Component.literal("Saved rug")
                            .equals(drops.getFirst().getItem().get(DataComponents.CUSTOM_NAME)),
                    "Dropped item must keep the carpet's name");
            drops.getFirst().discard();
            original.discard();
        }
        helper.succeed();
    }

    public static void playerTakesControl(GameTestHelper helper) {
        ServerPlayer rider = mockRider(helper);
        MagicCarpetEntity carpet =
                ModEntities.basic().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Sheep sheep = EntityTypes.SHEEP.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        try {
            Vec3 origin = helper.absoluteVec(new Vec3(3, 16, 3));
            carpet.setPos(origin);
            assertTrue(sheep.startRiding(carpet), "Mob must be able to board first");
            assertTrue(rider.startRiding(carpet), "Player must be able to join a mob");
            assertTrue(
                    carpet.getControllingPassenger() == rider,
                    "Player must steer even if a mob boarded first");
            rider.setYRot(0);
            rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
            carpet.tick();
            assertTrue(
                    carpet.getZ() > origin.z,
                    "Player's input must move the carpet with a mob aboard");
            assertTrue(sheep.getVehicle() == carpet, "Taking control must not eject the mob");
            rider.stopRiding();
            assertTrue(
                    carpet.getControllingPassenger() == sheep,
                    "Mob must remain aboard after the player leaves");
        } finally {
            rider.stopRiding();
            carpet.ejectPassengers();
            carpet.discard();
            sheep.discard();
            rider.discard();
        }
        helper.succeed();
    }

    /**
     * A plain right click mounts a carpet, and the same right click while sneaking does not: the
     * carpet reads a sneaking player's click as "use what is in my hand", its own interaction
     * passes, and nothing is boarded.
     */
    public static void mountRequiresAPlainUse(GameTestHelper helper) {
        MagicCarpetEntity carpet =
                ModEntities.basic().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        carpet.setPos(helper.absoluteVec(new Vec3(3, 2, 3)));
        assertTrue(helper.getLevel().addFreshEntity(carpet), "The carpet must be in the level");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(carpet.position());
        try {
            player.setShiftKeyDown(true);
            assertTrue(
                    carpet.interact(player, InteractionHand.MAIN_HAND, carpet.position())
                            == InteractionResult.PASS,
                    "Sneaking and using must pass the carpet's interaction");
            assertTrue(!player.isPassenger(), "A sneaking player must stay off the carpet");
            player.setShiftKeyDown(false);
            assertTrue(
                    carpet.interact(player, InteractionHand.MAIN_HAND, carpet.position())
                            == InteractionResult.SUCCESS,
                    "A plain right click must board the carpet");
            assertTrue(player.getVehicle() == carpet, "The clicking player must be riding");
        } finally {
            player.stopRiding();
            player.discard();
            carpet.discard();
        }
        helper.succeed();
    }

    /**
     * Letting go of the movement keys does not stop the carpet: it keeps its speed for a moment,
     * slows tick by tick, and comes to rest. Being slower to stop is what he asked for; stopping is
     * what keeps it from drifting forever.
     */
    public static void releasedKeysCoastToAStop(GameTestHelper helper) {
        ServerPlayer rider = mockRider(helper);
        MagicCarpetEntity carpet =
                ModEntities.advanced().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        try {
            Vec3 origin = helper.absoluteVec(new Vec3(3, 16, 3));
            carpet.setPos(origin);
            assertTrue(rider.startRiding(carpet), "Rider must mount");
            rider.setYRot(0);
            rider.setXRot(0);
            rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
            for (int tick = 0; tick < 40; tick++) {
                carpet.setPos(origin);
                carpet.tick();
            }
            double flying = carpet.getDeltaMovement().horizontalDistance();
            assertNear(1.2, flying, 0.02, "The forward key must reach the advanced top speed");

            rider.setLastClientInput(new Input(false, false, false, false, false, false, false));
            carpet.setPos(origin);
            carpet.tick();
            double justReleased = carpet.getDeltaMovement().horizontalDistance();
            assertTrue(
                    justReleased > flying * 0.85,
                    "Letting go must not stop the carpet at once, found " + justReleased);
            for (int tick = 0; tick < 5; tick++) {
                carpet.setPos(origin);
                carpet.tick();
                assertTrue(
                        carpet.getDeltaMovement().horizontalDistance() <= justReleased,
                        "Coasting must slow the carpet, never speed it up");
            }
            for (int tick = 0; tick < 200; tick++) {
                carpet.setPos(origin);
                carpet.tick();
            }
            assertNear(
                    0,
                    carpet.getDeltaMovement().length(),
                    0,
                    "A carpet with no input must come to rest");
        } finally {
            rider.stopRiding();
            carpet.discard();
            rider.discard();
        }
        helper.succeed();
    }

    /**
     * A riding ServerPlayer sends its passenger packets through its connection, which
     * makeMockServerPlayer does not create; the mock stays out of the player list.
     */
    private static ServerPlayer mockRider(GameTestHelper helper) {
        ServerPlayer rider = (ServerPlayer) helper.makeMockServerPlayer(GameType.CREATIVE);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(
                helper.getLevel().getServer(),
                connection,
                rider,
                CommonListenerCookie.createInitial(rider.getGameProfile(), false));
        return rider;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertNear(
            double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected " + expected + ", found " + actual);
        }
    }
}
