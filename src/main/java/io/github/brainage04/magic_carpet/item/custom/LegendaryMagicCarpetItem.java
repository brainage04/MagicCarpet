package io.github.brainage04.magic_carpet.item.custom;

import io.github.brainage04.magic_carpet.entity.custom.LegendaryMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.Level;

public class LegendaryMagicCarpetItem extends MagicCarpetItem {
    public LegendaryMagicCarpetItem(Properties settings) {
        super(settings);
    }

    @Override
    public MagicCarpetEntity createEntity(Level world) {
        return LegendaryMagicCarpetEntity.ENTITY_TYPE.create(world, EntitySpawnReason.SPAWN_ITEM_USE);
    }
}
