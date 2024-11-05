package io.github.brainage04.magic_carpet.entity.renderer;

import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class MagicCarpetEntityRenderer extends EntityRenderer<MagicCarpetEntity, MagicCarpetEntityRenderState> {
    private final Identifier texture;
    private final EntityModel<MagicCarpetEntityRenderState> model;

    public MagicCarpetEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.texture = MagicCarpetEntityModel.ENTITY_MODEL_LAYER.id().withPath((path) -> "textures/entity/" + path + ".png");
        this.model = new MagicCarpetEntityModel(context.getPart(MagicCarpetEntityModel.ENTITY_MODEL_LAYER));
    }

    protected EntityModel<MagicCarpetEntityRenderState> getModel() {
        return this.model;
    }

    protected RenderLayer getRenderLayer() {
        return this.model.getLayer(this.texture);
    }

    @Override
    public void render(MagicCarpetEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));

        matrices.translate(0.0F, -1.5F, 0.0F);

        EntityModel<MagicCarpetEntityRenderState> entityModel = this.getModel();
        entityModel.setAngles(state);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.getRenderLayer());
        entityModel.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        
        super.render(state, matrices, vertexConsumers, light);
    }

    @Override
    public MagicCarpetEntityRenderState createRenderState() {
        return new MagicCarpetEntityRenderState();
    }

    @Override
    public void updateRenderState(MagicCarpetEntity entity, MagicCarpetEntityRenderState state, float tickDelta) {
        LivingEntity livingEntity = entity.getControllingPassenger();

        if (livingEntity instanceof PlayerEntity player) {
            state.yaw = player.getLerpedYaw(tickDelta);
        }

        super.updateRenderState(entity, state, tickDelta);
    }
}