package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = MagicCarpet.MOD_ID)
public final class MagicCarpetNeoForgeGameTest {
    private MagicCarpetNeoForgeGameTest() {
    }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "registrations"), () -> MagicCarpetNeoForgeGameTest::registrations);
    }

    private static void registrations(GameTestHelper helper) {
        assertEquals(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "basic_magic_carpet"), BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.basic()), "Basic carpet entity is not registered");
        assertEquals(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "legendary_magic_carpet"), BuiltInRegistries.ITEM.getKey(ModItems.legendary()), "Legendary carpet item is not registered");
        helper.succeed();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + ", found " + actual);
    }
}
