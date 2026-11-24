package io.github.brainage04.magic_carpet;

import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Turns the rider's camera for the film, eased and drawn across the client's frames.
 *
 * <p>The turn is set at the end of each client tick, after the tick has copied the rotation into
 * the old one ({@code Entity.setOldRot}, called from {@code Entity.baseTick}), so a frame draws the
 * camera between the previous tick's angle and this tick's, at its own partial tick. The test
 * thread cannot do this: the Fabric gametest API runs it between the client's task queue and its
 * tick ({@code MinecraftMixin.postRunTasksHook}), so the tick that follows a turn made there copies
 * the turned rotation into the old one and every frame of that tick draws it with nothing to
 * interpolate. That is a jump once a tick, 20 a second, in a film of 30 frames a second.
 */
final class CameraTurn {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    /** Client thread only. */
    private static Plan plan;

    /** A turn that eases one angle from where it is by {@code degrees} over {@code ticks}. */
    private static final class Plan {
        private final CameraFrames.Axis axis;
        private final float from;
        private final float degrees;
        private final int ticks;
        private int done;

        private Plan(CameraFrames.Axis axis, float from, float degrees, int ticks) {
            this.axis = axis;
            this.from = from;
            this.degrees = degrees;
            this.ticks = ticks;
        }
    }

    private CameraTurn() {}

    private static void install() {
        if (INSTALLED.compareAndSet(false, true)) {
            ClientTickEvents.END_CLIENT_TICK.register(CameraTurn::apply);
        }
    }

    /** A smoothstep: no speed at either end, the most in the middle. */
    static float ease(float progress) {
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static void apply(Minecraft client) {
        Plan current = plan;
        if (current == null || client.player == null) {
            return;
        }
        current.done++;
        float angle = current.from + current.degrees * ease((float) current.done / current.ticks);
        LocalPlayer player = client.player;
        if (current.axis == CameraFrames.Axis.YAW) {
            player.setYRot(angle);
            // The drawn head interpolates yHeadRotO to yHeadRot (LivingEntityRenderer.java:242 in
            // 26.3), so the head turns with the body, as the rider's own turn would.
            player.setYHeadRot(angle);
        } else {
            player.setXRot(angle);
        }
        if (current.done >= current.ticks) {
            plan = null;
        }
    }

    /**
     * Turns the rider by {@code degrees} of yaw over {@code ticks} client ticks and fails unless
     * the frames drawn during it moved evenly. {@code tickWait} waits one tick, and runs {@code
     * ticks} plus {@link #SETTLE_TICKS} times.
     */
    static void yaw(ClientGameTestContext context, float degrees, int ticks, Runnable tickWait) {
        run(context, CameraFrames.Axis.YAW, "yaw turn", degrees, ticks, tickWait);
    }

    /**
     * Turns the rider's pitch to {@code target} over {@code ticks} client ticks, as {@link #yaw}.
     */
    static void pitchTo(ClientGameTestContext context, float target, int ticks, Runnable tickWait) {
        float from = context.computeOnClient(client -> client.player.getXRot());
        run(context, CameraFrames.Axis.PITCH, "look down", target - from, ticks, tickWait);
    }

    /** Ticks the frame log keeps running after a turn, so the frames that end it are drawn. */
    private static final int SETTLE_TICKS = 2;

    private static void run(
            ClientGameTestContext context,
            CameraFrames.Axis axis,
            String what,
            float degrees,
            int ticks,
            Runnable tickWait) {
        install();
        CameraFrames.start(context);
        int startTick =
                context.computeOnClient(
                        client -> {
                            float from =
                                    axis == CameraFrames.Axis.YAW
                                            ? client.player.getYRot()
                                            : client.player.getXRot();
                            plan = new Plan(axis, from, degrees, ticks);
                            return CameraFrames.ticks();
                        });
        for (int tick = 0; tick < ticks + SETTLE_TICKS; tick++) {
            tickWait.run();
        }
        CameraFrames.verify(what, CameraFrames.stop(context), axis, startTick, ticks, degrees);
    }
}
