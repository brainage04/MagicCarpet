package io.github.brainage04.entitytesting;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityTesting implements ModInitializer {
	public static final String MOD_ID = "entitytesting";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final EntityType<CubeEntity> CUBE = Registry.register(
			Registries.ENTITY_TYPE,
			Identifier.of("entitytesting", "cube"),
			EntityType.Builder.create(CubeEntity::new, SpawnGroup.CREATURE)
					.dimensions(0.75f, 0.75f)
					.build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of("entitytesting", "cube")))
	);

	@Override
	public void onInitialize() {
		FabricDefaultAttributeRegistry.register(CUBE, CubeEntity.createMobAttributes());
	}
}