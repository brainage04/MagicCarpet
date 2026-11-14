package io.github.brainage04.magic_carpet.fabric;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.client.ServerSupport;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.CarpetTier;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.magic_carpet.item.custom.MagicCarpetItem;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class FabricPlatform {
    public void register() {
        DynamicRegistries.registerSynced(CarpetTier.REGISTRY, CarpetTier.CODEC);

        EntityType<MagicCarpetEntity> basic = entityType("basic_magic_carpet");
        EntityType<MagicCarpetEntity> advanced = entityType("advanced_magic_carpet");
        EntityType<MagicCarpetEntity> legendary = entityType("legendary_magic_carpet");
        ModEntities.register(() -> basic, () -> advanced, () -> legendary);

        Item b = item("basic_magic_carpet", basic);
        Item a = item("advanced_magic_carpet", advanced);
        Item l = item("legendary_magic_carpet", legendary);
        ModItems.register(() -> b, () -> a, () -> l);

        CreativeModeTabEvents.modifyOutputEvent(
                        ResourceKey.create(
                                Registries.CREATIVE_MODE_TAB,
                                Identifier.withDefaultNamespace("tools_and_utilities")))
                .register(
                        e -> {
                            // Tab contents are built only on the client, once per connection; a
                            // server without MagicCarpet gets none of its carpets offered (see
                            // ServerSupport).
                            if (!ServerSupport.serverHasMagicCarpet()) return;
                            e.accept(b);
                            e.accept(a);
                            e.accept(l);
                        });
    }

    private static EntityType<MagicCarpetEntity> entityType(String path) {
        return Registry.register(
                BuiltInRegistries.ENTITY_TYPE,
                id(path),
                ModEntities.builder().build(ResourceKey.create(Registries.ENTITY_TYPE, id(path))));
    }

    private static Item item(String path, EntityType<MagicCarpetEntity> type) {
        return Registry.register(
                BuiltInRegistries.ITEM,
                id(path),
                new MagicCarpetItem(
                        () -> type,
                        new Item.Properties()
                                .stacksTo(1)
                                .setId(ResourceKey.create(Registries.ITEM, id(path)))));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, path);
    }
}
