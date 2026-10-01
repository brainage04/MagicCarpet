package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.MagicCarpetGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the shared GameTests as NeoForge test functions. Each function needs a matching
 * {@code data/<mod_id>/test_instance/<name>.json} in this source set's resources.
 */
@EventBusSubscriber(modid = MagicCarpet.MOD_ID)
public final class MagicCarpetNeoForgeGameTest {
    private MagicCarpetNeoForgeGameTest() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "registrations"), () -> MagicCarpetGameTests::registrations);
    }
}
