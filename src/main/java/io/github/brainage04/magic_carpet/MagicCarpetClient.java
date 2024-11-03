package io.github.brainage04.magic_carpet;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class MagicCarpetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(MagicCarpetEntity.ENTITY_TYPE, MagicCarpetEntityRenderer::new);

		EntityModelLayerRegistry.registerModelLayer(MagicCarpetEntityModel.ENTITY_MODEL_LAYER, MagicCarpetEntityModel::getTexturedModelData);
	}
}