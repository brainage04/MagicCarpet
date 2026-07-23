package io.github.brainage04.magic_carpet;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

        try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
            ClientGameTestServers.connectToDedicatedServer(context, server, "MagicCarpet entity rendering GameTest");
            try {
                server.runOnServer(minecraftServer -> prepareStage(
                        minecraftServer.getPlayerList().getPlayers().getFirst()));
                ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                context.waitTicks(30);

                ClientGameTestRecorder.startRecording(context);
                ClientGameTestRecorder.showStep(
                        context,
                        "magic_carpet.tiers",
                        "Magic Carpet tiers",
                        "Basic, advanced, and legendary carpet entities must all render"
                );
                context.waitTicks(80);
            } finally {
                ClientGameTestServers.disconnectFromDedicatedServer(context);
            }
        }
    }

    private static void prepareStage(ServerPlayer player) {
        ServerLevel level = player.level();
        verifyEntityRegistration();
        verifyRecipes(level);

        for (BlockPos position : BlockPos.betweenClosed(-7, 62, -8, 7, 66, 5)) {
            level.setBlock(
                    position,
                    position.getY() == 62 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    3
            );
        }

        spawn(level, ModEntities.BASIC_MAGIC_CARPET.create(level, EntitySpawnReason.COMMAND), -3.0D);
        spawn(level, ModEntities.ADVANCED_MAGIC_CARPET.create(level, EntitySpawnReason.COMMAND), 0.0D);
        spawn(level, ModEntities.LEGENDARY_MAGIC_CARPET.create(level, EntitySpawnReason.COMMAND), 3.0D);

        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.BASIC_MAGIC_CARPET));
        player.getInventory().setItem(1, new ItemStack(ModItems.ADVANCED_MAGIC_CARPET));
        player.getInventory().setItem(2, new ItemStack(ModItems.LEGENDARY_MAGIC_CARPET));
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

    private static void verifyEntityRegistration() {
        verifyEntityRegistration(ModEntities.BASIC_MAGIC_CARPET, "basic_magic_carpet");
        verifyEntityRegistration(ModEntities.ADVANCED_MAGIC_CARPET, "advanced_magic_carpet");
        verifyEntityRegistration(ModEntities.LEGENDARY_MAGIC_CARPET, "legendary_magic_carpet");
    }

    private static void verifyEntityRegistration(EntityType<?> entityType, String path) {
        Identifier expected = Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path);
        if (!expected.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entityType))) {
            throw new AssertionError("Missing entity registration " + expected);
        }
    }

    private static void verifyRecipes(ServerLevel level) {
        for (String path : new String[]{
                "basic_magic_carpet",
                "advanced_magic_carpet",
                "legendary_magic_carpet"
        }) {
            Identifier expected = Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path);
            boolean loaded = level.getServer().getRecipeManager().getRecipes().stream()
                    .anyMatch(recipe -> recipe.id().identifier().equals(expected));
            if (!loaded) {
                throw new AssertionError("Missing recipe " + expected);
            }
        }
    }
}
