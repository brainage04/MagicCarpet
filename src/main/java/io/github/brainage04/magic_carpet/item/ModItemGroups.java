package io.github.brainage04.magic_carpet.item;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    private static String getKey(String itemGroup) {
        return "itemGroup.%s.%s".formatted(MagicCarpet.MOD_ID, itemGroup);
    }

    public static final ItemGroup MAIN_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.MAGIC_CARPET))
            .displayName(Text.translatable(getKey("main_group")))
            .entries((context, entries) -> {
                entries.add(ModItems.MAGIC_CARPET);
            })
            .build();

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, Identifier.of(MagicCarpet.MOD_ID, "main_group"), MAIN_GROUP);
    }
}
