package io.github.brainage04.magic_carpet.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LegendaryMagicCarpetEntityRenderer extends MagicCarpetEntityRenderer {
    public LegendaryMagicCarpetEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public String getCarpetType() {
        return "legendary";
    }
}