package io.github.brainage04.magic_carpet.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public abstract class MagicCarpetEntityRenderer extends EntityRenderer<MagicCarpetEntity, MagicCarpetEntityRenderState> {
    private final MagicCarpetEntityModel model;
    private final RenderType renderType;
    private final @Nullable RenderType emissiveRenderType;

    /**
     * @param carpetType the tier name, e.g. {@code basic}
     * @param emissive   whether {@code textures/entity/<tier>_magic_carpet_glow.png} is drawn at full brightness on top
     */
    protected MagicCarpetEntityRenderer(EntityRendererProvider.Context context, String carpetType, boolean emissive) {
        super(context);
        this.model = new MagicCarpetEntityModel(context.bakeLayer(modelLayer(carpetType)));
        this.renderType = this.model.renderType(texture(carpetType, ""));
        this.emissiveRenderType = emissive ? RenderTypes.eyes(texture(carpetType, "_glow")) : null;
    }

    protected static ModelLayerLocation modelLayer(String carpetType) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, carpetType + "_magic_carpet"), "main");
    }

    private static Identifier texture(String carpetType, String suffix) {
        return Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "textures/entity/%s_magic_carpet%s.png".formatted(carpetType, suffix));
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

        queue.submitModel(this.model, state, matrices, this.renderType,
                state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        if (this.emissiveRenderType != null) {
            queue.submitModel(this.model, state, matrices, this.emissiveRenderType,
                    LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        }

        matrices.popPose();

        super.submit(state, matrices, queue, cameraState);
    }

    @Override
    public MagicCarpetEntityRenderState createRenderState() {
        return new MagicCarpetEntityRenderState();
    }

    @Override
    public void extractRenderState(MagicCarpetEntity entity, MagicCarpetEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);

        state.yaw = entity.getYRot(tickDelta);
        state.pitch = Mth.rotLerp(tickDelta, entity.prevRenderPitch, entity.renderPitch);
        state.roll = Mth.rotLerp(tickDelta, entity.prevRenderRoll, entity.renderRoll);
        state.speed = Mth.lerp(tickDelta, entity.prevAnimationSpeed, entity.animationSpeed);
        state.animationOffset = (entity.getId() * 37) % 1000;
    }
}
