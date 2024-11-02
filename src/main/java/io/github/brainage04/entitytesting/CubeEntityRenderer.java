package io.github.brainage04.entitytesting;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class CubeEntityRenderer extends MobEntityRenderer<CubeEntity, CubeEntityRenderState, CubeEntityModel> {
    public CubeEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new CubeEntityModel(context.getPart(EntityTestingClient.MODEL_CUBE_LAYER)), 0.5f);
    }

    @Override
    public CubeEntityRenderState createRenderState() {
        return new CubeEntityRenderState();
    }

    @Override
    public Identifier getTexture(CubeEntityRenderState state) {
        return Identifier.of("entitytesting", "textures/entity/cube/cube.png");
    }
}