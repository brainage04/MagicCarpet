package io.github.brainage04.magic_carpet;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties = ClientGameTestServers.flatServerProperties();

        ClientGameTestServers.withDedicatedServer(context, serverProperties, "MagicCarpet entity rendering GameTest", server -> {
            server.runOnServer(minecraftServer -> prepareStage(
                    minecraftServer.getPlayerList().getPlayers().getFirst()));
            ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
            context.waitTicks(30);
            context.runOnClient(client -> verifyAnimationAndTranslations());
        
            ClientGameTestRecorder.startRecording(context);
            ClientGameTestRecorder.showStep(
                    context,
                    "magic_carpet.tiers",
                    "Magic Carpet tiers",
                    "Basic, advanced, and legendary carpet entities must all render"
            );
            context.waitTicks(80);
            verifyHeldMovement(context, server);
        });
    }

    private static void verifyAnimationAndTranslations() {
        EntityType<?>[] types = {ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()};
        String[] names = {"Basic Magic Carpet", "Advanced Magic Carpet", "Legendary Magic Carpet"};
        for (int tier = 0; tier < types.length; tier++) {
            if (!names[tier].equals(types[tier].getDescription().getString())) {
                throw new AssertionError("Missing translated entity name for " + types[tier]);
            }
            var root = MagicCarpetEntityModel.createBodyLayer(tier > 0, tier == 2).bakeRoot();
            var model = new MagicCarpetEntityModel(root);
            var state = new MagicCarpetEntityRenderState();
            state.ageInTicks = 20;
            model.setupAnim(state);
            float idle = root.getChild("carpet").getChild("segment_7").y;
            state.speed = 1;
            model.setupAnim(state);
            float flying = root.getChild("carpet").getChild("segment_7").y;
            if (Math.abs(idle - flying) < 0.01) throw new AssertionError("Flight must change the carpet's ripple");
            model.setupAnim(state);
            if (root.getChild("carpet").getChild("segment_7").y != flying) {
                throw new AssertionError("Animation must not accumulate offsets between frames");
            }
        }
    }

    private static void verifyHeldMovement(ClientGameTestContext context, TestDedicatedServerContext server) {
        EntityType<?>[] tiers = {ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()};
        double[] speeds = {0.6, 1.2, 2.4};
        for (int tier = 0; tier < tiers.length; tier++) {
            EntityType<?> type = tiers[tier];
            double speed = speeds[tier];
            server.runOnServer(minecraftServer -> {
                ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                player.stopRiding();
                MagicCarpetEntity carpet = (MagicCarpetEntity) type.create(player.level(), EntitySpawnReason.COMMAND);
                carpet.setPos(0.5, 100, 0.5);
                player.level().addFreshEntity(carpet);
                player.teleportTo(0.5, 101, 0.5);
                player.setYRot(0);
                player.setXRot(0);
                if (!player.startRiding(carpet)) throw new AssertionError("Could not mount " + type);
            });
            context.waitFor(client -> client.player != null && client.player.getVehicle() != null
                    && client.player.getVehicle().getType() == type);
            context.runOnClient(client -> {
                if (client.player.getVehicle().isLocalInstanceAuthoritative()) {
                    throw new AssertionError("A carpet must be server-authoritative on its rider's client");
                }
                client.player.setYRot(0);
                client.player.setXRot(0);
            });
            context.getInput().holdKey(options -> options.keyUp);
            context.getInput().holdKey(options -> options.keyJump);
            try {
                // No mouse movement or repeat input: exercise the actual input packet path for eight seconds.
                context.waitTicks(160);
                double[] before = server.computeOnServer(minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    return new double[]{player.getVehicle().getZ(), player.level().getGameTime()};
                });
                context.waitTicks(40);
                context.runOnClient(client -> {
                    if (!client.player.input.keyPresses.forward() || !client.player.input.keyPresses.jump()) {
                        throw new AssertionError("Client stopped holding input: " + client.player.input.keyPresses);
                    }
                });
                server.runOnServer(minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    MagicCarpetEntity carpet = (MagicCarpetEntity) player.getVehicle();
                    double measured = (carpet.getZ() - before[0]) / (player.level().getGameTime() - before[1]);
                    if (Math.abs(measured - speed) > 0.01) {
                        throw new AssertionError(type + " sustained speed: expected " + speed * 20 + ", found " + measured * 20
                                + " blocks/s; input=" + player.getLastClientInput() + ", motion=" + carpet.getDeltaMovement()
                                + ", pos=" + carpet.position() + ", rider=" + player.position() + ", ticks=" + carpet.tickCount);
                    }
                    if (Math.abs(carpet.getDeltaMovement().y - speed / 2) > 0.01) {
                        throw new AssertionError("Held jump stopped climbing for " + type);
                    }
                    MagicCarpet.LOGGER.info("Held-input speed for {}: {} blocks/s", type, measured * 20);
                });
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
                context.getInput().releaseKey(options -> options.keyJump);
                server.runOnServer(minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    if (player.getVehicle() instanceof MagicCarpetEntity carpet) {
                        player.stopRiding();
                        carpet.discard();
                    }
                });
            }
        }
    }

    private static void prepareStage(ServerPlayer player) {
        ServerLevel level = player.level();

        for (BlockPos position : BlockPos.betweenClosed(-7, 62, -8, 7, 66, 5)) {
            level.setBlock(
                    position,
                    position.getY() == 62 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    3
            );
        }

        spawn(level, ModEntities.basic().create(level, EntitySpawnReason.COMMAND), -3.0D);
        spawn(level, ModEntities.advanced().create(level, EntitySpawnReason.COMMAND), 0.0D);
        spawn(level, ModEntities.legendary().create(level, EntitySpawnReason.COMMAND), 3.0D);
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.basic()));
        player.getInventory().setItem(1, new ItemStack(ModItems.advanced()));
        player.getInventory().setItem(2, new ItemStack(ModItems.legendary()));
        player.getInventory().setSelectedSlot(0);
        player.teleportTo(0.5D, 64.0D, -6.5D);
        player.setYRot(0.0F);
        player.setXRot(12.0F);
    }

    private static void spawn(ServerLevel level, MagicCarpetEntity carpet, double x) {
        if (carpet == null) {
            throw new AssertionError("Expected a registered magic carpet entity type");
        }
        carpet.setPos(x, 63.0D, 0.0D);
        if (!level.addFreshEntity(carpet)) {
            throw new AssertionError("Failed to spawn " + carpet.getType());
        }
    }

}
