package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.AdvancedMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.BasicMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.LegendaryMagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.magic_carpet.item.custom.AdvancedMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.BasicMagicCarpetItem;
import io.github.brainage04.magic_carpet.item.custom.LegendaryMagicCarpetItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NeoForgePlatform {
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MagicCarpet.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MagicCarpet.MOD_ID);

    public static void register(IEventBus modBus) {
        DeferredHolder<EntityType<?>, EntityType<BasicMagicCarpetEntity>> basic = ENTITIES.register("basic_magic_carpet", () -> ModEntities.builder(BasicMagicCarpetEntity::new).build(entityKey("basic_magic_carpet")));
        DeferredHolder<EntityType<?>, EntityType<AdvancedMagicCarpetEntity>> advanced = ENTITIES.register("advanced_magic_carpet", () -> ModEntities.builder(AdvancedMagicCarpetEntity::new).build(entityKey("advanced_magic_carpet")));
        DeferredHolder<EntityType<?>, EntityType<LegendaryMagicCarpetEntity>> legendary = ENTITIES.register("legendary_magic_carpet", () -> ModEntities.builder(LegendaryMagicCarpetEntity::new).build(entityKey("legendary_magic_carpet")));
        ModEntities.register(basic, advanced, legendary);
        var basicItem = ITEMS.register("basic_magic_carpet", () -> new BasicMagicCarpetItem(itemProperties("basic_magic_carpet")));
        var advancedItem = ITEMS.register("advanced_magic_carpet", () -> new AdvancedMagicCarpetItem(itemProperties("advanced_magic_carpet")));
        var legendaryItem = ITEMS.register("legendary_magic_carpet", () -> new LegendaryMagicCarpetItem(itemProperties("legendary_magic_carpet")));
        ModItems.register(basicItem, advancedItem, legendaryItem);
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(NeoForgePlatform::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) { event.accept(ModItems.basic()); event.accept(ModItems.advanced()); event.accept(ModItems.legendary()); }
    }
    private static ResourceKey<EntityType<?>> entityKey(String path) { return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path)); }
    private static Item.Properties itemProperties(String path) { return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path))).stacksTo(1); }
}
