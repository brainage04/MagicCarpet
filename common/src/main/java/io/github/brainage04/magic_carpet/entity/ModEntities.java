package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.custom.AdvancedMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.BasicMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.LegendaryMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.world.entity.EntityType;

import java.util.function.Supplier;

public final class ModEntities {
    private static Supplier<? extends EntityType<BasicMagicCarpetEntity>> basic;
    private static Supplier<? extends EntityType<AdvancedMagicCarpetEntity>> advanced;
    private static Supplier<? extends EntityType<LegendaryMagicCarpetEntity>> legendary;
    public static void register(Supplier<? extends EntityType<BasicMagicCarpetEntity>> basic, Supplier<? extends EntityType<AdvancedMagicCarpetEntity>> advanced, Supplier<? extends EntityType<LegendaryMagicCarpetEntity>> legendary) { ModEntities.basic = basic; ModEntities.advanced = advanced; ModEntities.legendary = legendary; }
    public static EntityType<BasicMagicCarpetEntity> basic() { return basic.get(); }
    public static EntityType<AdvancedMagicCarpetEntity> advanced() { return advanced.get(); }
    public static EntityType<LegendaryMagicCarpetEntity> legendary() { return legendary.get(); }
    public static <T extends MagicCarpetEntity> EntityType.Builder<T> builder(EntityType.EntityFactory<T> factory) { return EntityType.Builder.of(factory, net.minecraft.world.entity.MobCategory.MISC).sized(2.0F, .25F); }
    public static void initialize() { }
}
