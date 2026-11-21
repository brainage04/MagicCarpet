package io.github.brainage04.magic_carpet.entity.renderer.state;

import io.github.brainage04.magic_carpet.entity.model.MagicCarpetPose;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class MagicCarpetEntityRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float roll;

    public float speed;

    public float ripplePhase;

    public float flutterPhase;

    public float animationOffset;

    public int shape;
    public boolean glow;

    /**
     * Where the body sits this frame. The renderer reads it when the frame is submitted and the
     * model refills it when the frame is drawn; both compute it from this state, so they cannot
     * disagree.
     */
    public final MagicCarpetPose pose = new MagicCarpetPose();
}
