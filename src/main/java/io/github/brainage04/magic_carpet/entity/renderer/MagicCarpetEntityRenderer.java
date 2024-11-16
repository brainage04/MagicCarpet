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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

        // DO NOT CHANGE THE ORDER OF THESE STATEMENTS
        // horizontal rotation (along controlling passenger's Y axis)
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
        // forward rotation (along controlling passenger's X axis)
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(state.pitch));
        // sideways rotation (along controlling passenger's Z axis)
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(state.roll));

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
        state.yaw = entity.getLerpedYaw(tickDelta);

        float yaw = (float) Math.toRadians(state.yaw);

        float sin = MathHelper.sin(yaw);
        float cos = MathHelper.cos(yaw);

        double x = entity.getVelocity().getX();
        double y = entity.getVelocity().getY();
        double z = entity.getVelocity().getZ();

        state.pitch = (float) MathHelper.clamp(
                (y - (cos * z - sin * x)) * 30.0F,
                -45.0F,
                45.0F
        );

        state.roll = (float) MathHelper.clamp(
                (sin * z + cos * x) * 30.0F,
                -30.0F,
                30.0F
        );

        super.updateRenderState(entity, state, tickDelta);
    }
}