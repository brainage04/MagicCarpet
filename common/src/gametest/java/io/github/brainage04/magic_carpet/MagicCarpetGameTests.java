package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;

/**
 * Loader-neutral server GameTest bodies. Both loaders compile this source set into their GameTest
 * mods: Fabric runs them through {@code @GameTest} methods, NeoForge through registered test
 * functions and {@code test_instance} data.
 */
public final class MagicCarpetGameTests {
    private MagicCarpetGameTests() {
    }

    public static void registrations(GameTestHelper helper) {
        assertEquals(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "basic_magic_carpet"), BuiltInRegistries.ENTITY_TYPE.getKey(ModEntities.basic()), "Basic carpet entity is not registered");
        assertEquals(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "legendary_magic_carpet"), BuiltInRegistries.ITEM.getKey(ModItems.legendary()), "Legendary carpet item is not registered");
        helper.succeed();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": expected " + expected + ", found " + actual);
    }
}
