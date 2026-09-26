package io.github.brainage04.magic_carpet.entity.model;

import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A 24x32 pixel carpet (front towards -Z, up towards +Y) split into eight 4-pixel segments so it can ripple.
 * <p>
 * The texture layout (96x48 texture units) is shared with {@code tools/generate_carpet_assets.py}, which paints the
 * textures and item models; change both together.
 */
public class MagicCarpetEntityModel extends EntityModel<MagicCarpetEntityRenderState> {
    public static final int TEXTURE_WIDTH = 96;
    public static final int TEXTURE_HEIGHT = 48;

    private static final int SEGMENTS = 8;
    private static final float SEGMENT_LENGTH = 4.0F;
    private static final float FRONT_Z = -16.0F;
    private static final float[] TASSEL_X = {-6.0F, -3.0F, 0.0F, 3.0F, 6.0F};
    private static final float TASSEL_DROOP = 0.6F;
    private static final float CURL_PITCH = 0.9F;
    private static final float CURL_ROLL = 0.5F;
    /** Resting height above the entity's origin, so the bob and ripple never sink into the ground. */
    private static final float HOVER_HEIGHT = 1.0F;

    private final ModelPart carpet;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];
    private final ModelPart[] frontTassels;
    private final ModelPart[] backTassels;
    private final ModelPart[] curls;
    private final float[] heights = new float[SEGMENTS + 1];

    public MagicCarpetEntityModel(ModelPart root) {
        super(root);
        this.carpet = root.getChild("carpet");
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i] = this.carpet.getChild(segmentName(i));
        }

        ModelPart front = this.segments[0];
        ModelPart back = this.segments[SEGMENTS - 1];
        boolean tassels = front.hasChild(tasselName("front", 0));
        this.frontTassels = new ModelPart[tassels ? TASSEL_X.length : 0];
        this.backTassels = new ModelPart[this.frontTassels.length];
        for (int i = 0; i < this.frontTassels.length; i++) {
            this.frontTassels[i] = front.getChild(tasselName("front", i));
            this.backTassels[i] = back.getChild(tasselName("back", i));
        }

        this.curls = front.hasChild("left_curl")
                ? new ModelPart[]{front.getChild("left_curl"), front.getChild("right_curl")}
                : new ModelPart[0];
    }

    /**
     * @param tassels add five tassels to the front and back edges
     * @param curls   replace the front corners with curled-up flaps
     */
    public static LayerDefinition createBodyLayer(boolean tassels, boolean curls) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition carpet = mesh.getRoot().addOrReplaceChild("carpet", CubeListBuilder.create(), PartPose.ZERO);

        for (int i = 0; i < SEGMENTS; i++) {
            boolean first = i == 0;
            boolean last = i == SEGMENTS - 1;
            boolean cutCorners = first && curls;

            // each segment hinges on its top front edge, so the visible surface stays seamless as it bends
            CubeListBuilder cubes = cutCorners
                    ? CubeListBuilder.create().texOffs(56, 40).addBox(-8.0F, -1.0F, 0.0F, 16.0F, 1.0F, 4.0F)
                    : CubeListBuilder.create().texOffs(0, 5 * i).addBox(-12.0F, -1.0F, 0.0F, 24.0F, 1.0F, 4.0F);

            // raised border, one pixel above the surface
            if (!cutCorners) {
                cubes.texOffs(56, 5 * i)
                        .addBox(-12.0F, 0.0F, 0.0F, 2.0F, 1.0F, 4.0F)
                        .addBox(10.0F, 0.0F, 0.0F, 2.0F, 1.0F, 4.0F);
            }
            if (cutCorners) {
                cubes.texOffs(0, 43).addBox(-8.0F, 0.0F, 0.0F, 16.0F, 1.0F, 2.0F);
            } else if (first) {
                cubes.texOffs(0, 40).addBox(-10.0F, 0.0F, 0.0F, 20.0F, 1.0F, 2.0F);
            } else if (last) {
                cubes.texOffs(0, 40).addBox(-10.0F, 0.0F, 2.0F, 20.0F, 1.0F, 2.0F);
            }

            PartDefinition segment = carpet.addOrReplaceChild(segmentName(i), cubes,
                    PartPose.offset(0.0F, 1.0F, FRONT_Z + SEGMENT_LENGTH * i));

            if (tassels && first) {
                for (int t = 0; t < TASSEL_X.length; t++) {
                    segment.addOrReplaceChild(tasselName("front", t),
                            CubeListBuilder.create().texOffs(44, 40).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F),
                            PartPose.offset(TASSEL_X[t], -0.5F, 0.0F));
                }
            }
            if (tassels && last) {
                for (int t = 0; t < TASSEL_X.length; t++) {
                    segment.addOrReplaceChild(tasselName("back", t),
                            CubeListBuilder.create().texOffs(44, 40).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 3.0F),
                            PartPose.offset(TASSEL_X[t], -0.5F, SEGMENT_LENGTH));
                }
            }
            if (cutCorners) {
                addCurl(segment, "left_curl", -1.0F);
                addCurl(segment, "right_curl", 1.0F);
            }
        }

        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    /** A 4x4 corner flap hinged at its inner back corner, lifted up and outwards, with a roll along its front edge. */
    private static void addCurl(PartDefinition segment, String name, float side) {
        float innerX = side < 0 ? -4.0F : 0.0F;
        segment.addOrReplaceChild(name,
                CubeListBuilder.create().mirror(side > 0)
                        .texOffs(68, 0).addBox(innerX, -1.0F, -4.0F, 4.0F, 1.0F, 4.0F)
                        .texOffs(68, 5).addBox(innerX, -1.0F, -5.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(8.0F * side, 0.0F, SEGMENT_LENGTH, CURL_PITCH, 0.0F, CURL_ROLL * side));
    }

    private static String segmentName(int index) {
        return "segment_" + index;
    }

    private static String tasselName(String end, int index) {
        return end + "_tassel_" + index;
    }

    @Override
    public void setupAnim(MagicCarpetEntityRenderState state) {
        super.setupAnim(state);

        float time = state.ageInTicks + state.animationOffset;
        float speed = state.speed;
        float calm = 1.0F - 0.6F * speed;

        // hover: bob about 1/16 of a block and sway gently, settling down at speed
        this.carpet.y = HOVER_HEIGHT + 0.5F * Mth.sin(time * 0.1F) * calm;
        this.carpet.zRot = 0.03F * Mth.sin(time * 0.07F) * calm;
        this.carpet.xRot = 0.015F * Mth.sin(time * 0.05F + 1.0F) * calm;

        // a wave travelling from front to back, growing towards the back edge and with speed,
        // plus a trailing lift of the back edge as the carpet drags through the air
        float amplitude = 0.4F + 1.1F * speed;
        float frequency = 0.08F + 0.42F * speed;
        for (int i = 0; i <= SEGMENTS; i++) {
            float along = (float) i / SEGMENTS;
            float z = SEGMENT_LENGTH * i;
            this.heights[i] = amplitude * (0.5F + along) * Mth.sin(z * 0.22F - time * frequency)
                    + 1.4F * speed * along * along;
        }
        for (int i = 0; i < SEGMENTS; i++) {
            ModelPart segment = this.segments[i];
            segment.y += this.heights[i];
            segment.xRot = (float) -Math.atan2(this.heights[i + 1] - this.heights[i], SEGMENT_LENGTH);
        }

        // tassels hang and sway when idle, and stream behind the carpet at speed
        for (int i = 0; i < this.backTassels.length; i++) {
            float flutter = (0.12F + 0.3F * speed) * Mth.sin(time * (0.12F + 0.6F * speed) + i * 1.3F);
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
