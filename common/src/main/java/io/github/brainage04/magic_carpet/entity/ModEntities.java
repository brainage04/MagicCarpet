package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    private static Supplier<? extends EntityType<MagicCarpetEntity>> basic;
    private static Supplier<? extends EntityType<MagicCarpetEntity>> advanced;
    private static Supplier<? extends EntityType<MagicCarpetEntity>> legendary;

    public static void register(
            Supplier<? extends EntityType<MagicCarpetEntity>> basic,
            Supplier<? extends EntityType<MagicCarpetEntity>> advanced,
            Supplier<? extends EntityType<MagicCarpetEntity>> legendary) {
        ModEntities.basic = basic;
        ModEntities.advanced = advanced;
        ModEntities.legendary = legendary;
    }

    public static EntityType<MagicCarpetEntity> basic() {
        return basic.get();
    }

    public static EntityType<MagicCarpetEntity> advanced() {
        return advanced.get();
    }

    public static EntityType<MagicCarpetEntity> legendary() {
        return legendary.get();
    }

    public static EntityType.Builder<MagicCarpetEntity> builder() {
        return EntityType.Builder.of(MagicCarpetEntity::new, MobCategory.MISC).sized(1.5F, .25F);
    }
}
