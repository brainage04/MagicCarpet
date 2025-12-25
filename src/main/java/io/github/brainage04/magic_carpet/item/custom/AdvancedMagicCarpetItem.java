package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.custom.AdvancedMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

public class AdvancedMagicCarpetItem extends MagicCarpetItem {
    public AdvancedMagicCarpetItem(Properties settings) {
        super(settings);
    }

    @Override
    public MagicCarpetEntity createEntity(Level world) {
        return AdvancedMagicCarpetEntity.ENTITY_TYPE.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
    }
}
