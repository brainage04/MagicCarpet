package io.github.brainage04.magic_carpet.entity.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.FloatRange;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CarpetMotionTest {
    private static final double EPSILON = 1.0E-9;

    @Test
    void theShippedAccelerationTimesReach99PercentOnTime() {
        for (double seconds : new double[] {0.5, 1.0, 2.0}) {
            double remaining = 1.0;
            double factor = CarpetMotion.smoothingFactor(seconds);
            int ticks = (int) Math.round(seconds * 20);
            for (int tick = 0; tick < ticks; tick++) {
                remaining -= remaining * factor;
            }
            assertEquals(0.01, remaining, 1.0E-12, "remaining share after " + seconds + " s");
        }
    }

    @Property
    void aTickCoversPartOfTheWayButNeverAllOfIt(
            @ForAll @DoubleRange(min = 0.05, max = 60.0) double seconds) {
        double factor = CarpetMotion.smoothingFactor(seconds);
        assertTrue(factor > 0.0 && factor < 1.0, "factor " + factor + " for " + seconds + " s");
    }

    @Property
    void aLongerAccelerationTimeIsSlower(
            @ForAll @DoubleRange(min = 0.05, max = 30.0) double seconds,
            @ForAll @DoubleRange(min = 1.01, max = 4.0) double longer) {
        assertTrue(
                CarpetMotion.smoothingFactor(seconds * longer)
                        < CarpetMotion.smoothingFactor(seconds));
    }

    @Property
    void smoothingNeverOvershootsTheTarget(
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double currentX,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double currentY,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double currentZ,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double targetX,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double targetY,
            @ForAll @DoubleRange(min = -3.0, max = 3.0) double targetZ,
            @ForAll @DoubleRange(min = 0.05, max = 10.0) double seconds) {
        Vec3 current = new Vec3(currentX, currentY, currentZ);
        Vec3 target = new Vec3(targetX, targetY, targetZ);
        Vec3 next = CarpetMotion.smoothVelocity(current, target, seconds);
        assertBetween(currentX, targetX, next.x);
        assertBetween(currentY, targetY, next.y);
        assertBetween(currentZ, targetZ, next.z);
    }

    @Test
    void slowingDownTakesTwiceAsLongAsSpeedingUp() {
        double seconds = 1.0;
        Vec3 speedingUp = CarpetMotion.smoothVelocity(Vec3.ZERO, new Vec3(1.0, 1.0, 0.0), seconds);
        Vec3 slowingDown = CarpetMotion.smoothVelocity(new Vec3(1.0, 1.0, 0.0), Vec3.ZERO, seconds);
        assertEquals(CarpetMotion.smoothingFactor(seconds), speedingUp.x, EPSILON);
        assertEquals(CarpetMotion.smoothingFactor(seconds), speedingUp.y, EPSILON);
        assertEquals(1.0 - CarpetMotion.smoothingFactor(seconds * 2.0), slowingDown.x, EPSILON);
        assertEquals(1.0 - CarpetMotion.smoothingFactor(seconds * 2.0), slowingDown.y, EPSILON);
    }

    @Test
    void horizontalAndVerticalChooseTheirRatesApart() {
        Vec3 next =
                CarpetMotion.smoothVelocity(new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0), 1.0);
        assertEquals(1.0 - CarpetMotion.smoothingFactor(2.0), next.x, EPSILON);
        assertEquals(CarpetMotion.smoothingFactor(1.0), next.y, EPSILON);
    }

    @Test
    void anEqualHorizontalSpeedCountsAsSpeedingUp() {
        Vec3 next =
                CarpetMotion.smoothVelocity(new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 1.0);
        assertEquals(1.0 - CarpetMotion.smoothingFactor(1.0), next.x, EPSILON);
    }

    @Test
    void anEqualVerticalSpeedCountsAsSpeedingUp() {
        Vec3 next =
                CarpetMotion.smoothVelocity(new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, -1.0, 0.0), 1.0);
        assertEquals(1.0 - 2.0 * CarpetMotion.smoothingFactor(1.0), next.y, EPSILON);
    }

    @Test
    void theJumpKeyClimbsDivesAndBlendsBetween() {
        assertEquals(0.5F, CarpetMotion.verticalShare(-90.0F));
        assertEquals(0.5F, CarpetMotion.verticalShare(15.0F));
        assertEquals(0.3F, CarpetMotion.verticalShare(21.0F), 1.0E-6F);
        assertEquals(0.0F, CarpetMotion.verticalShare(30.0F), 1.0E-6F);
        assertEquals(-0.3F, CarpetMotion.verticalShare(39.0F), 1.0E-6F);
        assertEquals(-0.5F, CarpetMotion.verticalShare(CarpetMotion.DIVE_PITCH));
        assertEquals(-0.5F, CarpetMotion.verticalShare(90.0F));
    }

    @Property
    void theJumpKeyNeverAsksForMoreThanHalfAndFallsWithPitch(
            @ForAll @FloatRange(min = -90.0F, max = 90.0F) float pitch,
            @ForAll @FloatRange(min = 0.0F, max = 10.0F) float lower) {
        float share = CarpetMotion.verticalShare(pitch);
        assertTrue(share >= -0.5F && share <= 0.5F, "share " + share + " at " + pitch);
        assertTrue(CarpetMotion.verticalShare(pitch + lower) <= share);
    }

    @Test
    void opposingKeysCancel() {
        assertEquals(1.0F, CarpetMotion.impulse(true, false));
        assertEquals(-1.0F, CarpetMotion.impulse(false, true));
        assertEquals(0.0F, CarpetMotion.impulse(true, true));
        assertEquals(0.0F, CarpetMotion.impulse(false, false));
    }

    private static void assertBetween(double from, double to, double value) {
        double low = Math.min(from, to);
        double high = Math.max(from, to);
        assertTrue(
                value >= low - EPSILON && value <= high + EPSILON,
                value + " must lie between " + from + " and " + to);
    }
}
