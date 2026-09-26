package io.github.brainage04.magic_carpet.entity.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class MagicCarpetEntityRenderState extends EntityRenderState {
    public float yaw;
    public float pitch;
    public float roll;
    /** Horizontal speed as a fraction of the carpet's maximum speed, from 0 to 1. */
    public float speed;
    /** Per-entity phase offset in ticks, so neighbouring carpets don't ripple in lockstep. */
    public float animationOffset;
}
