package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BasicMagicCarpetEntity extends MagicCarpetEntity {
    public static final EntityType<BasicMagicCarpetEntity> ENTITY_TYPE =
            MagicCarpetEntity.generateEntityType("basic", BasicMagicCarpetEntity::new);

    public BasicMagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected double getMaxSpeed() {
        return 0.6;
    }

    @Override
    protected double getAccelerationTime() {
        return 2.0;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.BASIC_MAGIC_CARPET;
    }
}