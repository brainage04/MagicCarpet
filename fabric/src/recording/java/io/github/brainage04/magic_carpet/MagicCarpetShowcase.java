package io.github.brainage04.magic_carpet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.brainage04.magic_carpet.client.MagicCarpetFlightSound;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.CarpetMotion;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.CameraType;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The film of Magic Carpet's showcase: the three tiers side by side, then each tier twice, first
 * sneaking and right clicking it, which leaves it empty, then right clicking it, mounting it and
 * flying it.
 *
 * <p>Every stage plays like a person: the player looks at the carpet it rides from the stage's
 * first frame, presses the use key with that carpet under the crosshair and inside vanilla reach,
 * and flies it with the forward and jump keys. {@link ClientGameTestPlayerActions} fails the stage
 * when the crosshair is not on the carpet or the carpet is out of reach.
 *
 * <p>The ground is a saved structure, {@code magic_carpet-runway.nbt} (written by {@code
 * tools/generate_showcase_runway.py}), placed once before the client joins. Server-side calls only
 * place it, put the player and a carpet in position before a stage's caption, and assert what the
 * camera cannot prove.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetShowcase {
    private static final int FLOOR_Y = 100;
    private static final int STAND_Y = FLOOR_Y + 1;
    private static final int RENDER_DISTANCE = 12;

    private static final float NORTH = 180.0F;
    private static final int NORMAL_FOV = 70;

    private static final String RUNWAY = "/magic_carpet-runway.nbt";

    /** The runway's north and south ends: every flight stays over it for its whole length. */
    private static final int RUNWAY_FAR_Z = -400;

    private static final int RUNWAY_NEAR_Z = 8;
    private static final int RUNWAY_HALF_WIDTH = 8;

    /** Where the three tiers hang for the first stage, and where the player stands to see them. */
    private static final double TIERS_Z = 0.5D;

    private static final double TIER_SPACING = 2.0D;
    private static final Vec3 TIERS_STAND = new Vec3(0.0D, STAND_Y, 3.8D);
    private static final long TIERS_MILLIS = 2500L;

    /**
     * One site per tier, far enough apart that a stage never fights the last one's scene. The tiers
     * stage shares the basic site.
     */
    private static final int BASIC_SITE = 0;

    private static final int ADVANCED_SITE = 200;
    private static final int LEGENDARY_SITE = 400;

    /** The carpet's height above the platform. */
    private static final double CARPET_Y = STAND_Y + 0.5D;

    private static final double CARPET_Z = 0.5D;

    /**
     * How far south of the carpet the player stands: inside vanilla entity reach, and far enough
     * that looking at the carpet is a shallow look down, so the jump key climbs (a pitch of 19
     * degrees, against 45 for a dive).
     */
    private static final double MOUNT_DISTANCE = 2.8D;

    /** The hotbar slot the player holds: empty, so the hand stays out of the frame. */
    private static final int EMPTY_SLOT = 8;

    private static final long BEFORE_MILLIS = 300L;

    /** How long the sneaking click is held on screen after the press, the carpet still empty. */
    private static final long SNEAK_CLICK_MILLIS = 1300L;

    /** How long the first-person view keeps the crosshair on the carpet after the right click. */
    private static final long CROSSHAIR_MILLIS = 250L;

    private static final long MOUNT_MILLIS = 150L;
    private static final long FLY_MILLIS = 1500L;

    /**
     * The climb after the level run. Its vertical speed ramps up like the horizontal one, so the
     * lift is about a fifth of the top speed for the first half second.
     */
    private static final long CLIMB_MILLIS = 900L;

    private static final long LEVEL_MILLIS = 250L;
    private static final long COAST_MILLIS = 500L;

    /** Ticks the rider takes to look down to the pitch that flies the carpet down. */
    private static final int LOOK_DOWN_TICKS = 8;

    private static final int LAND_TIMEOUT_TICKS = 200;

    /** How long the landed carpet stays on screen before its rider gets off. */
    private static final long LANDED_MILLIS = 300L;

    /** How long the player stays on screen after getting off. */
    private static final long STEPPED_OFF_MILLIS = 800L;

    /** Ticks the flight speed is measured over, once the carpet has reached its top speed. */
    private static final int SPEED_WINDOW_TICKS = 20;

    /** The most the measured top speed may differ from the tier's, as a share of that speed. */
    private static final double SPEED_TOLERANCE = 0.05D;

    /**
     * The widest gap, in blocks, between where a frame draws the carpet and where it draws the
     * rider. The rider sits over the carpet's centre, so the gap is 0 when both move together.
     * Without an interpolation handler the carpet is drawn three ticks of flight ahead: 1.8, 3.6
     * and 7.2 blocks for the three tiers (measured 12 Nov 2026).
     */
    private static final double MAX_RIDER_GAP = 0.01D;

    /**
     * The most the carpet's pitch or roll may change in one tick, in degrees. A tilt read from the
     * server's velocity updates turns by up to 4.4 degrees in one tick on the basic carpet and 7.1
     * on the legendary one, and the eased tilt by at most 2.4 (measured 12 Nov 2026).
     */
    private static final float MAX_TILT_STEP = 4.0F;

    /**
     * The fewest ticks of a flight in which the carpet must move, so the checks are not empty: at
     * least the speed window, the one part of the flight counted in ticks rather than by the clock.
     */
    private static final int MIN_MOVING_TICKS = SPEED_WINDOW_TICKS;

    private static final int CAPTION_LEAD_TICKS = 4;
    private static final int HOLD_AFTER_END_TICKS = 6;
    private static final int LOAD_TIMEOUT_TICKS = 2400;
    private static final int MOUNT_TIMEOUT_TICKS = 100;
    private static final int SNEAK_TIMEOUT_TICKS = 40;

    private static final Map<String, String[]> CAPTIONS = loadCaptions();

    private static final SoundLog SOUNDS = new SoundLog();

    private MagicCarpetShowcase() {}

    /** Server properties the film wants: chunks out to its horizon. */
    public static Properties serverProperties(Properties properties) {
        properties.setProperty("view-distance", Integer.toString(RENDER_DISTANCE));
        return properties;
    }

    /** Places the saved runway under each tier's site, before the client joins. */
    public static void buildStage(MinecraftServer server) {
        ServerLevel level = server.overworld();
        StructureTemplate runway = loadRunway(server);
        for (int site : List.of(BASIC_SITE, ADVANCED_SITE, LEGENDARY_SITE)) {
            BlockPos corner = new BlockPos(site - RUNWAY_HALF_WIDTH, FLOOR_Y, RUNWAY_FAR_Z);
            check(
                    runway.placeInWorld(
                            level,
                            corner,
                            corner,
                            new StructurePlaceSettings(),
                            level.getRandom(),
                            Block.UPDATE_CLIENTS),
                    "The runway must place at site " + site);
            check(
                    level.getBlockState(new BlockPos(site, FLOOR_Y, RUNWAY_NEAR_Z))
                                    .is(Blocks.SMOOTH_QUARTZ)
                            && level.getBlockState(new BlockPos(site, FLOOR_Y, -4))
                                    .is(Blocks.CONCRETE.black()),
                    "The runway at site " + site + " must be striped quartz and black concrete");
        }
        command(server, "gamerule advance_time false");
        command(server, "gamerule advance_weather false");
        command(server, "gamerule spawn_mobs false");
        command(server, "weather clear");
        command(server, "time set noon");
    }

    /** Films the stages, in order. */
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
        context.runOnClient(client -> client.getSoundManager().addListener(SOUNDS));
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
        context.runOnClient(
                client -> {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.getSoundManager().removeListener(SOUNDS);
                });
    }

    /**
     * The three tiers side by side, close enough that the shapes and the textures read, all in
     * frame from where the player stands.
     */
    private static void tiers(ClientGameTestContext context, TestDedicatedServerContext server) {
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    ServerLevel level = player.level();
                    resetPlayer(player);
                    player.getInventory().setItem(0, new ItemStack(ModItems.basic()));
                    player.getInventory().setItem(1, new ItemStack(ModItems.advanced()));
                    player.getInventory().setItem(2, new ItemStack(ModItems.legendary()));
                    player.teleportTo(
                            level,
                            TIERS_STAND.x,
                            TIERS_STAND.y,
                            TIERS_STAND.z,
                            Set.of(),
                            NORTH,
                            8.0F,
                            true);
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
        arrive(context, TIERS_STAND);
        ClientGameTestPlayerActions.lookAt(context, tierPoint(0.0D));

        String id = "magic_carpet.tiers";
        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, TIERS_MILLIS);
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
     * One tier, in two stages from the same spot. First the player sneaks and right clicks the
     * carpet, which stays empty. Then it right clicks it, mounts it, flies it with the forward and
     * jump keys, lets go to watch it coast, looks down and holds jump to land it on the runway, and
     * gets off with the sneak key. The next stage teleports the player before its caption.
     */
    private static void flight(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            int site,
            String id,
            EntityType<?> type,
            Item item,
            double topSpeed) {
        Vec3 stand = new Vec3(site + 0.5D, STAND_Y, CARPET_Z + MOUNT_DISTANCE);
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
                    carpet.setPos(site + 0.5D, CARPET_Y, CARPET_Z);
                    check(level.addFreshEntity(carpet), "Failed to spawn " + type);
                    carpetId[0] = carpet.getId();
                });
        arrive(context, stand);
        ClientGameTestPlayerActions.lookAt(context, carpetPoint(site));
        ClientGameTestPlayerActions.requireVanillaInteractionRanges(context);

        context.runOnClient(client -> SOUNDS.clear());
        String sneakId = id + ".sneak";
        caption(context, sneakId);
        ClientGameTestStages.markStage(sneakId);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
        context.getInput().holdKey(options -> options.keyShift);
        try {
            ClientGameTestWaits.waitForServer(
                    context,
                    server,
                    minecraftServer -> player(minecraftServer).isShiftKeyDown(),
                    SNEAK_TIMEOUT_TICKS,
                    "the sneak key reaches the server");
            ClientGameTestPlayerActions.requireLookingAtEntity(context, carpetId[0]);
            context.getInput().pressKey(options -> options.keyUse);
            ClientGameTestWaits.holdFor(context, SNEAK_CLICK_MILLIS);
            server.runOnServer(
                    minecraftServer ->
                            check(
                                    player(minecraftServer).getVehicle() == null,
                                    "Sneaking and right clicking must leave " + type + " empty"));
            context.runOnClient(
                    client ->
                            check(
                                    !SOUNDS.heard(SoundEvents.WOOL_PLACE) && SOUNDS.flight == null,
                                    "A carpet left empty must play neither the mount sound nor"
                                            + " the wind"));
        } finally {
            context.getInput().releaseKey(options -> options.keyShift);
        }
        ClientGameTestWaits.waitForServer(
                context,
                server,
                minecraftServer -> !player(minecraftServer).isShiftKeyDown(),
                SNEAK_TIMEOUT_TICKS,
                "the released sneak key reaches the server");
        ClientGameTestStages.endStage(context, sneakId, HOLD_AFTER_END_TICKS);

        caption(context, id);
        ClientGameTestStages.markStage(id);
        ClientGameTestWaits.holdFor(context, BEFORE_MILLIS);
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
        context.waitFor(
                client -> SOUNDS.heard(SoundEvents.WOOL_PLACE) && SOUNDS.flight != null,
                MOUNT_TIMEOUT_TICKS);
        // The crosshair is on the carpet in first person, and stays there for a moment after the
        // mount, so the film shows what the player clicked.
        ClientGameTestWaits.holdFor(context, CROSSHAIR_MILLIS);
        context.runOnClient(
                client -> {
                    // Third person is what F5 shows a rider: the carpet under them, not only the
                    // horizon.
                    client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                });
        ClientGameTestWaits.holdFor(context, MOUNT_MILLIS);

        // The player keeps the direction it clicked in: north, and a little down, so the carpet
        // sits in the lower frame and the ground shows the speed.
        RiderWatch watch =
                new RiderWatch(carpetId[0], context.computeOnClient(client -> SOUNDS.flight));
        context.getInput().holdKey(options -> options.keyUp);
        try {
            watch.holdFor(context, FLY_MILLIS);
            double before = alongZ(server);
            double ticksBefore = gameTime(server);
            watch.waitTicks(context, SPEED_WINDOW_TICKS);
            double measured = Math.abs(alongZ(server) - before) / (gameTime(server) - ticksBefore);
            check(
                    Math.abs(measured - topSpeed) <= topSpeed * SPEED_TOLERANCE,
                    type
                            + " must fly at "
                            + topSpeed * 20
                            + " blocks/s, measured "
                            + measured * 20
                            + " blocks/s");
            context.getInput().holdKey(options -> options.keyJump);
            watch.holdFor(context, CLIMB_MILLIS);
            context.getInput().releaseKey(options -> options.keyJump);
            server.runOnServer(
                    minecraftServer -> {
                        double height = player(minecraftServer).getVehicle().getY();
                        check(
                                height > CARPET_Y + 1.0D,
                                "Held jump must lift " + type + ", held at " + height);
                    });
        } finally {
            context.getInput().releaseKey(options -> options.keyJump);
        }
        watch.holdFor(context, LEVEL_MILLIS);
        context.getInput().releaseKey(options -> options.keyUp);
        double movingOnRelease = alongZ(server);
        double releasedAt = gameTime(server);
        watch.holdFor(context, 100L);
        double coasted =
                Math.abs(alongZ(server) - movingOnRelease) / (gameTime(server) - releasedAt);
        check(
                coasted > topSpeed * 0.2D,
                "Letting go must not stop "
                        + type
                        + " at once; it coasted at "
                        + coasted * 20
                        + " blocks/s");
        assertAPassengerUpdateKeepsOneWind(context, watch);
        watch.holdFor(context, COAST_MILLIS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    check(
                            player.getVehicle() != null && player.getVehicle().getType() == type,
                            "The rider must stay on " + type + " through the coast");
                    checkNoItemsAt(player.level(), site);
                });

        float fromPitch = context.computeOnClient(client -> client.player.getXRot());
        for (int tick = 1; tick <= LOOK_DOWN_TICKS; tick++) {
            float pitch =
                    Mth.lerp((float) tick / LOOK_DOWN_TICKS, fromPitch, CarpetMotion.DIVE_PITCH);
            context.runOnClient(client -> client.player.setXRot(pitch));
            watch.tick(context);
        }
        context.getInput().holdKey(options -> options.keyJump);
        try {
            watch.untilServer(
                    context,
                    server,
                    minecraftServer -> player(minecraftServer).getVehicle().onGround(),
                    LAND_TIMEOUT_TICKS,
                    type + " lands");
        } finally {
            context.getInput().releaseKey(options -> options.keyJump);
        }
        server.runOnServer(
                minecraftServer -> {
                    Entity carpet = player(minecraftServer).getVehicle();
                    check(
                            Math.abs(carpet.getY() - STAND_Y) < 1.0E-3D
                                    && !carpet.level()
                                            .getBlockState(carpet.blockPosition().below())
                                            .isAir(),
                            type + " must land on the runway, landed at " + carpet.position());
                });
        watch.holdFor(context, LANDED_MILLIS);
        watch.verify(type, topSpeed);

        // The rider gets off with the sneak key, which plays the dismount sound and stops the
        // wind.
        context.getInput().holdKey(options -> options.keyShift);
        try {
            context.waitFor(client -> client.player.getVehicle() == null, MOUNT_TIMEOUT_TICKS);
        } finally {
            context.getInput().releaseKey(options -> options.keyShift);
        }
        context.waitFor(
                client ->
                        SOUNDS.heard(SoundEvents.WOOL_STEP)
                                && !client.getSoundManager().isActive(watch.sound),
                MOUNT_TIMEOUT_TICKS);
        ClientGameTestWaits.holdFor(context, STEPPED_OFF_MILLIS);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = player(minecraftServer);
                    check(
                            player.getVehicle() == null
                                    && player.onGround()
                                    && player.getY() >= STAND_Y - 1.0E-3D,
                            "The rider must get off onto the landed "
                                    + type
                                    + " or the runway, stood at "
                                    + player.position());
                });
        ClientGameTestStages.endStage(context, id, HOLD_AFTER_END_TICKS);
    }

    /**
     * A seat change - another player or a mob boarding or leaving, or a {@code /ride} - makes the
     * server send the carpet's passengers again, and the client answers by ejecting and re-mounting
     * every passenger in that one packet (ClientPacketListener.java:1140-1152 in 26.3). The rider
     * must keep hearing one wind, not one per seat change. The handler runs here on the client
     * thread, where packet processing runs it (PacketProcessor.java:22-24), so no second rider
     * shows in the film.
     */
    private static void assertAPassengerUpdateKeepsOneWind(
            ClientGameTestContext context, RiderWatch watch) {
        int before = context.computeOnClient(client -> SOUNDS.flightStarts);
        context.runOnClient(
                client -> {
                    Entity carpet = client.player.getVehicle();
                    check(carpet != null, "The rider must hold a carpet at a passenger update");
                    client.getConnection()
                            .handleSetEntityPassengersPacket(
                                    new ClientboundSetPassengersPacket(carpet));
                    check(
                            client.player.getVehicle() == carpet,
                            "A passenger update must leave the rider on its carpet");
                    check(
                            SOUNDS.flightStarts == before,
                            "A passenger update must keep one wind; it started "
                                    + (SOUNDS.flightStarts - before)
                                    + " more");
                    check(
                            client.getSoundManager().isActive(watch.sound),
                            "The wind must keep playing through a passenger update");
                });
    }

    /**
     * Watches a flight each tick, as a frame would draw it: where the carpet is drawn against its
     * rider at both ends of the tick, and how far the carpet's tilt turned in the tick.
     */
    private static final class RiderWatch {
        private final int carpetId;
        private final MagicCarpetFlightSound sound;
        private double widestGap;
        private float steepestTiltStep;
        private int movingTicks;
        private float loudest;
        private float volumeError;

        private RiderWatch(int carpetId, MagicCarpetFlightSound sound) {
            this.carpetId = carpetId;
            this.sound = sound;
        }

        private void holdFor(ClientGameTestContext context, long millis) {
            long end = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < end) {
                tick(context);
            }
        }

        private void waitTicks(ClientGameTestContext context, int ticks) {
            for (int tick = 0; tick < ticks; tick++) {
                tick(context);
            }
        }

        private void untilServer(
                ClientGameTestContext context,
                TestDedicatedServerContext server,
                Predicate<MinecraftServer> condition,
                int timeoutTicks,
                String description) {
            for (int tick = 0; !server.computeOnServer(condition::test); tick++) {
                check(tick < timeoutTicks, "Timed out waiting until " + description);
                tick(context);
            }
        }

        private void tick(ClientGameTestContext context) {
            context.waitTick();
            context.runOnClient(
                    client -> {
                        Entity vehicle = client.player.getVehicle();
                        check(
                                vehicle instanceof MagicCarpetEntity
                                        && vehicle.getId() == this.carpetId,
                                "The player must ride the carpet through its flight");
                        MagicCarpetEntity carpet = (MagicCarpetEntity) vehicle;
                        // The frame draws both at a point between the start and the end of the
                        // tick, and both move in a straight line, so the two ends bound the gap.
                        for (float partialTick : new float[] {0.0F, 1.0F}) {
                            double gap =
                                    carpet.getPosition(partialTick)
                                            .subtract(client.player.getPosition(partialTick))
                                            .horizontalDistance();
                            this.widestGap = Math.max(this.widestGap, gap);
                        }
                        if (carpet.getPosition(0.0F).distanceToSqr(carpet.position()) > 1.0E-6D) {
                            this.movingTicks++;
                        }
                        float tiltStep =
                                Math.max(
                                        Math.abs(carpet.renderPitch - carpet.prevRenderPitch),
                                        Math.abs(carpet.renderRoll - carpet.prevRenderRoll));
                        this.steepestTiltStep = Math.max(this.steepestTiltStep, tiltStep);
                        check(
                                client.getSoundManager().isActive(this.sound),
                                "The wind must play through the flight");
                        float volume = this.sound.speedVolume();
                        this.loudest = Math.max(this.loudest, volume);
                        this.volumeError =
                                Math.max(
                                        this.volumeError,
                                        Math.abs(
                                                volume
                                                        - MagicCarpetFlightSound.volumeFor(
                                                                carpet.getDrawnMovement()
                                                                        .lengthSqr())));
                    });
        }

        private void verify(EntityType<?> type, double topSpeed) {
            MagicCarpet.LOGGER.info(
                    "Flight of {}: moved in {} ticks, widest carpet-to-rider gap {} blocks,"
                            + " steepest tilt step {} degrees, loudest wind {}, wind volume off by"
                            + " up to {}",
                    type,
                    this.movingTicks,
                    this.widestGap,
                    this.steepestTiltStep,
                    this.loudest,
                    this.volumeError);
            check(
                    this.movingTicks >= MIN_MOVING_TICKS,
                    type + " must move in at least " + MIN_MOVING_TICKS + " ticks of its flight");
            check(
                    this.widestGap <= MAX_RIDER_GAP,
                    "Every frame must draw "
                            + type
                            + " under its rider; they were drawn "
                            + this.widestGap
                            + " blocks apart");
            check(
                    this.steepestTiltStep <= MAX_TILT_STEP,
                    type
                            + " must tilt by at most "
                            + MAX_TILT_STEP
                            + " degrees a tick; it tilted "
                            + this.steepestTiltStep);
            check(
                    this.volumeError < 1.0E-4F,
                    "The wind's volume must follow the carpet's drawn speed; it was off by "
                            + this.volumeError);
            // The speed check above holds top speed to within 5 %, so the wind must reach at
            // least the volume of 90 % of that speed.
            float expected = MagicCarpetFlightSound.volumeFor(Math.pow(topSpeed * 0.9D, 2.0D));
            check(
                    this.loudest >= expected,
                    type
                            + "'s wind must reach "
                            + expected
                            + " at top speed, reached "
                            + this.loudest);
        }
    }

    /**
     * The sounds the client starts, in order, and the last carpet wind among them. The sound engine
     * tells its listeners before it applies the volume (SoundEngine.java:400-402 in 26.3), so the
     * log also works in test runs whose sound categories are at volume 0.
     */
    private static final class SoundLog implements SoundEventListener {
        private final List<Identifier> started = new ArrayList<>();
        private @Nullable MagicCarpetFlightSound flight;
        private int flightStarts;

        @Override
        public void onPlaySound(SoundInstance sound, WeighedSoundEvents events, float range) {
            this.started.add(sound.getIdentifier());
            if (sound instanceof MagicCarpetFlightSound wind) {
                this.flight = wind;
                this.flightStarts++;
            }
        }

        private boolean heard(SoundEvent event) {
            return this.started.contains(event.location());
        }

        private void clear() {
            this.started.clear();
            this.flight = null;
            this.flightStarts = 0;
        }
    }

    /** Waits until the client stands where the server put it, with the scene loaded and drawn. */
    private static void arrive(ClientGameTestContext context, Vec3 stand) {
        context.waitFor(
                client ->
                        client.level.dimension() == Level.OVERWORLD
                                && client.player.position().distanceTo(stand) < 0.01D
                                && client.player.getVehicle() == null,
                LOAD_TIMEOUT_TICKS);
        context.runOnClient(
                client -> {
                    // First person to click, so the crosshair on the carpet is on screen. A flight
                    // then switches to the chase camera.
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.options.fov().set(NORMAL_FOV);
                    client.gui.hud.getChat().clearMessages(false);
                    client.player.getInventory().setSelectedSlot(EMPTY_SLOT);
                });
        ClientGameTestWaits.waitForChunkWindow(context, RENDER_DISTANCE - 2, LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);
    }

    private static StructureTemplate loadRunway(MinecraftServer server) {
        try (InputStream stream = MagicCarpetShowcase.class.getResourceAsStream(RUNWAY)) {
            check(stream != null, "The runway structure is missing: " + RUNWAY);
            StructureTemplate template = new StructureTemplate();
            template.load(
                    server.registryAccess().lookupOrThrow(Registries.BLOCK),
                    NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap()));
            return template;
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static Vec3 tierPoint(double x) {
        return new Vec3(x, CARPET_Y + 0.15D, TIERS_Z);
    }

    private static Vec3 carpetPoint(int site) {
        return new Vec3(site + 0.5D, CARPET_Y + 0.15D, CARPET_Z);
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
        player.getInventory().setSelectedSlot(EMPTY_SLOT);
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
