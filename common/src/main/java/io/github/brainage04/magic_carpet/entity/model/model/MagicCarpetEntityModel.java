package io.github.brainage04.magic_carpet.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

/**
 * The carpet's tassels and curled corners, and the pose the body is drawn from. The body itself is
 * one surface, {@link MagicCarpetBodyMesh}, because eight rigid boxes leave a crack at every joint.
 *
 * <p>The texture layout (96x48 texture units) is shared with {@code
 * tools/generate_carpet_assets.py}, which paints the textures and item models; change both
 * together.
 */
public class MagicCarpetEntityModel extends EntityModel<MagicCarpetEntityRenderState> {
    public static final int TEXTURE_WIDTH = 96;
    public static final int TEXTURE_HEIGHT = 48;

    private static final float[] TASSEL_X = {-6.0F, -3.0F, 0.0F, 3.0F, 6.0F};
    private static final float TASSEL_DROOP = 0.6F;
    private static final float CURL_PITCH = 0.9F;
    private static final float CURL_ROLL = 0.5F;

    /** The curl's flap ends inside the roll, which covers its front face. */
    private static final Set<Direction> CURL_FLAP_SIDES = without(Direction.NORTH);

    private final ModelPart carpet;
    private final ModelPart[] segments = new ModelPart[MagicCarpetPose.SEGMENTS];
    private final ModelPart[] frontTassels;
    private final ModelPart[] backTassels;
    private final ModelPart[] curls;

    public MagicCarpetEntityModel(ModelPart root) {
        super(root);
        this.carpet = root.getChild("carpet");
        for (int i = 0; i < MagicCarpetPose.SEGMENTS; i++) {
            this.segments[i] = this.carpet.getChild(segmentName(i));
        }

        ModelPart front = this.segments[0];
        ModelPart back = this.segments[MagicCarpetPose.SEGMENTS - 1];
        boolean tassels = front.hasChild(tasselName("front", 0));
        this.frontTassels = new ModelPart[tassels ? TASSEL_X.length : 0];
        this.backTassels = new ModelPart[this.frontTassels.length];
        for (int i = 0; i < this.frontTassels.length; i++) {
            this.frontTassels[i] = front.getChild(tasselName("front", i));
            this.backTassels[i] = back.getChild(tasselName("back", i));
        }

        this.curls =
                front.hasChild("left_curl")
                        ? new ModelPart[] {
                            front.getChild("left_curl"), front.getChild("right_curl")
                        }
                        : new ModelPart[0];
    }

    public static LayerDefinition createBodyLayer(boolean tassels, boolean curls) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition carpet =
                mesh.getRoot().addOrReplaceChild("carpet", CubeListBuilder.create(), PartPose.ZERO);

