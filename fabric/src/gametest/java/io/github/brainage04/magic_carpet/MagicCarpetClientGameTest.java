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
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

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
                    verifyRiderYaw(context, server);
                    verifySpeedAtASlowTickRate(context, server);
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
     * The carpet must be drawn facing where its rider looks, every frame. The mouse turns the rider
     * between ticks (Entity.java:495-503 in 26.3), so a carpet drawn from its own yaw fields, which
     * are set once a tick, snaps round behind the player. This turns the rider a quarter turn
     * between ticks and fails if any frame draws the two apart.
     */
    private static void verifyRiderYaw(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        int[] carpetId = new int[1];
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = player.level();
                    player.teleportTo(level, 400.5D, 100.0D, 0.5D, Set.of(), 0.0F, 0.0F, true);
                    MagicCarpetEntity carpet =
                            (MagicCarpetEntity)
                                    ModEntities.basic().create(level, EntitySpawnReason.COMMAND);
                    carpet.setPos(400.5D, 100.0D, 0.5D);
                    level.addFreshEntity(carpet);
                    player.startRiding(carpet, true, true);
                    carpetId[0] = carpet.getId();
                });
        context.waitFor(
                client ->
                        client.player.getVehicle() != null
                                && client.player.getVehicle().getId() == carpetId[0],
                200);
        context.waitTicks(10);
        for (int tick = 1; tick <= 20; tick++) {
            context.waitTick();
            // The mouse turns the rider between ticks, moving both of its yaw fields
            // (Entity.turn, Entity.java:495-503 in 26.3), so the frame is drawn at a yaw the
            // carpet's own tick never saw. turn scales its argument by 0.15
            // (Entity.java:498), so 30 is 4.5 degrees: a quarter turn over the 20 ticks.
            context.runOnClient(client -> client.player.turn(30.0D, 0.0D));
            float gap =
                    context.computeOnClient(
                            client -> {
                                Entity carpet = client.level.getEntity(carpetId[0]);
                                MagicCarpetEntity magic = (MagicCarpetEntity) carpet;
                                MagicCarpetEntityRenderer renderer =
                                        (MagicCarpetEntityRenderer)
                                                client.getEntityRenderDispatcher()
                                                        .getRenderer(magic);
                                MagicCarpetEntityRenderState state =
                                        renderer.createRenderState(magic, 0.5F);
                                return Math.abs(
                                        Mth.wrapDegrees(state.yaw - client.player.getYRot(0.5F)));
                            });
            if (gap > 0.01F) {
                throw new AssertionError(
                        "Every frame must draw the carpet facing where it draws its rider; they"
                                + " were drawn "
                                + gap
                                + " degrees apart at tick "
                                + tick);
            }
        }
        server.runOnServer(
                minecraftServer ->
                        minecraftServer.getPlayerList().getPlayers().getFirst().stopRiding());
    }

    /**
     * The film measures a tier's top speed after a wall-clock hold, so a client that ticks below 20
     * a second gets fewer ticks than the tier's acceleration needs and the carpet is still speeding
     * up when the window opens. The basic tier accelerates for 2 s, 40 ticks; a 1.5 s hold at 10
     * ticks a second is 15 ticks, and the carpet is then still accelerating.
     *
     * <p>This runs the film's own measurement at 10 ticks a second, so it fails when the hold is
     * wall clock alone and passes when the film waits in ticks for the speed it checks.
     */
    private static void verifySpeedAtASlowTickRate(
            ClientGameTestContext context, TestDedicatedServerContext server) {
        int[] carpetId = new int[1];
        server.runOnServer(
                minecraftServer -> {
                    ServerPlayer player = minecraftServer.getPlayerList().getPlayers().getFirst();
                    ServerLevel level = player.level();
                    // The film's own basic site, where buildStage placed the runway.
                    Vec3 stand = MagicCarpetShowcase.standAt(MagicCarpetShowcase.BASIC_SITE);
                    Vec3 hang = MagicCarpetShowcase.carpetAt(MagicCarpetShowcase.BASIC_SITE);
                    player.teleportTo(
                            level,
                            stand.x,
                            stand.y,
                            stand.z,
                            Set.of(),
                            MagicCarpetShowcase.mountYaw(),
                            MagicCarpetShowcase.mountPitch(),
                            true);
                    MagicCarpetEntity carpet =
                            (MagicCarpetEntity)
                                    ModEntities.basic().create(level, EntitySpawnReason.COMMAND);
                    carpet.setPos(hang.x, hang.y, hang.z);
                    level.addFreshEntity(carpet);
                    player.startRiding(carpet, true, true);
                    carpetId[0] = carpet.getId();
                });
        try {
            context.waitFor(
                    client ->
                            client.player.getVehicle() != null
                                    && client.player.getVehicle().getId() == carpetId[0],
                    200);
            context.waitTicks(20);
            context.getInput().holdKey(options -> options.keyUp);
            try {
                // The server must see the key before the hold means anything.
                for (int tick = 0; ; tick++) {
                    if (tick > 100) {
                        throw new AssertionError("The forward key never reached the server");
                    }
                    if (server.computeOnServer(
                            minecraftServer ->
                                    minecraftServer
                                            .getPlayerList()
                                            .getPlayers()
                                            .getFirst()
                                            .getLastClientInput()
                                            .forward())) {
                        break;
                    }
                    context.waitTick();
                }
                server.runOnServer(
                        minecraftServer -> minecraftServer.tickRateManager().setTickRate(10.0F));
                // The film's own hold, in wall clock, at the slow rate, then its own measurement.
                long end = System.currentTimeMillis() + MagicCarpetShowcase.FLY_MILLIS;
                while (System.currentTimeMillis() < end) {
                    context.waitTick();
                }
                double topSpeed =
                        server.computeOnServer(
                                minecraftServer -> {
                                    Entity vehicle =
                                            minecraftServer
                                                    .getPlayerList()
                                                    .getPlayers()
                                                    .getFirst()
                                                    .getVehicle();
                                    if (!(vehicle instanceof MagicCarpetEntity carpet)) {
                                        throw new AssertionError(
                                                "The player must ride a carpet to measure its"
                                                        + " speed");
                                    }
                                    return carpet.tier().maxSpeed();
                                });
                double measured =
                        MagicCarpetShowcase.measureTopSpeed(server, context::waitTick, topSpeed);
                if (Math.abs(measured - topSpeed)
                        > topSpeed * MagicCarpetShowcase.speedTolerance()) {
                    throw new AssertionError(
                            "The film's measurement at 10 ticks a second must reach the top speed;"
                                    + " it measured "
                                    + measured * 20.0D
                                    + " blocks/s");
                }
            } finally {
                context.getInput().releaseKey(options -> options.keyUp);
            }
        } finally {
            // The carpet flies north along the runway the film's basic flight uses, so it must not
            // be left in the world.
            server.runOnServer(
                    minecraftServer -> {
                        minecraftServer.tickRateManager().setTickRate(20.0F);
                        ServerPlayer player =
                                minecraftServer.getPlayerList().getPlayers().getFirst();
                        player.stopRiding();
                        Entity carpet = player.level().getEntity(carpetId[0]);
                        if (carpet != null) {
                            carpet.discard();
                        }
                    });
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

    /**
     * The idle hover and the flight ripple must differ, and the animation must not accumulate. The
     * ripple's phase comes from the entity, so the same phase draws the same ripple however many
     * frames are drawn.
     */
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
