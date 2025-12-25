package io.github.brainage04.magic_carpet.entity.renderer;

import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BasicMagicCarpetEntityRenderer extends MagicCarpetEntityRenderer {
    public BasicMagicCarpetEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public String getCarpetType() {
        return "basic";
    }
}