        for (int i = 0; i < MagicCarpetPose.SEGMENTS; i++) {
            boolean first = i == 0;
            boolean last = i == MagicCarpetPose.SEGMENTS - 1;
            boolean cutCorners = first && curls;

            PartDefinition segment =
                    carpet.addOrReplaceChild(
                            segmentName(i),
                            CubeListBuilder.create(),
                            PartPose.offset(
                                    0.0F,
                                    1.0F,
                                    MagicCarpetPose.FRONT_Z + MagicCarpetPose.SEGMENT_LENGTH * i));

            if (tassels && first) {
                for (int t = 0; t < TASSEL_X.length; t++) {
                    segment.addOrReplaceChild(
                            tasselName("front", t),
                            CubeListBuilder.create()
                                    .texOffs(44, 40)
                                    .addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                            PartPose.offset(TASSEL_X[t], -0.5F, 0.0F));
                }
            }
            if (tassels && last) {
                for (int t = 0; t < TASSEL_X.length; t++) {
                    segment.addOrReplaceChild(
                            tasselName("back", t),
                            CubeListBuilder.create()
                                    .texOffs(44, 40)
                                    .addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                            PartPose.offset(TASSEL_X[t], -0.5F, MagicCarpetPose.SEGMENT_LENGTH));
                }
            }
            if (cutCorners) {
                addCurl(segment, "left_curl", -1.0F);
                addCurl(segment, "right_curl", 1.0F);
            }
        }

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /**
     * The flap stops where the roll starts, so the two never share a plane: the roll covers the
     * flap's last pixel of depth with the same width and a taller box, so the surface the player
     * sees is the same. The flap's front face is then inside the roll and is left out.
     */
    private static void addCurl(PartDefinition segment, String name, float side) {
        float innerX = side < 0 ? -4.0F : 0.0F;
        segment.addOrReplaceChild(
                name,
                CubeListBuilder.create()
                        .mirror(side > 0)
                        .texOffs(68, 0)
                        .addBox(innerX, -1.0F, -3.0F, 4.0F, 1.0F, 3.0F, CURL_FLAP_SIDES)
                        .texOffs(68, 5)
                        .addBox(innerX, -1.0F, -5.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(
                        8.0F * side,
                        0.0F,
                        MagicCarpetPose.SEGMENT_LENGTH,
                        CURL_PITCH,
                        0.0F,
                        CURL_ROLL * side));
    }

    private static Set<Direction> without(Direction... hidden) {
        Set<Direction> sides = EnumSet.allOf(Direction.class);
        sides.removeAll(EnumSet.copyOf(List.of(hidden)));
        return sides;
    }

    private static String segmentName(int index) {
        return "segment_" + index;
    }

    private static String tasselName(String end, int index) {
        return end + "_tassel_" + index;
    }

    /**
     * Puts the matrices where the body is drawn: the carpet part's own hover and sway, which the
     * tassels and curls get from {@link #setupAnim} through the part tree.
     */
    public static void transformBody(PoseStack matrices, MagicCarpetPose pose) {
        matrices.translate(0.0F, (pose.y() + 1.0F) / 16.0F, 0.0F);
        matrices.rotate(new Quaternionf().rotationZYX(pose.zRot(), 0.0F, pose.xRot()));
    }

    /** Draws the body at the pose the render state carries. */
    public void renderBody(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            MagicCarpetPose carpet,
            int light,
            int overlay,
            int color) {
        MagicCarpetBodyMesh.render(
                pose, buffer, carpet, this.curls.length > 0, light, overlay, color);
    }

    @Override
    public void setupAnim(MagicCarpetEntityRenderState state) {
        super.setupAnim(state);

        float time = state.ageInTicks + state.animationOffset;
        float speed = state.speed;
        float calm = 1.0F - 0.6F * speed;

        MagicCarpetPose pose = state.pose;
        pose.set(state);
        this.carpet.y = pose.y();
        this.carpet.zRot = pose.zRot();
        this.carpet.xRot = pose.xRot();

        float[] heights = pose.heights();
        for (int i = 0; i < MagicCarpetPose.SEGMENTS; i++) {
            ModelPart segment = this.segments[i];
            segment.y += heights[i];
            segment.xRot =
                    (float)
                            -Math.atan2(
                                    heights[i + 1] - heights[i], MagicCarpetPose.SEGMENT_LENGTH);
        }

        for (int i = 0; i < this.backTassels.length; i++) {
            float flutter = (0.12F + 0.3F * speed) * Mth.sin(state.flutterPhase + i * 1.3F);
            float sway = 0.12F * Mth.sin(time * 0.09F + i * 2.1F) * calm;
            this.backTassels[i].xRot = Mth.lerp(speed, TASSEL_DROOP, 0.2F) + flutter;
            this.backTassels[i].yRot = sway;
            this.frontTassels[i].xRot = -(Mth.lerp(speed, TASSEL_DROOP, 2.2F) + flutter);
            this.frontTassels[i].yRot = sway;
        }

        for (ModelPart curl : this.curls) {
            curl.xRot += 0.05F * Mth.sin(time * 0.1F) + 0.1F * speed;
        }
    }
}
