package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.custom.AdvancedMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.BasicMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.LegendaryMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.world.entity.EntityType;

public final class ModEntities {
    public static final EntityType<BasicMagicCarpetEntity> BASIC_MAGIC_CARPET =
            MagicCarpetEntity.generateEntityType("basic", BasicMagicCarpetEntity::new);
    public static final EntityType<AdvancedMagicCarpetEntity> ADVANCED_MAGIC_CARPET =
            MagicCarpetEntity.generateEntityType("advanced", AdvancedMagicCarpetEntity::new);
    public static final EntityType<LegendaryMagicCarpetEntity> LEGENDARY_MAGIC_CARPET =
            MagicCarpetEntity.generateEntityType("legendary", LegendaryMagicCarpetEntity::new);

    private ModEntities() {
    }

    public static void initialize() {
        // Class loading registers every entity type.
    }
}
