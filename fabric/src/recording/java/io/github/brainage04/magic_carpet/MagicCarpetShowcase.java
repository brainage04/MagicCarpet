package io.github.brainage04.magic_carpet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.minecraftmodconventions.ClientGameTestPlayerActions;
import io.github.brainage04.minecraftmodconventions.ClientGameTestRecorder;
import io.github.brainage04.minecraftmodconventions.ClientGameTestStages;
import io.github.brainage04.minecraftmodconventions.ClientGameTestWaits;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The film of Magic Carpet's showcase: the three tiers side by side, then each tier mounted and
 * flown.
 *
 * <p>Every stage plays like a person: the player turns to the carpet it rides, presses the use key
 * with that carpet under the crosshair and inside vanilla reach, and flies it with the forward and
 * jump keys. {@link ClientGameTestPlayerActions} fails the stage when the crosshair is not on the
 * carpet or the carpet is out of reach.
 *
 * <p>Server-side calls only build the scene, place the player between stages and assert what the
 * camera cannot prove, and each one happens before its stage is filmed.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetShowcase {
    private static final int FLOOR_Y = 100;
    private static final int STAND_Y = FLOOR_Y + 1;
    private static final int RENDER_DISTANCE = 12;

    private static final float NORTH = 180.0F;
    private static final int NORMAL_FOV = 70;

    /** Where the three tiers hang for the first stage, and where the player stands to see them. */
    private static final double TIERS_Z = 0.5D;

    private static final double TIERS_STAND_Z = 4.8D;
    private static final double TIER_SPACING = 2.0D;

    /**
     * One site per flight stage, far enough apart that a stage never fights the last one's scene.
     */
    private static final int BASIC_SITE = 0;

    private static final int ADVANCED_SITE = 96;
    private static final int LEGENDARY_SITE = 192;

    /** The carpet's height above the platform, and where the player stands to mount it. */
    private static final double CARPET_Y = STAND_Y + 0.5D;

    private static final double FLIGHT_STAND_Z = 4.5D;

    /** How close the player stops to the carpet before the right click. */
    private static final double MOUNT_REACH = 2.0D;

    /** The runway's near and far end: the flight stays over it for its whole length. */
    private static final int RUNWAY_NEAR_Z = 8;

    private static final int RUNWAY_FAR_Z = -200;

    private static final long BEFORE_MILLIS = 400L;
    private static final long MOUNT_MILLIS = 300L;
    private static final long FLY_MILLIS = 1600L;

    /** The climb after the level run, short enough to keep the runway in frame. */
    private static final long CLIMB_MILLIS = 500L;

    private static final long LEVEL_MILLIS = 500L;
    private static final long COAST_MILLIS = 700L;
    private static final long AFTER_MILLIS = 400L;

    /** Ticks the flight speed is measured over, once the carpet has reached its top speed. */
    private static final int SPEED_WINDOW_TICKS = 20;

    /** The most the measured top speed may differ from the tier's, as a share of that speed. */
    private static final double SPEED_TOLERANCE = 0.05D;

    private static final int CAPTION_LEAD_TICKS = 6;
    private static final int HOLD_AFTER_END_TICKS = 6;
    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int MOUNT_TIMEOUT_TICKS = 100;

    private static final Map<String, String[]> CAPTIONS = loadCaptions();

    private MagicCarpetShowcase() {}

    /** Server properties the film wants: chunks out to its horizon. */
    public static Properties serverProperties(Properties properties) {
        properties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));
        return properties;
    }

    /** Builds the platforms the four stages stand on, before the client joins. */
    public static void buildStage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int site : List.of(BASIC_SITE, ADVANCED_SITE, LEGENDARY_SITE)) {
            // A long striped floor under each flight: the stripes are what makes the speed read.
            for (int z = RUNWAY_FAR_Z; z <= RUNWAY_NEAR_Z; z++) {
                BlockState stripe =
                        (Math.floorDiv(z, 4) & 1) == 0
                                ? Blocks.SMOOTH_QUARTZ.defaultBlockState()
                                : Blocks.POLISHED_DIORITE.defaultBlockState();
                for (int x = site - 8; x <= site + 8; x++) {
                    level.setBlock(cursor.set(x, FLOOR_Y, z), stripe, Block.UPDATE_CLIENTS);
                }
            }
            // Clear the air where the player and the carpet stand, so nothing clips the mount.
            for (int x = site - 8; x <= site + 8; x++) {
                for (int z = -8; z <= 8; z++) {
                    for (int y = FLOOR_Y + 1; y <= FLOOR_Y + 6; y++) {
                        level.setBlock(
                                cursor.set(x, y, z),
                                Blocks.AIR.defaultBlockState(),
                                Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        command(server, "gamerule advance_time false");
        command(server, "gamerule advance_weather false");
        command(server, "gamerule spawn_mobs false");
        command(server, "weather clear");
        command(server, "time set noon");
    }

    /** Films the four stages, in order. */
    public static void film(ClientGameTestContext context, TestDedicatedServerContext server) {
        context.runOnClient(
                client -> {
                    client.options.renderDistance().set(RENDER_DISTANCE);
                    // The server only sends chunks out to the render distance the client last told
                    // it.
                    client.options.broadcastOptions();
                    // The tiers' own particles are part of what the film shows.
                    client.options.particles().set(ParticleStatus.ALL);
                    client.options.chunkSectionFadeInTime().set(0.0D);
                    client.options.notificationDisplayTime().set(0.5D);
                    client.options.fov().set(NORMAL_FOV);
                });
        tiers(context, server);
        flight(
                context,
                server,
                BASIC_SITE,
                "magic_carpet.basic",
                ModEntities.basic(),
                ModItems.basic(),
                0.6D);
        flight(
                context,
                server,
                ADVANCED_SITE,
                "magic_carpet.advanced",
                ModEntities.advanced(),
                ModItems.advanced(),
                1.2D);
        flight(
                context,
                server,
                LEGENDARY_SITE,
                "magic_carpet.legendary",
                ModEntities.legendary(),
                ModItems.legendary(),
                2.4D);
        context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
    }

    /** The three tiers side by side, close enough that the shapes and the textures read. */
    private static void tiers(ClientGameTestContext context, TestDedicatedServerContext server) {
        Vec3 stand = new Vec3(0.5D, STAND_Y, TIERS_STAND_Z);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    ServerLevel level = player.level();
                    resetPlayer(player);
                    player.getInventory().setItem(0, new ItemStack(ModItems.basic()));
                    player.getInventory().setItem(1, new ItemStack(ModItems.advanced()));
                    player.getInventory().setItem(2, new ItemStack(ModItems.legendary()));
                    player.teleportTo(
                            level, stand.x, stand.y, stand.z, Set.of(), NORTH, 8.0F, true);
                    double[] xs = {-TIER_SPACING, 0.0D, TIER_SPACING};
                    EntityType<?>[] types = {
                        ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()
                    };
                    for (int tier = 0; tier < types.length; tier++) {
                        MagicCarpetEntity carpet =
                                (MagicCarpetEntity)
                                        types[tier].create(level, EntitySpawnReason.COMMAND);
                        check(carpet != null, "Expected a registered carpet for " + types[tier]);
                        carpet.setPos(xs[tier], CARPET_Y, TIERS_Z);
                        check(level.addFreshEntity(carpet), "Failed to spawn " + types[tier]);
                    }
                });
        context.waitFor(
                client ->
                        client.level.dimension() == Level.OVERWORLD
                                && client.player.position().distanceTo(stand) < 0.01D,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.gui.hud.getChat().clearMessages(false);
                    client.player.getInventory().setSelectedSlot(0);
                });
        ClientGameTestWaits.waitForChunkWindow(context, RENDER_DISTANCE - 2, LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);

        String id = "magic_carpet.tiers";
        ClientGameTestPlayerActions.lookAt(context, tierPoint(-TIER_SPACING));
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.walkTo(context, tierPoint(0.0D), 3.0D);
        // A mouse turn across the row, holding each tier at the middle of the frame.
        ClientGameTestPlayerActions.lookAt(context, tierPoint(-TIER_SPACING));
        ClientGameTestWaits.holdFor(context, 900L);
        ClientGameTestPlayerActions.lookAt(context, tierPoint(0.0D));
        ClientGameTestWaits.holdFor(context, 900L);
        ClientGameTestPlayerActions.lookAt(context, tierPoint(TIER_SPACING));
        ClientGameTestWaits.holdFor(context, 900L);
        ClientGameTestPlayerActions.lookAt(context, tierPoint(0.0D));
        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        server.runOnServer(
                minecraftServer -> {
                    List<MagicCarpetEntity> carpets =
                            player(minecraftServer)
                                    .level()
                                    .getEntitiesOfClass(
                                            MagicCarpetEntity.class,
                                            new AABB(
                                                    -5,
                                                    CARPET_Y - 1,
                                                    TIERS_Z - 2,
                                                    5,
                                                    CARPET_Y + 2,
                                                    TIERS_Z + 2));
                    check(
                            carpets.size() == 3,
                            "The three tiers must stand side by side, found " + carpets.size());
                    List<EntityType<?>> shown =
                            carpets.stream().map(MagicCarpetEntity::getType).toList();
                    for (EntityType<?> type :
                            List.of(
                                    ModEntities.basic(),
                                    ModEntities.advanced(),
                                    ModEntities.legendary())) {
                        check(shown.contains(type), "Tier " + type + " must be on the platform");
                    }
                });
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * One tier: the player mounts it with a right click, flies it with the forward and jump keys,
     * and lets go to watch it coast. The rider stays aboard, so the stage never films a fall from
     * the carpet's height; the next stage teleports the player before its caption.
     */
    private static void flight(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            int site,
            String id,
            EntityType<?> type,
            Item item,
            double topSpeed) {
        Vec3 stand = new Vec3(site + 0.5D, STAND_Y, FLIGHT_STAND_Z);
        int[] carpetId = new int[1];
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    ServerLevel level = player.level();
                    resetPlayer(player);
                    player.getInventory().setItem(0, new ItemStack(item));
                    player.teleportTo(
                            level, stand.x, stand.y, stand.z, Set.of(), NORTH, 10.0F, true);
                    level.getEntitiesOfClass(ItemEntity.class, siteBox(site))
                            .forEach(ItemEntity::discard);
                    level.getEntitiesOfClass(MagicCarpetEntity.class, siteBox(site))
                            .forEach(MagicCarpetEntity::discard);
                    MagicCarpetEntity carpet =
                            (MagicCarpetEntity) type.create(level, EntitySpawnReason.COMMAND);
                    check(carpet != null, "Expected a registered carpet for " + type);
                    carpet.setPos(site + 0.5D, CARPET_Y, 0.5D);
                    check(level.addFreshEntity(carpet), "Failed to spawn " + type);
                    carpetId[0] = carpet.getId();
                });
        context.waitFor(
                client ->
                        client.level.dimension() == Level.OVERWORLD
                                && client.player.position().distanceTo(stand) < 0.01D
                                && client.player.getVehicle() == null,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    // Third person is what F5 shows a rider: the carpet under them, not only the
                    // horizon.
                    client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    client.options.fov().set(NORMAL_FOV);
                    client.gui.hud.getChat().clearMessages(false);
                    client.player.getInventory().setSelectedSlot(0);
                });
        ClientGameTestWaits.waitForChunkWindow(context, RENDER_DISTANCE - 2, LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);
        ClientGameTestPlayerActions.lookAt(context, carpetPoint(site));
        ClientGameTestPlayerActions.requireVanillaInteractionRanges(context);

        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        ClientGameTestPlayerActions.walkTo(context, carpetPoint(site), MOUNT_REACH);
        ClientGameTestPlayerActions.lookAt(context, carpetPoint(site));

        ClientGameTestPlayerActions.requireLookingAtEntity(context, carpetId[0]);
        context.getInput().pressKey(options -> options.keyUse);
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> player(minecraftServer).getVehicle() != null,
                MOUNT_TIMEOUT_TICKS,
                "the right click mounts the carpet");
        context.waitFor(
                client ->
                        client.player.getVehicle() != null
                                && client.player.getVehicle().getId() == carpetId[0],
                MOUNT_TIMEOUT_TICKS);
        context.runOnClient(
                client ->
                        check(
                                !client.player.getVehicle().isLocalInstanceAuthoritative(),
                                "A carpet must be server-authoritative on its rider's client"));
        ClientGameTestWaits.holdFor(context, MOUNT_MILLIS);

        // Aim north and a little down, so the carpet sits in the lower frame and the ground shows
        // the speed.
        ClientGameTestPlayerActions.lookAt(context, flightAim(site));
        context.getInput().holdKey(options -> options.keyUp);
        try {
            ClientGameTestWaits.holdFor(context, FLY_MILLIS);
            double before = alongZ(server);
            double ticksBefore = gameTime(server);
            context.waitTicks(SPEED_WINDOW_TICKS);
            double measured = Math.abs(alongZ(server) - before) / (gameTime(server) - ticksBefore);
            server.runOnServer(
                    minecraftServer ->
                            check(
                                    Math.abs(measured - topSpeed) <= topSpeed * SPEED_TOLERANCE,
                                    type
                                            + " must fly at "
                                            + topSpeed * 20
                                            + " blocks/s, measured "
                                            + measured * 20
                                            + " blocks/s"));
            context.getInput().holdKey(options -> options.keyJump);
            ClientGameTestWaits.holdFor(context, CLIMB_MILLIS);
            context.getInput().releaseKey(options -> options.keyJump);
            server.runOnServer(
                    minecraftServer ->
                            check(
                                    player(minecraftServer).getVehicle().getY() > CARPET_Y + 1.0D,
                                    "Held jump must lift " + type));
        } finally {
            context.getInput().releaseKey(options -> options.keyJump);
        }
        ClientGameTestWaits.holdFor(context, LEVEL_MILLIS);
        context.getInput().releaseKey(options -> options.keyUp);
        double movingOnRelease = alongZ(server);
        double releasedAt = gameTime(server);
        ClientGameTestWaits.holdFor(context, 100L);
        double coasted =
                Math.abs(alongZ(server) - movingOnRelease) / (gameTime(server) - releasedAt);
        server.runOnServer(
                minecraftServer ->
                        check(
                                coasted > topSpeed * 0.2D,
                                "Letting go must not stop "
                                        + type
                                        + " at once; it coasted at "
                                        + coasted * 20
                                        + " blocks/s"));
        ClientGameTestWaits.holdFor(context, COAST_MILLIS);

        ClientGameTestWaits.holdFor(context, AFTER_MILLIS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    check(
                            player.getVehicle() != null && player.getVehicle().getType() == type,
                            "The rider must stay on " + type + " through the coast");
                    checkNoItemsAt(player.level(), site);
                });
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    private static Vec3 tierPoint(double x) {
        return new Vec3(x, CARPET_Y + 0.15D, TIERS_Z);
    }

    private static Vec3 carpetPoint(int site) {
        return new Vec3(site + 0.5D, CARPET_Y + 0.15D, 0.5D);
    }

    /**
     * Straight north from the carpet and level with it: the rider looks a little down, the carpet
     * fills the lower frame, and the jump key climbs at half the tier's speed.
     */
    private static Vec3 flightAim(int site) {
        return new Vec3(site + 0.5D, CARPET_Y, -7.5D);
    }

    private static double alongZ(TestDedicatedServerContext server) {
        return server.computeOnServer(
                minecraftServer -> player(minecraftServer).getVehicle().getZ());
    }

    private static double gameTime(TestDedicatedServerContext server) {
        return server.computeOnServer(
                minecraftServer -> player(minecraftServer).level().getGameTime());
    }

    private static void caption(ClientGameTestContext context, String id) {
        String[] text = CAPTIONS.get(id);
        check(text != null, "No caption is recorded for stage " + id);
        ClientGameTestRecorder.showStep(context, id, text[0], text[1]);
        context.waitTicks(CAPTION_LEAD_TICKS);
    }

    private static Map<String, String[]> loadCaptions() {
        String resource = "/magic_carpet-captions.json";
        try (InputStream stream = MagicCarpetShowcase.class.getResourceAsStream(resource)) {
            check(stream != null, "The recording captions are missing: " + resource);
            JsonObject root =
                    JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            Map<String, String[]> captions = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                JsonObject text = entry.getValue().getAsJsonObject();
                captions.put(
                        entry.getKey(),
                        new String[] {
                            text.get("title").getAsString(), text.get("subtitle").getAsString()
                        });
            }
            return Map.copyOf(captions);
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static void resetPlayer(ServerPlayer player) {
        player.stopRiding();
        player.getInventory().clearContent();
        player.getInventory().setSelectedSlot(0);
        player.setGameMode(GameType.CREATIVE);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0F);
        player.setDeltaMovement(Vec3.ZERO);
        player.clearFire();
        player.removeAllEffects();
    }

    private static AABB siteBox(int site) {
        return new AABB(site - 10, FLOOR_Y - 2, -12, site + 10, FLOOR_Y + 24, 12);
    }

    private static void checkNoItemsAt(ServerLevel level, int site) {
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, siteBox(site));
        check(items.isEmpty(), "No drop may stay in the world at site " + site + ": " + items);
    }

    private static ServerPlayer player(MinecraftServer server) {
        return server.getPlayerList().getPlayers().getFirst();
    }

    private static void command(MinecraftServer server, String command) {
        CommandSourceStack source =
                server.createCommandSourceStack()
                        .withPermission(PermissionSet.ALL_PERMISSIONS)
                        .withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
