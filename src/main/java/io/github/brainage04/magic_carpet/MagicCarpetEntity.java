package io.github.brainage04.magic_carpet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class MagicCarpetEntity extends BoatEntity {
    public static final EntityType<MagicCarpetEntity> ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MagicCarpet.MOD_ID, "magic_carpet"),
            EntityType.Builder.create(MagicCarpetEntity::new, SpawnGroup.MISC)
                    .dimensions(0.75f, 0.75f)
                    .build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MagicCarpet.MOD_ID, "magic_carpet")))
    );

    public MagicCarpetEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world, null);
    }
}