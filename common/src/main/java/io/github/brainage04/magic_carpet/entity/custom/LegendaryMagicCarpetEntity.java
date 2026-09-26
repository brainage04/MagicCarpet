package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LegendaryMagicCarpetEntity extends MagicCarpetEntity {
    public LegendaryMagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public double getMaxSpeed() {
        return 2.4;
    }

    @Override
    protected double getAccelerationTime() {
        return 0.5;
    }

    @Override
    protected void spawnAmbientParticle(Vec3 position, Vec3 backward, float speed) {
        Vec3 drift = backward.scale(0.05 * speed);
        if (getRandom().nextFloat() < 0.3F) {
            level().addParticle(ParticleTypes.PORTAL,
                    position.x, position.y, position.z,
                    (getRandom().nextDouble() - 0.5) * 0.6, getRandom().nextDouble() * 0.3, (getRandom().nextDouble() - 0.5) * 0.6);
        } else {
            level().addParticle(ParticleTypes.END_ROD,
                    position.x, position.y, position.z,
                    drift.x + (getRandom().nextDouble() - 0.5) * 0.02, 0.01, drift.z + (getRandom().nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    protected Item getDropItem() {
        return ModItems.legendary();
    }
}
