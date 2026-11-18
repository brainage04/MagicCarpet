package io.github.brainage04.magic_carpet.entity.custom;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The carpet's animation arithmetic, apart from the entity so that unit tests can reach it.
 *
 * <p>The ripple and the tassel flutter run at a frequency that rises with the carpet's speed. Their
 * phase is accumulated tick by tick rather than computed as {@code time * frequency}: with the
 * product, a change of frequency moves the phase by {@code time * change}, which grows without
 * bound, so the wave runs far too fast while the carpet accelerates and again whenever it slows.
 */
public final class CarpetAnimation {
    private static final float RIPPLE_BASE = 0.08F;
    private static final float RIPPLE_PER_SPEED = 0.42F;
    private static final float FLUTTER_BASE = 0.12F;
    private static final float FLUTTER_PER_SPEED = 0.6F;

    private CarpetAnimation() {}

    /** The share of the top speed the carpet is flying at, from 0 to 1. */
    public static float speedTarget(Vec3 velocity, double maxSpeed) {
        return (float) Mth.clamp(velocity.horizontalDistance() / maxSpeed, 0.0, 1.0);
    }

    /** One tick of the animation speed towards {@code target}. */
    public static float advanceSpeed(float current, float target, float easing) {
        return current + (target - current) * easing;
    }

    /** The ripple's angular frequency in radians per tick. */
    public static float rippleFrequency(float speed) {
        return RIPPLE_BASE + RIPPLE_PER_SPEED * speed;
    }

    /** The tassel flutter's angular frequency in radians per tick. */
    public static float flutterFrequency(float speed) {
        return FLUTTER_BASE + FLUTTER_PER_SPEED * speed;
    }

    /** One tick of a phase, kept in [0, 2 pi) so a float never loses the wave to rounding. */
    public static float advancePhase(float phase, float frequency) {
        float next = (phase + frequency) % Mth.TWO_PI;
        return next < 0.0F ? next + Mth.TWO_PI : next;
    }

    /** A phase between two ticks, along the shorter way round the circle. */
    public static float lerpPhase(float from, float to, float delta) {
        float difference = Mth.wrapDegrees((to - from) * Mth.RAD_TO_DEG);
        return advancePhase(from, difference * Mth.DEG_TO_RAD * delta);
    }
}
