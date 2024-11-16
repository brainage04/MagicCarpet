package io.github.brainage04.magic_carpet.item;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.item.custom.MagicCarpetItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item MAGIC_CARPET = register("magic_carpet", MagicCarpetItem::new, new Item.Settings().maxCount(1));

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MagicCarpet.MOD_ID, path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize() {
        registerToVanillaItemGroups();
    }

    public static void registerToVanillaItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            content.add(ModItems.MAGIC_CARPET);
        });
    }
}
