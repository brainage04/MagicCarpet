package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.custom.AdvancedMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.BasicMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.custom.LegendaryMagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.AdvancedMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.BasicMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.LegendaryMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ModEntities {
    public static void initialize() {
        EntityRendererRegistry.register(BasicMagicCarpetEntity.ENTITY_TYPE, BasicMagicCarpetEntityRenderer::new);
        EntityRendererRegistry.register(AdvancedMagicCarpetEntity.ENTITY_TYPE, AdvancedMagicCarpetEntityRenderer::new);
        EntityRendererRegistry.register(LegendaryMagicCarpetEntity.ENTITY_TYPE, LegendaryMagicCarpetEntityRenderer::new);

        EntityModelLayerRegistry.registerModelLayer(MagicCarpetEntityRenderer.ENTITY_MODEL_LAYER, MagicCarpetEntityModel::getTexturedModelData);
    }
}
