package io.github.brainage04.magic_carpet.fabric;
import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.*;
import io.github.brainage04.magic_carpet.item.ModItems;
import io.github.brainage04.magic_carpet.item.custom.*;
import io.github.brainage04.magic_carpet.platform.Platform;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
public final class FabricPlatform implements Platform {
 public void register() {
  var basic=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("basic_magic_carpet"),ModEntities.builder(BasicMagicCarpetEntity::new).build(ResourceKey.create(Registries.ENTITY_TYPE,id("basic_magic_carpet"))));
  var advanced=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("advanced_magic_carpet"),ModEntities.builder(AdvancedMagicCarpetEntity::new).build(ResourceKey.create(Registries.ENTITY_TYPE,id("advanced_magic_carpet"))));
  var legendary=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("legendary_magic_carpet"),ModEntities.builder(LegendaryMagicCarpetEntity::new).build(ResourceKey.create(Registries.ENTITY_TYPE,id("legendary_magic_carpet"))));
  ModEntities.register(()->basic,()->advanced,()->legendary);
  var b=Registry.register(BuiltInRegistries.ITEM,id("basic_magic_carpet"),new BasicMagicCarpetItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM,id("basic_magic_carpet")))));
  var a=Registry.register(BuiltInRegistries.ITEM,id("advanced_magic_carpet"),new AdvancedMagicCarpetItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM,id("advanced_magic_carpet")))));
  var l=Registry.register(BuiltInRegistries.ITEM,id("legendary_magic_carpet"),new LegendaryMagicCarpetItem(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM,id("legendary_magic_carpet")))));
  ModItems.register(()->b,()->a,()->l);
  CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB,Identifier.withDefaultNamespace("tools_and_utilities"))).register(e->{e.accept(b);e.accept(a);e.accept(l);});
 }
 private static Identifier id(String path){return Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID,path);}
}
