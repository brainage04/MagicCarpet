package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.client.ServerSupport;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.AdvancedMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.BasicMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.LegendaryMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.minecraftmodconventions.ClientGameTestServers;
import java.util.List;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@SuppressWarnings("UnstableApiUsage")
public final class MagicCarpetClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        Properties serverProperties =
                MagicCarpetShowcase.serverProperties(ClientGameTestServers.flatServerProperties());

        ClientGameTestServers.withDedicatedServer(
                context,
                serverProperties,
                "MagicCarpet showcase GameTest",
                server -> server.runOnServer(MagicCarpetShowcase::buildStage),
                server -> {
                    ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
                    context.waitTicks(30);
                    context.runOnClient(
                            MagicCarpetClientGameTest::verifyCreativeOnServerWithMagicCarpet);
                    context.runOnClient(client -> verifyTierModels());
                    context.runOnClient(client -> verifyAnimation());
                    MagicCarpetShowcase.film(context, server);
                });
    }

    private static void verifyCreativeOnServerWithMagicCarpet(Minecraft client) {
        if (!ServerSupport.serverHasMagicCarpet()) {
            throw new AssertionError("The test server has MagicCarpet");
        }
        CreativeModeTabs.tryRebuildTabContents(
                client.player.connection.enabledFeatures(), true, client.level.registryAccess());
        CreativeModeTab tools =
                BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(
                        CreativeModeTabs.TOOLS_AND_UTILITIES);
        for (Item item : List.of(ModItems.basic(), ModItems.advanced(), ModItems.legendary())) {
            ItemStack stack = new ItemStack(item);
            if (!ServerSupport.isMagicCarpetItem(stack)) {
                throw new AssertionError(item + " must count as a MagicCarpet item");
            }
            if (!tools.contains(stack) || !CreativeModeTabs.searchTab().contains(stack)) {
                throw new AssertionError(item + " must be in the tools tab and the search");
            }
        }
        if (ServerSupport.isMagicCarpetItem(new ItemStack(Items.MOSS_CARPET))) {
            throw new AssertionError("Vanilla carpets are not MagicCarpet items");
        }
        ItemStack previous = client.player.getInventory().getItem(0);
        client.player.getInventory().setItem(0, new ItemStack(ModItems.basic()));
        Hotbar hotbar = new Hotbar();
        hotbar.storeFrom(client.player.getInventory(), client.level.registryAccess());
        client.player.getInventory().setItem(0, previous);
        if (!hotbar.load(client.level.registryAccess()).getFirst().is(ModItems.basic())) {
            throw new AssertionError("A saved hotbar must keep its carpet");
        }
    }

    /**
     * The entity names, and the parts each tier's model is built from: the basic carpet has neither
     * tassels nor curls, the advanced carpet has tassels only, and the legendary carpet has both.
     * This is the negative that keeps the three tiers from rendering as the same carpet.
     */
    private static void verifyTierModels() {
        EntityType<?>[] types = {
            ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()
        };
        String[] names = {"Basic Magic Carpet", "Advanced Magic Carpet", "Legendary Magic Carpet"};
        for (int tier = 0; tier < types.length; tier++) {
            if (!names[tier].equals(types[tier].getDescription().getString())) {
                throw new AssertionError("Missing translated entity name for " + types[tier]);
            }
        }
        assertTierParts(BasicMagicCarpetEntityRenderer.createBodyLayer(), false, false, "basic");
        assertTierParts(
                AdvancedMagicCarpetEntityRenderer.createBodyLayer(), true, false, "advanced");
        assertTierParts(
                LegendaryMagicCarpetEntityRenderer.createBodyLayer(), true, true, "legendary");
    }

    private static void assertTierParts(
            LayerDefinition layer, boolean tassels, boolean curls, String tier) {
        ModelPart front = layer.bakeRoot().getChild("carpet").getChild("segment_0");
        if (front.hasChild("front_tassel_0") != tassels) {
            throw new AssertionError(
                    tassels
                            ? "The " + tier + " carpet must have tassels"
                            : "The " + tier + " carpet must not have tassels");
        }
        if (front.hasChild("left_curl") != curls) {
            throw new AssertionError(
                    curls
                            ? "The " + tier + " carpet must have curled corners"
                            : "The " + tier + " carpet must not have curled corners");
        }
    }

    /** The idle hover and the flight ripple must differ, and the animation must not accumulate. */
    private static void verifyAnimation() {
        var root = MagicCarpetEntityModel.createBodyLayer(true, true).bakeRoot();
        var model = new MagicCarpetEntityModel(root);
        var state = new MagicCarpetEntityRenderState();
        state.ageInTicks = 20;
        model.setupAnim(state);
        float idle = root.getChild("carpet").getChild("segment_7").y;
        state.speed = 1;
        model.setupAnim(state);
        float flying = root.getChild("carpet").getChild("segment_7").y;
        if (Math.abs(idle - flying) < 0.01) {
            throw new AssertionError("Flight must change the carpet's ripple");
        }
        model.setupAnim(state);
        if (root.getChild("carpet").getChild("segment_7").y != flying) {
            throw new AssertionError("Animation must not accumulate offsets between frames");
        }
    }
}
