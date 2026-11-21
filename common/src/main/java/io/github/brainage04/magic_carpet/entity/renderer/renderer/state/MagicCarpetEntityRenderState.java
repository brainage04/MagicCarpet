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
     * Where the body sits this frame. The renderer draws the body from it when the frame is
     * submitted and the model poses the tassels from it when the frame is drawn, so it is filled
     * once, here, rather than by either of them.
     */
    public final MagicCarpetPose pose = new MagicCarpetPose();
}
