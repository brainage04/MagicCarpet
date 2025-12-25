package io.github.brainage04.magic_carpet.entity.renderer;

import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import org.jspecify.annotations.NonNull;

public abstract class MagicCarpetEntityRenderer extends EntityRenderer<MagicCarpetEntity, MagicCarpetEntityRenderState> {
    public abstract String getCarpetType();

    public static final ModelLayerLocation ENTITY_MODEL_LAYER =
            new ModelLayerLocation(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "magic_carpet"), "main");
    private final Identifier texture;
    private final EntityModel<MagicCarpetEntityRenderState> model;

    public MagicCarpetEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.texture = ENTITY_MODEL_LAYER.model().withPath(path -> "textures/entity/%s_%s.png".formatted(getCarpetType(), path));
        this.model = new MagicCarpetEntityModel(context.bakeLayer(ENTITY_MODEL_LAYER));
    }

    protected EntityModel<MagicCarpetEntityRenderState> getModel() {
        return this.model;
    }

    protected RenderType getRenderLayer() {
        return this.model.renderType(this.texture);
    }

    @Override
    public void submit(MagicCarpetEntityRenderState state, PoseStack matrices, SubmitNodeCollector queue, @NonNull CameraRenderState cameraState) {
        matrices.pushPose();

        // DO NOT CHANGE THE ORDER OF THESE STATEMENTS
        // horizontal rotation (along controlling passenger's Y axis)
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F - state.yaw));
        // forward rotation (along controlling passenger's X axis)
        matrices.mulPose(Axis.XP.rotationDegrees(state.pitch));
        // sideways rotation (along controlling passenger's Z axis)
        matrices.mulPose(Axis.ZP.rotationDegrees(state.roll));

        matrices.translate(0.0F, -1.5F, 0.0F);

        EntityModel<MagicCarpetEntityRenderState> entityModel = this.getModel();
        entityModel.setupAnim(state);

        queue.submitModel(
                entityModel,
                state,
                matrices,
                this.getRenderLayer(),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor,
                null
        );

        matrices.popPose();

        super.submit(state, matrices, queue, cameraState);
    }

    @Override
    public MagicCarpetEntityRenderState createRenderState() {
        return new MagicCarpetEntityRenderState();
    }

    @Override
    public void extractRenderState(MagicCarpetEntity entity, MagicCarpetEntityRenderState state, float tickDelta) {
        state.yaw = entity.getYRot(tickDelta);
        state.pitch = Mth.rotLerp(tickDelta, entity.prevRenderPitch, entity.renderPitch);
        state.roll = Mth.rotLerp(tickDelta, entity.prevRenderRoll, entity.renderRoll);

        super.extractRenderState(entity, state, tickDelta);
    }
}