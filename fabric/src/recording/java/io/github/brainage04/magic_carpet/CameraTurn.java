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
 * the old one ({@code Entity.commonTick} calls {@code setOldPosAndRot}, {@code Entity.java:522} in
 * 26.3, and {@code ClientLevel.tickPassenger} calls it for a rider), so a frame draws the camera
 * between the previous tick's angle and this tick's, at its own partial tick. The test thread
 * cannot do this: the Fabric gametest API runs it after {@code Minecraft.runAllTasks} and before
 * {@code Minecraft.tick} ({@code MinecraftMixin.postRunTasksHook}, {@code Minecraft.java:1163-1178}
 * in 26.3), so the tick that follows a turn made there copies the turned rotation into the old one
 * and every frame of that tick draws it with nothing to interpolate. That is a jump once a tick, 20
 * a second, in a film of 30 frames a second.
 */
final class CameraTurn {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    /** Client thread only. */
    private static Plan plan;

    /** A turn that eases one angle, and how many of its ticks have been set. */
    private static final class Plan {
        private final CameraFrames.Turn turn;
        private int done;

        private Plan(CameraFrames.Turn turn) {
            this.turn = turn;
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
        float angle = (float) current.turn.set(current.done);
        LocalPlayer player = client.player;
        if (current.turn.axis() == CameraFrames.Axis.YAW) {
            player.setYRot(angle);
            // The drawn head interpolates yHeadRotO to yHeadRot (LivingEntityRenderer.java:242 in
            // 26.3), so the head turns with the body, as the rider's own turn would.
            player.setYHeadRot(angle);
        } else {
            player.setXRot(angle);
        }
        if (current.done >= current.turn.ticks()) {
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
    private static final int SETTLE_TICKS = 3;

    private static void run(
            ClientGameTestContext context,
            CameraFrames.Axis axis,
            String what,
            float degrees,
            int ticks,
            Runnable tickWait) {
        install();
        CameraFrames.start(context);
        CameraFrames.Turn turn =
                context.computeOnClient(
                        client -> {
                            float from =
                                    axis == CameraFrames.Axis.YAW
                                            ? client.player.getYRot()
                                            : client.player.getXRot();
                            CameraFrames.Turn started =
                                    new CameraFrames.Turn(
                                            axis, from, degrees, ticks, CameraFrames.ticks());
                            plan = new Plan(started);
                            return started;
                        });
        for (int tick = 0; tick < ticks + SETTLE_TICKS; tick++) {
            tickWait.run();
        }
        CameraFrames.verify(what, CameraFrames.stop(context), turn, CameraFrames.isRecording());
    }
}
