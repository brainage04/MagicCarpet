package io.github.brainage04.magic_carpet;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item MAGIC_CARPET = register("magic_carpet", MagicCarpetItem::new, new Item.Settings());

    public static Item register(String path, Function<Item.Settings, Item> factory, Item.Settings settings) {
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MagicCarpet.MOD_ID, path));
        return Items.register(registryKey, factory, settings);
    }

    public static void initialize() {
        MagicCarpet.LOGGER.info(MAGIC_CARPET.toString());
    }
}
