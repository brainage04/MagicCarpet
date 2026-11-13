package io.github.brainage04.magic_carpet.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.brainage04.magic_carpet.MagicCarpet;
import io.github.brainage04.magic_carpet.entity.custom.CarpetTier;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import io.github.brainage04.magic_carpet.entity.model.MagicCarpetEntityModel;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
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
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.NonNull;

/**
 * Draws every carpet tier. The texture comes from the entity type's name ({@code
 * textures/entity/<name>.png}, plus {@code <name>_glow.png} for a glowing tier); the optional parts
 * come from the tier's data, so the model of each part combination is baked once and picked per
 * carpet.
 */
public class MagicCarpetEntityRenderer
        extends EntityRenderer<MagicCarpetEntity, MagicCarpetEntityRenderState> {
    /** One model layer per combination of tassels and curls, indexed by {@link #shape}. */
    public static final List<ModelLayerLocation> MODEL_LAYERS =
            List.of(
                    modelLayer("plain"),
                    modelLayer("tassels"),
                    modelLayer("curls"),
                    modelLayer("tassels_and_curls"));

    private final MagicCarpetEntityModel[] models = new MagicCarpetEntityModel[MODEL_LAYERS.size()];
    private final RenderType renderType;
    private final RenderType emissiveRenderType;

    public MagicCarpetEntityRenderer(
            EntityRendererProvider.Context context, EntityType<MagicCarpetEntity> type) {
        super(context);
        for (int shape = 0; shape < this.models.length; shape++) {
            this.models[shape] =
                    new MagicCarpetEntityModel(context.bakeLayer(MODEL_LAYERS.get(shape)));
        }
        Identifier name = EntityType.getKey(type);
        this.renderType = this.models[0].renderType(texture(name, ""));
        this.emissiveRenderType = RenderTypes.eyes(texture(name, "_glow"));
    }

    /** The body layer of the shape at {@code shape} in {@link #MODEL_LAYERS}. */
    public static LayerDefinition createBodyLayer(int shape) {
        return MagicCarpetEntityModel.createBodyLayer((shape & 1) != 0, (shape & 2) != 0);
    }

    /** The index in {@link #MODEL_LAYERS} of a tier's model. */
    public static int shape(CarpetTier.Model model) {
        return (model.tassels() ? 1 : 0) | (model.curls() ? 2 : 0);
    }

    private static ModelLayerLocation modelLayer(String shape) {
        return new ModelLayerLocation(
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "magic_carpet"), shape);
    }

    private static Identifier texture(Identifier name, String suffix) {
        return name.withPath(path -> "textures/entity/" + path + suffix + ".png");
    }

    @Override
    public void submit(
            MagicCarpetEntityRenderState state,
            PoseStack matrices,
            SubmitNodeCollector queue,
            @NonNull CameraRenderState cameraState) {
        matrices.pushPose();

        // DO NOT CHANGE THE ORDER OF THESE STATEMENTS
        matrices.rotateDegrees(Axis.YP, 180.0F - state.yaw);
        matrices.rotateDegrees(Axis.XP, state.pitch);
        matrices.rotateDegrees(Axis.ZP, state.roll);

        MagicCarpetEntityModel model = this.models[state.shape];
        queue.submitModel(
                model,
                state,
                matrices,
                this.renderType,
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                state.outlineColor);
        if (state.glow) {
            queue.submitModel(
                    model,
                    state,
                    matrices,
                    this.emissiveRenderType,
                    LightCoordsUtil.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    state.outlineColor);
        }

        matrices.popPose();

        super.submit(state, matrices, queue, cameraState);
    }

    @Override
    public MagicCarpetEntityRenderState createRenderState() {
        return new MagicCarpetEntityRenderState();
    }

    @Override
    public void extractRenderState(
            MagicCarpetEntity entity, MagicCarpetEntityRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);

        state.yaw = entity.getYRot(tickDelta);
        state.pitch = Mth.rotLerp(tickDelta, entity.prevRenderPitch, entity.renderPitch);
        state.roll = Mth.rotLerp(tickDelta, entity.prevRenderRoll, entity.renderRoll);
        state.speed = Mth.lerp(tickDelta, entity.prevAnimationSpeed, entity.animationSpeed);
        state.animationOffset = (entity.getId() * 37) % 1000;
        CarpetTier.Model model = entity.tier().model();
        state.shape = shape(model);
        state.glow = model.glow();
    }
}
