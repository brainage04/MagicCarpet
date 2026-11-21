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
import net.minecraft.world.phys.Vec3;

/**
 * Photographs each tier's top face from straight above, in the real client. It does nothing unless
 * the system property {@value #DIRECTORY_PROPERTY} names a directory, and writes {@code
 * <tier>-top.png} there.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetTopCapture {
    public static final String DIRECTORY_PROPERTY = "magic_carpet.top_screenshots.directory";

    /** The platform the carpet hangs over, so the shot has a plain background instead of sky. */
    private static final int FLOOR_Y = 100;

    private static final int FLOOR_RADIUS = 4;

    private static final double CARPET_Y = FLOOR_Y + 1.5D;

    /** The camera's height above the carpet. */
    private static final double CAMERA_HEIGHT = 2.0D;

    /**
     * The field of view the capture uses. At the camera's height the carpet fills about four tenths
     * of a 70 degree frame, so the narrower view brings it to about four fifths.
     */
    private static final int CAPTURE_FOV = 40;

    /** The field of view the rest of the client GameTest runs at. */
    private static final int NORMAL_FOV = 70;

    /** One site per tier, clear of the showcase's runway sites. */
    private static final int FIRST_SITE = 600;

    private static final int SITE_SPACING = 20;

    private static final int LOAD_TIMEOUT_TICKS = 2400;

    private MagicCarpetTopCapture() {}

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
                        for (int dx = -FLOOR_RADIUS; dx <= FLOOR_RADIUS; dx++) {
                            for (int dz = -FLOOR_RADIUS; dz <= FLOOR_RADIUS; dz++) {
                                level.setBlockAndUpdate(
                                        new BlockPos(site + dx, FLOOR_Y, site + dz),
                                        Blocks.SMOOTH_QUARTZ.defaultBlockState());
                            }
                        }
                        player.teleportTo(
                                level,
                                site + 0.5D,
                                CARPET_Y + CAMERA_HEIGHT,
                                site + 0.5D,
                                Set.of(),
                                180.0F,
                                90.0F,
                                true);
                        MagicCarpetEntity carpet =
                                (MagicCarpetEntity)
                                        type.create(level, EntitySpawnReason.COMMAND);
                        if (carpet == null) {
                            throw new AssertionError("Expected a registered carpet for " + type);
                        }
                        carpet.setPos(site + 0.5D, CARPET_Y, site + 0.5D);
                        if (!level.addFreshEntity(carpet)) {
                            throw new AssertionError("Failed to spawn " + type);
                        }
                    });
            Vec3 camera = new Vec3(site + 0.5D, CARPET_Y + CAMERA_HEIGHT, site + 0.5D);
            context.waitFor(
                    client ->
                            client.level != null
                                    && client.player != null
                                    && client.player.position().distanceTo(camera) < 0.01D,
                    LOAD_TIMEOUT_TICKS);
            ClientGameTestWaits.waitForChunkWindow(context, 4, LOAD_TIMEOUT_TICKS);
            ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);
            context.runOnClient(
                    client -> {
                        // The HUD, the recording panel included, would cover the carpet.
                        client.gui.hud.toggle();
                        client.options.fov().set(CAPTURE_FOV);
                    });
            context.waitTicks(2);
            Path shot =
                    context.takeScreenshot(
                            TestScreenshotOptions.of(names[index] + "-top")
                                    .disableCounterPrefix()
                                    .withDestinationDir(output));
            context.runOnClient(
                    client -> {
                        client.gui.hud.toggle();
                        client.options.fov().set(NORMAL_FOV);
                    });
            MagicCarpet.LOGGER.info("Top capture of the {} carpet: {}", names[index], shot);
        }
    }
}
