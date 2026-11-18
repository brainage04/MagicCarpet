package io.github.brainage04.magic_carpet.entity.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CarpetAnimationTest {
    private static final float EPSILON = 1.0E-4F;

    /**
     * The ripple's phase must move by one tick's worth of frequency per tick, whatever the speed
     * does. Computing it as {@code time * frequency} instead moves it by {@code time * change} when
     * the frequency changes, which is what made the basic carpet vibrate as it accelerated and
     * spasm again when it slowed.
     */
    @Test
    void aSpeedChangeDoesNotJumpThePhase() {
        float phase = 0.0F;
        float speed = 0.0F;
        for (int tick = 0; tick < 200; tick++) {
            float before = phase;
            phase = CarpetAnimation.advancePhase(phase, CarpetAnimation.rippleFrequency(speed));
            float step = Mth.wrapDegrees((phase - before) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            assertEquals(
                    CarpetAnimation.rippleFrequency(speed),
                    step,
                    EPSILON,
                    "tick " + tick + " at speed " + speed);
            speed = Math.min(1.0F, speed + 0.02F);
        }
    }

    /** The phase stays in one turn, so a long flight cannot lose the wave to float rounding. */
    @Test
    void thePhaseStaysInOneTurn() {
        float phase = 0.0F;
        for (int tick = 0; tick < 100_000; tick++) {
            phase = CarpetAnimation.advancePhase(phase, CarpetAnimation.rippleFrequency(1.0F));
            assertTrue(phase >= 0.0F && phase < Mth.TWO_PI, "phase " + phase + " at tick " + tick);
        }
    }

    @Test
    void thePhaseBetweenTicksMovesForward() {
        float from = Mth.TWO_PI - 0.1F;
        float to = CarpetAnimation.advancePhase(from, 0.2F);
        float half = CarpetAnimation.lerpPhase(from, to, 0.5F);
        assertEquals(
                0.1F * Mth.RAD_TO_DEG,
                Mth.wrapDegrees((half - from) * Mth.RAD_TO_DEG),
                0.01F,
                "half a tick past the end of a turn is half a tick forward, not most of a turn"
                        + " back");
    }

    /** A carpet at rest has no ripple speed, and one at the top speed has all of it. */
    @Test
    void theSpeedShareFollowsTheVelocity() {
        assertEquals(0.0F, CarpetAnimation.speedTarget(Vec3.ZERO, 0.6), EPSILON);
        assertEquals(
                1.0F, CarpetAnimation.speedTarget(new Vec3(0.6, 0.0, 0.0), 0.6), EPSILON);
        assertEquals(
                0.5F, CarpetAnimation.speedTarget(new Vec3(0.0, 0.0, 0.3), 0.6), EPSILON);
        assertEquals(
                1.0F,
                CarpetAnimation.speedTarget(new Vec3(6.0, 0.0, 0.0), 0.6),
                EPSILON,
                "a carpet faster than its top speed still animates at full speed");
    }

    /** The animation speed eases towards its target and never overshoots it. */
    @Test
    void theAnimationSpeedEasesWithoutOvershoot() {
        float speed = 0.0F;
        for (int tick = 0; tick < 100; tick++) {
            speed = CarpetAnimation.advanceSpeed(speed, 1.0F, 0.25F);
            assertTrue(speed >= 0.0F && speed <= 1.0F, "speed " + speed + " at tick " + tick);
        }
        assertTrue(speed > 0.99F, "the speed must reach its target, reached " + speed);
    }
}
