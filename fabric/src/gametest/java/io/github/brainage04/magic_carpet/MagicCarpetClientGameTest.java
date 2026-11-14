package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.client.ServerSupport;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.CarpetTier;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
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
import net.minecraft.core.Registry;
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
                    context.runOnClient(MagicCarpetClientGameTest::verifyTierModels);
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
     * The entity names, and each tier as the client received it from the dedicated server: the top
     * speed, the glow, and the parts its model is built from. The basic carpet has neither tassels
     * nor curls, the advanced carpet has tassels only, and the legendary carpet has both and glows.
     * This is the negative that keeps the three tiers from rendering as the same carpet.
     */
    private static void verifyTierModels(Minecraft client) {
        List<EntityType<MagicCarpetEntity>> types =
                List.of(ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary());
        String[] names = {"Basic Magic Carpet", "Advanced Magic Carpet", "Legendary Magic Carpet"};
        double[] speeds = {0.6, 1.2, 2.4};
        boolean[] tassels = {false, true, true};
        boolean[] curls = {false, false, true};
        boolean[] glows = {false, false, true};
        Registry<CarpetTier> tiers =
                client.level.registryAccess().lookupOrThrow(CarpetTier.REGISTRY);
        for (int index = 0; index < types.size(); index++) {
            EntityType<MagicCarpetEntity> type = types.get(index);
            if (!names[index].equals(type.getDescription().getString())) {
                throw new AssertionError("Missing translated entity name for " + type);
            }
            CarpetTier tier =
                    tiers.getOptional(CarpetTier.key(EntityType.getKey(type)))
                            .orElseThrow(
                                    () ->
                                            new AssertionError(
                                                    "The server sent no tier for " + type));
            if (tier.maxSpeed() != speeds[index] || tier.model().glow() != glows[index]) {
                throw new AssertionError("The client received a changed tier: " + tier);
            }
            assertTierParts(
                    MagicCarpetEntityRenderer.createBodyLayer(
                            MagicCarpetEntityRenderer.shape(tier.model())),
                    tassels[index],
                    curls[index],
                    names[index]);
        }
    }

    private static void assertTierParts(
            LayerDefinition layer, boolean tassels, boolean curls, String tier) {
        ModelPart front = layer.bakeRoot().getChild("carpet").getChild("segment_0");
        if (front.hasChild("front_tassel_0") != tassels) {
            throw new AssertionError(
                    tassels
                            ? "The " + tier + " must have tassels"
                            : "The " + tier + " must not have tassels");
        }
        if (front.hasChild("left_curl") != curls) {
            throw new AssertionError(
                    curls
                            ? "The " + tier + " must have curled corners"
                            : "The " + tier + " must not have curled corners");
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
