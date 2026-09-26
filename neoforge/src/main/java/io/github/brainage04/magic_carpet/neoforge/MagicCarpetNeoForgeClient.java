package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.renderer.AdvancedMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.BasicMagicCarpetEntityRenderer;
import io.github.brainage04.magic_carpet.entity.renderer.LegendaryMagicCarpetEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = MagicCarpet.MOD_ID, dist = Dist.CLIENT)
public final class MagicCarpetNeoForgeClient {
    public MagicCarpetNeoForgeClient(IEventBus modBus) { modBus.addListener(this::registerRenderers); modBus.addListener(this::registerLayers); }
    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.basic(), BasicMagicCarpetEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.advanced(), AdvancedMagicCarpetEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.legendary(), LegendaryMagicCarpetEntityRenderer::new);
    }
    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BasicMagicCarpetEntityRenderer.MODEL_LAYER, BasicMagicCarpetEntityRenderer::createBodyLayer);
        event.registerLayerDefinition(AdvancedMagicCarpetEntityRenderer.MODEL_LAYER, AdvancedMagicCarpetEntityRenderer::createBodyLayer);
        event.registerLayerDefinition(LegendaryMagicCarpetEntityRenderer.MODEL_LAYER, LegendaryMagicCarpetEntityRenderer::createBodyLayer);
    }
}
