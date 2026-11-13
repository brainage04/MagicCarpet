package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.MagicCarpetGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Each function needs a matching {@code data/<mod_id>/test_instance/<name>.json} in this source
 * set's resources.
 */
@EventBusSubscriber(modid = MagicCarpet.MOD_ID)
public final class MagicCarpetNeoForgeGameTest {
    private MagicCarpetNeoForgeGameTest() {}

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "sustained_movement"),
                () -> MagicCarpetGameTests::sustainedMovement);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "passengers_and_fall_damage"),
                () -> MagicCarpetGameTests::passengersAndFallDamage);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "collision"),
                () -> MagicCarpetGameTests::collision);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "tier_data"),
                () -> MagicCarpetGameTests::tierData);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "tier_codec_round_trip"),
                () -> MagicCarpetGameTests::tierCodecRoundTrip);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "crafting"),
                () -> MagicCarpetGameTests::crafting);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "placement"),
                () -> MagicCarpetGameTests::placement);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "persistence_and_drops"),
                () -> MagicCarpetGameTests::persistenceAndDrops);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "player_takes_control"),
                () -> MagicCarpetGameTests::playerTakesControl);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "mount_requires_a_plain_use"),
                () -> MagicCarpetGameTests::mountRequiresAPlainUse);
        event.register(
                BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(
                        MagicCarpet.MOD_ID, "released_keys_coast_to_a_stop"),
                () -> MagicCarpetGameTests::releasedKeysCoastToAStop);
    }
}
