package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.client.ServerSupport;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.minecraftmodconventions.VanillaServerClientGameTests;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.item.ItemStack;

/**
 * Joins a plain vanilla server, which has no MagicCarpet, and checks that none of its items can be
 * taken in creative there, with the conventions' creative-item checks. Runs only under {@code
 * :fabric:runVanillaServerClientGameTest}, which starts that server; in every other client run it
 * does nothing.
 */
@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetVanillaServerClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        VanillaServerClientGameTests.checkCreativeItems(
                context,
                VanillaServerClientGameTests.CreativeItems.of(
                        "MagicCarpet",
                        ServerSupport::serverHasMagicCarpet,
                        ServerSupport::isMagicCarpetItem,
                        () -> new ItemStack(ModItems.basic())));
    }
}
