package io.github.brainage04.magic_carpet.entity;

import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.AdvancedMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.BasicMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.LegendaryMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class ModEntityRenderers {
    public static void initialize() {
        EntityRenderers.register(ModEntities.BASIC_MAGIC_CARPET, BasicMagicCarpetEntityRenderer::new);
        EntityRenderers.register(ModEntities.ADVANCED_MAGIC_CARPET, AdvancedMagicCarpetEntityRenderer::new);
        EntityRenderers.register(ModEntities.LEGENDARY_MAGIC_CARPET, LegendaryMagicCarpetEntityRenderer::new);

        ModelLayerRegistry.registerModelLayer(MagicCarpetEntityRenderer.ENTITY_MODEL_LAYER, MagicCarpetEntityModel::getTexturedModelData);
    }
}
