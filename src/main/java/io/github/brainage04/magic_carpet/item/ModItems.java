package io.github.brainage04.magic_carpet.item;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.item.custom.AdvancedMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.BasicMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.LegendaryMagicCarpetItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.function.Function;

public class ModItems {
    public static final Item BASIC_MAGIC_CARPET = register("basic_magic_carpet", BasicMagicCarpetItem::new, new Item.Properties().stacksTo(1));
    public static final Item ADVANCED_MAGIC_CARPET = register("advanced_magic_carpet", AdvancedMagicCarpetItem::new, new Item.Properties().stacksTo(1));
    public static final Item LEGENDARY_MAGIC_CARPET = register("legendary_magic_carpet", LegendaryMagicCarpetItem::new, new Item.Properties().stacksTo(1));

    public static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties settings) {
        final ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path));
        return Items.registerItem(registryKey, factory, settings);
    }

    public static void initialize() {
        registerToVanillaItemGroups();
    }

    public static void registerToVanillaItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> {
            content.accept(ModItems.BASIC_MAGIC_CARPET);
            content.accept(ModItems.ADVANCED_MAGIC_CARPET);
            content.accept(ModItems.LEGENDARY_MAGIC_CARPET);
        });
    }
}
