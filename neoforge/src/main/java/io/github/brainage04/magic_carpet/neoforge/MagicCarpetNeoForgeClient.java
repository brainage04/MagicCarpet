package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.renderer.MagicCarpetEntityRenderer;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = MagicCarpet.MOD_ID, dist = Dist.CLIENT)
public final class MagicCarpetNeoForgeClient {
    public MagicCarpetNeoForgeClient(IEventBus modBus) {
        modBus.addListener(this::registerRenderers);
        modBus.addListener(this::registerLayers);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        for (EntityType<MagicCarpetEntity> type :
                List.of(ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary())) {
            event.registerEntityRenderer(
                    type, context -> new MagicCarpetEntityRenderer(context, type));
        }
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (int shape = 0; shape < MagicCarpetEntityRenderer.MODEL_LAYERS.size(); shape++) {
            int layerShape = shape;
            event.registerLayerDefinition(
                    MagicCarpetEntityRenderer.MODEL_LAYERS.get(shape),
                    () -> MagicCarpetEntityRenderer.createBodyLayer(layerShape));
        }
    }
}
