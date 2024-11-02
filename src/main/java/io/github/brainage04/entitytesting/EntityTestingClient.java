package io.github.brainage04.entitytesting;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class EntityTestingClient implements ClientModInitializer {
	public static final EntityModelLayer MODEL_CUBE_LAYER = new EntityModelLayer(Identifier.of("entitytesting", "cube"), "main");

	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(EntityTesting.CUBE, CubeEntityRenderer::new);

		EntityModelLayerRegistry.registerModelLayer(MODEL_CUBE_LAYER, CubeEntityModel::getTexturedModelData);
	}
}