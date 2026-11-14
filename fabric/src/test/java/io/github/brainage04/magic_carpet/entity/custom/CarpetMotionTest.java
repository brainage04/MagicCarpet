package io.github.brainage04.magic_carpet.entity.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.From;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.FloatRange;
import net.jqwik.api.constraints.Size;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class CarpetMotionTest {
    private static final double EPSILON = 1.0E-9;

    /** Mth.sin and Mth.cos come from a float table, so a unit direction is one only to float precision. */
    private static final double SPEED_TOLERANCE = 1.0E-6;

    record Tick(Input input, float pitch, float yaw) {}

    @Provide
    Arbitrary<Input> inputs() {
        Arbitrary<Boolean> key = Arbitraries.of(true, false);
        return Combinators.combine(key, key, key, key, key, key, key).as(Input::new);
    }

    @Provide
    Arbitrary<Tick> ticks() {
        return Combinators.combine(
                        inputs(),
                        Arbitraries.floats().between(-90.0F, 90.0F),
                        Arbitraries.floats().between(-10.0F, 10.0F))
                .as(Tick::new);
    }

    @Property
    void theKeysNeverAskForMoreThanTheTopSpeed(
            @ForAll("ticks") Tick tick,
            @ForAll @DoubleRange(min = 0.01, max = 10.0) double maxSpeed) {
        Vec3 target = CarpetMotion.targetVelocity(tick.input(), tick.pitch(), tick.yaw(), maxSpeed);
        assertTrue(
                target.horizontalDistance() <= maxSpeed * (1.0 + SPEED_TOLERANCE),
                "horizontal " + target.horizontalDistance() + " above " + maxSpeed);
        assertTrue(Math.abs(target.y) <= maxSpeed / 2.0, "vertical " + target.y);
    }

    @Property
    void anyMovementKeyAsksForTheTopSpeedAndOpposingKeysForNone(
            @ForAll("ticks") Tick tick,
            @ForAll @DoubleRange(min = 0.01, max = 10.0) double maxSpeed) {
        Input input = tick.input();
        boolean moving = input.forward() != input.backward() || input.left() != input.right();
        Vec3 target = CarpetMotion.targetVelocity(input, tick.pitch(), tick.yaw(), maxSpeed);
        assertEquals(
                moving ? maxSpeed : 0.0,
                target.horizontalDistance(),
                maxSpeed * SPEED_TOLERANCE,
                "horizontal speed for " + input);
    }

    @Property
    void onlyTheJumpKeyMovesTheCarpetUpOrDown(
            @ForAll("ticks") Tick tick,
            @ForAll @DoubleRange(min = 0.01, max = 10.0) double maxSpeed) {
        Vec3 target = CarpetMotion.targetVelocity(tick.input(), tick.pitch(), tick.yaw(), maxSpeed);
        double expected =
                tick.input().jump() ? CarpetMotion.verticalShare(tick.pitch()) * maxSpeed : 0.0;
        assertEquals(expected, target.y, EPSILON);
    }

    @Test
    void theKeysFollowTheRidersYaw() {
        Input forward = new Input(true, false, false, false, false, false, false);
        Vec3 south = CarpetMotion.targetVelocity(forward, 0.0F, 0.0F, 2.0);
        assertEquals(0.0, south.x, 1.0E-6);
        assertEquals(2.0, south.z, 1.0E-6);
        Vec3 west = CarpetMotion.targetVelocity(forward, 0.0F, (float) (Math.PI / 2.0), 2.0);
        assertEquals(-2.0, west.x, 1.0E-6);
        assertEquals(0.0, west.z, 1.0E-6);
        Input left = new Input(false, false, true, false, false, false, false);
        Vec3 east = CarpetMotion.targetVelocity(left, 0.0F, 0.0F, 2.0);
        assertEquals(2.0, east.x, 1.0E-6);
        assertEquals(0.0, east.z, 1.0E-6);
    }

    @Property
    void aFlightNeverExceedsTheTopSpeed(
            @ForAll @Size(max = 200) List<@From("ticks") Tick> flight,
            @ForAll @DoubleRange(min = 0.01, max = 10.0) double maxSpeed,
            @ForAll @DoubleRange(min = 0.05, max = 5.0) double accelerationTime) {
        Vec3 velocity = Vec3.ZERO;
        for (Tick tick : flight) {
            Vec3 target =
                    CarpetMotion.targetVelocity(tick.input(), tick.pitch(), tick.yaw(), maxSpeed);
            velocity = CarpetMotion.smoothVelocity(velocity, target, accelerationTime);
            assertTrue(
                    velocity.horizontalDistance() <= maxSpeed * (1.0 + SPEED_TOLERANCE),
                    "horizontal " + velocity.horizontalDistance() + " above " + maxSpeed);
            assertTrue(
                    Math.abs(velocity.y) <= maxSpeed / 2.0 * (1.0 + SPEED_TOLERANCE),
                    "vertical " + velocity.y);
        }
    }

    @Property
    void holdingAKeyReaches99PercentOfTheTopSpeedInTheAccelerationTime(
            @ForAll @DoubleRange(min = 0.01, max = 10.0) double maxSpeed,
            @ForAll @DoubleRange(min = 0.05, max = 5.0) double accelerationTime) {
        Input forward = new Input(true, false, false, false, false, false, false);
        Vec3 target = CarpetMotion.targetVelocity(forward, 0.0F, 0.0F, maxSpeed);
        Vec3 velocity = Vec3.ZERO;
        int ticks = (int) Math.ceil(accelerationTime * 20.0 - EPSILON);
        for (int tick = 0; tick < ticks; tick++) {
            velocity = CarpetMotion.smoothVelocity(velocity, target, accelerationTime);
        }
        assertTrue(
                velocity.horizontalDistance() >= 0.99 * maxSpeed * (1.0 - SPEED_TOLERANCE),
                velocity.horizontalDistance() + " after " + ticks + " ticks");
    }

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
