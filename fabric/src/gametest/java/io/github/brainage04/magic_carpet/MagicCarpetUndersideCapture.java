package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.minecraftmodconventions.ClientGameTestWaits;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Photographs each tier from below, from the side, and from above at the front and at the back, in
 * the real client, so the joints between the body's segments, the holes and the symmetry of the top
 * texture can be looked at. It does nothing unless the system property {@value #DIRECTORY_PROPERTY}
 * names a directory, and writes {@code <tier>-underside.png}, {@code <tier>-side.png}, {@code
 * <tier>-front.png} and {@code <tier>-back.png} there. The carpet heads towards +z at yaw 0, so the
 * front shot looks north from the +z end.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetUndersideCapture {
    public static final String DIRECTORY_PROPERTY = "magic_carpet.underside_screenshots.directory";

    /** The plain ceiling the carpet hangs under, so the shot has a background instead of sky. */
    private static final int CEILING_Y = 100;

    /** The lit floor below it, which lights the carpet's underside and stays below the camera. */
    private static final int FLOOR_Y = 92;

    private static final int RADIUS = 6;

    private static final double CARPET_Y = CEILING_Y - 1.5D;

    /**
     * The camera's distance below the carpet, in blocks. The body is 2 blocks long, and the default
     * 70 degree field of view covers 2 * 1.5 * tan(35) = 2.1 blocks at this distance, so the body
     * fits the frame and each joint is several pixels wide; the tassels run past the top and bottom
     * edges.
     */
    private static final double CAMERA_DISTANCE = 1.5D;

    /**
     * The top views' camera: above the carpet and beyond one end of it, looking at its middle. The
     * carpet hangs 1.5 blocks under the ceiling, so the camera stays under it.
     */
    private static final double TOP_VIEW_HEIGHT = 1.2D;

    private static final double TOP_VIEW_DISTANCE = 1.5D;
    private static final float TOP_VIEW_PITCH =
            (float) Math.toDegrees(Math.atan2(TOP_VIEW_HEIGHT, TOP_VIEW_DISTANCE));

    /** One site per tier, clear of the showcase's runway sites. */
    private static final int FIRST_SITE = 700;

    private static final int SITE_SPACING = 20;

    private static final int LOAD_TIMEOUT_TICKS = 2400;

    private MagicCarpetUndersideCapture() {}

    public static void capture(ClientGameTestContext context, TestDedicatedServerContext server) {
        String directory = System.getProperty(DIRECTORY_PROPERTY);
        if (directory == null || directory.isBlank()) {
            return;
        }
        Path output = Path.of(directory);
        List<EntityType<MagicCarpetEntity>> types =
                List.of(ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary());
        String[] names = {"basic", "advanced", "legendary"};
        for (int index = 0; index < types.size(); index++) {
            int site = FIRST_SITE + SITE_SPACING * index;
            EntityType<MagicCarpetEntity> type = types.get(index);
            server.runOnServer(
                    minecraftServer -> {
                        ServerPlayer player =
                                minecraftServer.getPlayerList().getPlayers().getFirst();
                        ServerLevel level = player.level();
                        player.stopRiding();
                        player.setGameMode(GameType.SPECTATOR);
                        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                                level.setBlockAndUpdate(
                                        new BlockPos(site + dx, CEILING_Y, site + dz),
                                        Blocks.SMOOTH_QUARTZ.defaultBlockState());
                                level.setBlockAndUpdate(
                                        new BlockPos(site + dx, FLOOR_Y, site + dz),
                                        Blocks.GLOWSTONE.defaultBlockState());
                            }
                        }
                        MagicCarpetEntity carpet =
                                (MagicCarpetEntity) type.create(level, EntitySpawnReason.COMMAND);
                        if (carpet == null) {
                            throw new AssertionError("Expected a registered carpet for " + type);
                        }
                        carpet.setPos(site + 0.5D, CARPET_Y, site + 0.5D);
                        if (!level.addFreshEntity(carpet)) {
                            throw new AssertionError("Failed to spawn " + type);
                        }
                    });
            shoot(
                    context,
                    server,
                    type,
                    site,
                    names[index] + "-underside",
                    0.0D,
                    -CAMERA_DISTANCE,
                    0.0D,
                    180.0F,
                    -90.0F,
                    output);
            shoot(
                    context,
                    server,
                    type,
                    site,
                    names[index] + "-side",
                    0.0D,
                    -0.5D,
                    -1.5D,
                    0.0F,
                    -20.0F,
                    output);
            shoot(
                    context,
                    server,
                    type,
                    site,
                    names[index] + "-front",
                    0.0D,
                    TOP_VIEW_HEIGHT,
                    TOP_VIEW_DISTANCE,
                    180.0F,
                    TOP_VIEW_PITCH,
                    output);
            shoot(
                    context,
                    server,
                    type,
                    site,
                    names[index] + "-back",
                    0.0D,
                    TOP_VIEW_HEIGHT,
                    -TOP_VIEW_DISTANCE,
                    0.0F,
                    TOP_VIEW_PITCH,
                    output);
        }
    }

    private static void shoot(
            ClientGameTestContext context,
            TestDedicatedServerContext server,
            EntityType<MagicCarpetEntity> type,
            int site,
            String name,
            double dx,
            double dy,
            double dz,
            float yaw,
            float pitch,
            Path output) {
        double[] eye = new double[1];
        server.runOnServer(
                minecraftServer ->
                        eye[0] =
                                minecraftServer
                                        .getPlayerList()
                                        .getPlayers()
                                        .getFirst()
                                        .getEyeHeight());
        Vec3 camera = new Vec3(site + 0.5D + dx, CARPET_Y + dy - eye[0], site + 0.5D + dz);
        Vec3 carpetAt = new Vec3(site + 0.5D, CARPET_Y, site + 0.5D);
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    player.teleportTo(
                            player.level(),
                            camera.x,
                            camera.y,
                            camera.z,
                            Set.of(),
                            yaw,
                            pitch,
                            true);
                });
        context.waitFor(
                client ->
                        client.level != null
                                && client.player != null
                                && client.player.position().distanceTo(camera) < 0.01D
                                && client
                                        .level
                                        .getEntitiesOfClass(
                                                MagicCarpetEntity.class,
                                                new AABB(carpetAt, carpetAt).inflate(2.0D))
                                        .stream()
                                        .anyMatch(carpet -> carpet.getType() == type),
                LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForChunkWindow(context, 4, LOAD_TIMEOUT_TICKS);
        ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);
        Path shot =
                context.takeScreenshot(
                        TestScreenshotOptions.of(name)
                                .disableCounterPrefix()
                                .withDestinationDir(output));
        MagicCarpet.LOGGER.info("Capture {}: {}", name, shot);
    }
}
