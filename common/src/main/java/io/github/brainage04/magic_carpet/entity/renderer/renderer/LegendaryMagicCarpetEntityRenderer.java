package io.github.brainage04.magic_carpet.entity.renderer;

import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LegendaryMagicCarpetEntityRenderer extends MagicCarpetEntityRenderer {
    public static final ModelLayerLocation MODEL_LAYER = modelLayer("legendary");

    public LegendaryMagicCarpetEntityRenderer(EntityRendererProvider.Context context) {
        super(context, "legendary", true);
    }

    public static LayerDefinition createBodyLayer() {
        return MagicCarpetEntityModel.createBodyLayer(true, true);
    }
}
