package io.github.brainage04.magic_carpet.item;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    private static String getKey(String itemGroup) {
        return "itemGroup.%s.%s".formatted(MagicCarpet.MOD_ID, itemGroup);
    }

    public static final CreativeModeTab MAIN_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.BASIC_MAGIC_CARPET))
            .title(Component.translatable(getKey("main_group")))
            .displayItems((context, entries) -> {
                entries.accept(ModItems.BASIC_MAGIC_CARPET);
                entries.accept(ModItems.ADVANCED_MAGIC_CARPET);
                entries.accept(ModItems.LEGENDARY_MAGIC_CARPET);
            })
            .build();

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "main_group"), MAIN_GROUP);
    }
}
