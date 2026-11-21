package io.github.brainage04.magic_carpet.entity.model;

import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Where the carpet's body sits this frame: its own hover and sway, and the height of each of the
 * nine joints the body bends at. The model poses the tassels and curls from it and the renderer
 * draws the body from it, so the two cannot drift apart.
 */
public final class MagicCarpetPose {
    public static final int SEGMENTS = 8;
    public static final float SEGMENT_LENGTH = 4.0F;
    public static final float FRONT_Z = -16.0F;

    private static final float HOVER_HEIGHT = 1.0F;

    private final float[] heights = new float[SEGMENTS + 1];
    private float y;
    private float zRot;
    private float xRot;

    public void set(MagicCarpetEntityRenderState state) {
        float time = state.ageInTicks + state.animationOffset;
        float speed = state.speed;
        float calm = 1.0F - 0.6F * speed;

        this.y = HOVER_HEIGHT + 0.5F * Mth.sin(time * 0.1F) * calm;
        this.zRot = 0.03F * Mth.sin(time * 0.07F) * calm;
        this.xRot = 0.015F * Mth.sin(time * 0.05F + 1.0F) * calm;

        float amplitude = 0.4F + 1.1F * speed;
        for (int i = 0; i <= SEGMENTS; i++) {
            float along = (float) i / SEGMENTS;
            float z = SEGMENT_LENGTH * i;
            this.heights[i] =
                    amplitude * (0.5F + along) * Mth.sin(z * 0.22F - state.ripplePhase)
                            + 1.4F * speed * along * along;
        }
    }

    /** The height of each joint, from the front of the carpet to its back. */
    public float[] heights() {
        return this.heights;
    }

    public float y() {
        return this.y;
    }

    public float zRot() {
        return this.zRot;
    }

    public float xRot() {
        return this.xRot;
    }
}
