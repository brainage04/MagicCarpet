package io.github.brainage04.magic_carpet.entity.renderer;

import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class BasicMagicCarpetEntityRenderer extends MagicCarpetEntityRenderer {
    public static final ModelLayerLocation MODEL_LAYER = modelLayer("basic");

    public BasicMagicCarpetEntityRenderer(EntityRendererProvider.Context context) {
        super(context, "basic", false);
    }

    public static LayerDefinition createBodyLayer() {
        return MagicCarpetEntityModel.createBodyLayer(false, false);
    }
}
