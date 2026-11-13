package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.client.ServerSupport;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.CarpetTier;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.magic_carpet.item.custom.MagicCarpetItem;
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
import net.neoforged.neoforge.registries.NewDatapackRegistryEvent;

public final class NeoForgePlatform {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MagicCarpet.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, MagicCarpet.MOD_ID);

    public static void register(IEventBus modBus) {
        var basic = entityType("basic_magic_carpet");
        var advanced = entityType("advanced_magic_carpet");
        var legendary = entityType("legendary_magic_carpet");
        ModEntities.register(basic, advanced, legendary);
        ModItems.register(
                item("basic_magic_carpet", basic),
                item("advanced_magic_carpet", advanced),
                item("legendary_magic_carpet", legendary));
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(NeoForgePlatform::addCreativeItems);
        modBus.addListener(NeoForgePlatform::addTierRegistry);
    }

    private static DeferredHolder<EntityType<?>, EntityType<MagicCarpetEntity>> entityType(
            String path) {
        return ENTITIES.register(
                path,
                () ->
                        ModEntities.builder()
                                .build(
                                        ResourceKey.create(
                                                Registries.ENTITY_TYPE,
                                                Identifier.fromNamespaceAndPath(
                                                        MagicCarpet.MOD_ID, path))));
    }

    private static DeferredHolder<Item, MagicCarpetItem> item(
            String path, DeferredHolder<EntityType<?>, EntityType<MagicCarpetEntity>> type) {
        return ITEMS.register(
                path,
                () ->
                        new MagicCarpetItem(
                                type,
                                new Item.Properties()
                                        .setId(
                                                ResourceKey.create(
                                                        Registries.ITEM,
                                                        Identifier.fromNamespaceAndPath(
                                                                MagicCarpet.MOD_ID, path)))
                                        .stacksTo(1)));
    }

    private static void addTierRegistry(NewDatapackRegistryEvent event) {
        event.worldRegistry(CarpetTier.REGISTRY, CarpetTier.CODEC, CarpetTier.CODEC);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        // Tab contents are built only on the client, once per connection; a server without
        // MagicCarpet gets none of its carpets offered (see ServerSupport).
        if (!ServerSupport.serverHasMagicCarpet()) return;
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(ModItems.basic());
            event.accept(ModItems.advanced());
            event.accept(ModItems.legendary());
        }
    }
}
