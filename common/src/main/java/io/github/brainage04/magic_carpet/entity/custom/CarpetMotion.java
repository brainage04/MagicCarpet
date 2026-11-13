package io.github.brainage04.magic_carpet.entity.custom;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The carpet's flight arithmetic, apart from the entity so that unit tests can reach it. */
public final class CarpetMotion {
    /** The rider's pitch from which the jump key flies the carpet down at full speed. */
    public static final float DIVE_PITCH = 45.0F;

    private static final double DECELERATION_TIME_MULTIPLIER = 2.0;

    private CarpetMotion() {}

    /**
     * The share of the remaining way to a target speed that one tick covers, chosen so that {@code
     * seconds} of ticks cover 99 % of it.
     */
    public static double smoothingFactor(double seconds) {
        return 1.0 - Math.pow(0.01, 1.0 / (seconds * 20.0));
    }

    /**
     * One tick from {@code current} towards {@code target}. Each axis that would get slower moves
     * at half the rate, so the carpet coasts for twice its acceleration time.
     */
    public static Vec3 smoothVelocity(Vec3 current, Vec3 target, double accelerationTime) {
        double acceleration = smoothingFactor(accelerationTime);
        double deceleration = smoothingFactor(accelerationTime * DECELERATION_TIME_MULTIPLIER);

        double horizontal =
                target.horizontalDistanceSqr() < current.horizontalDistanceSqr()
                        ? deceleration
                        : acceleration;
        double vertical = Math.abs(target.y) < Math.abs(current.y) ? deceleration : acceleration;

        return new Vec3(
                Mth.lerp(horizontal, current.x, target.x),
                Mth.lerp(vertical, current.y, target.y),
                Mth.lerp(horizontal, current.z, target.z));
    }

    /**
     * The vertical speed the jump key asks for, as a share of the top speed: half up while the
     * rider looks at most 15 degrees down, half down from {@link #DIVE_PITCH}, and a straight line
     * between.
     */
    public static float verticalShare(float pitch) {
        if (pitch >= DIVE_PITCH) {
            return -0.50f;
        } else if (pitch > 15.0f) {
            return (-pitch + 30.0f) / 30.0f;
        } else {
            return 0.50f;
        }
    }

    /** The input along one axis from its two keys: 1, -1, or 0 when both or neither are held. */
    public static float impulse(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0F;
        }
        return positive ? 1.0F : -1.0F;
    }
}
