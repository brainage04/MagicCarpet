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
 * Photographs each tier's underside from below, in the real client, so the joints between the body's
 * segments can be looked at. It does nothing unless the system property {@value #DIRECTORY_PROPERTY}
 * names a directory, and writes {@code <tier>-underside.png} there.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetUndersideCapture {
    public static final String DIRECTORY_PROPERTY = "magic_carpet.underside_screenshots.directory";

    /**
     * The platform the carpet hangs under: glowstone, so the shot has a plain background instead of
     * sky and the carpet's underside is lit.
     */
    private static final int CEILING_Y = 100;

    private static final int CEILING_RADIUS = 6;

    private static final double CARPET_Y = CEILING_Y - 1.5D;

    /** The camera's depth below the carpet: the carpet fills about a third of the frame. */
    private static final double CAMERA_DEPTH = 7.0D;

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
                        for (int dx = -CEILING_RADIUS; dx <= CEILING_RADIUS; dx++) {
                            for (int dz = -CEILING_RADIUS; dz <= CEILING_RADIUS; dz++) {
                                level.setBlockAndUpdate(
                                        new BlockPos(site + dx, CEILING_Y, site + dz),
                                        Blocks.GLOWSTONE.defaultBlockState());
                            }
                        }
                        player.teleportTo(
                                level,
                                site + 0.5D,
                                CARPET_Y - CAMERA_DEPTH,
                                site + 0.5D,
                                Set.of(),
                                180.0F,
                                -90.0F,
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
            Vec3 camera = new Vec3(site + 0.5D, CARPET_Y - CAMERA_DEPTH, site + 0.5D);
            Vec3 carpetAt = new Vec3(site + 0.5D, CARPET_Y, site + 0.5D);
            context.waitFor(
                    client ->
                            client.level != null
                                    && client.player != null
                                    && client.player.position().distanceTo(camera) < 0.01D
                                    && client.level.getEntitiesOfClass(
                                                    MagicCarpetEntity.class,
                                                    new AABB(carpetAt, carpetAt).inflate(2.0D))
                                            .stream()
                                            .anyMatch(carpet -> carpet.getType() == type),
                    LOAD_TIMEOUT_TICKS);
            ClientGameTestWaits.waitForChunkWindow(context, 4, LOAD_TIMEOUT_TICKS);
            ClientGameTestWaits.waitForRenderSettled(context, 15, LOAD_TIMEOUT_TICKS);
            Path shot =
                    context.takeScreenshot(
                            TestScreenshotOptions.of(names[index] + "-underside")
                                    .disableCounterPrefix()
                                    .withDestinationDir(output));
            MagicCarpet.LOGGER.info("Underside capture of the {} carpet: {}", names[index], shot);
        }
    }
}
