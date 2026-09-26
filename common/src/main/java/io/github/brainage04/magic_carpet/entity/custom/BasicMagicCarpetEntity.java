package io.github.brainage04.magic_carpet.entity.custom;

import io.github.brainage04.magic_carpet.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class BasicMagicCarpetEntity extends MagicCarpetEntity {
    public BasicMagicCarpetEntity(EntityType<? extends VehicleEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    public double getMaxSpeed() {
        return 0.6;
    }

    @Override
    protected double getAccelerationTime() {
        return 2.0;
    }

    @Override
    protected void spawnAmbientParticle(Vec3 position, Vec3 backward, float speed) {
        // glyphs drift down into the weave, like an enchanting table
        level().addParticle(ParticleTypes.ENCHANT,
                position.x, position.y, position.z,
                (getRandom().nextDouble() - 0.5) * 1.5,
                getRandom().nextDouble() * 0.8 + 0.2,
                (getRandom().nextDouble() - 0.5) * 1.5);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.basic();
    }
}
