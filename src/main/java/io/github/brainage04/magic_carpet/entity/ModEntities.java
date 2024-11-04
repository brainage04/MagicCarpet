package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ModEntities {
    public static void initialize() {
        EntityRendererRegistry.register(MagicCarpetEntity.ENTITY_TYPE, MagicCarpetEntityRenderer::new);
        EntityModelLayerRegistry.registerModelLayer(MagicCarpetEntityModel.ENTITY_MODEL_LAYER, MagicCarpetEntityModel::getTexturedModelData);
    }
}
