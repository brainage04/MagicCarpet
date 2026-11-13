package io.github.brainage04.magic_carpet.fabric;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;

public final class MagicCarpetFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        for (EntityType<MagicCarpetEntity> type :
                List.of(ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary())) {
            EntityRenderers.register(type, context -> new MagicCarpetEntityRenderer(context, type));
        }
        for (int shape = 0; shape < MagicCarpetEntityRenderer.MODEL_LAYERS.size(); shape++) {
            int layerShape = shape;
            ModelLayerRegistry.registerModelLayer(
                    MagicCarpetEntityRenderer.MODEL_LAYERS.get(shape),
                    () -> MagicCarpetEntityRenderer.createBodyLayer(layerShape));
        }
    }
}
