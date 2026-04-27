package io.github.brainage04.magic_carpet.item;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.item.custom.AdvancedMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.BasicMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.LegendaryMagicCarpetItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {
    private static final ResourceKey<CreativeModeTab> TOOLS_AND_UTILITIES_GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("tools_and_utilities"));

    public static final Item BASIC_MAGIC_CARPET = register("basic_magic_carpet", BasicMagicCarpetItem::new, new Item.Properties().stacksTo(1));
    public static final Item ADVANCED_MAGIC_CARPET = register("advanced_magic_carpet", AdvancedMagicCarpetItem::new, new Item.Properties().stacksTo(1));
    public static final Item LEGENDARY_MAGIC_CARPET = register("legendary_magic_carpet", LegendaryMagicCarpetItem::new, new Item.Properties().stacksTo(1));

    public static Item register(String path, Function<Item.Properties, Item> factory, Item.Properties settings) {
        final ResourceKey<Item> registryKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path));
        return Registry.register(BuiltInRegistries.ITEM, registryKey, factory.apply(settings.setId(registryKey)));
    }

    public static void initialize() {
        registerToVanillaItemGroups();
    }

    public static void registerToVanillaItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(TOOLS_AND_UTILITIES_GROUP).register(content -> {
            content.accept(ModItems.BASIC_MAGIC_CARPET);
            content.accept(ModItems.ADVANCED_MAGIC_CARPET);
            content.accept(ModItems.LEGENDARY_MAGIC_CARPET);
        });
    }
}
