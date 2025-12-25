package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class LegendaryMagicCarpetEntity extends MagicCarpetEntity {
    public static final EntityType<LegendaryMagicCarpetEntity> ENTITY_TYPE =
            MagicCarpetEntity.generateEntityType("legendary", LegendaryMagicCarpetEntity::new);

    public LegendaryMagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected double getMaxSpeed() {
        return 2.4;
    }

    @Override
    protected double getAccelerationTime() {
        return 0.5;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LEGENDARY_MAGIC_CARPET;
    }
}