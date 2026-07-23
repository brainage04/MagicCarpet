package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

public class BasicMagicCarpetItem extends MagicCarpetItem {
    public BasicMagicCarpetItem(Properties settings) {
        super(settings);
    }

    @Override
    public MagicCarpetEntity createEntity(Level world) {
        return ModEntities.BASIC_MAGIC_CARPET.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
    }
}
