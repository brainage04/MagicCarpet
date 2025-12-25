package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class AdvancedMagicCarpetEntity extends MagicCarpetEntity {
    public static final EntityType<AdvancedMagicCarpetEntity> ENTITY_TYPE =
            MagicCarpetEntity.generateEntityType("advanced", AdvancedMagicCarpetEntity::new);

    public AdvancedMagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected double getMaxSpeed() {
        return 1.2;
    }

    @Override
    protected double getAccelerationTime() {
        return 1.0;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.ADVANCED_MAGIC_CARPET;
    }
}