package io.github.brainage04.magic_carpet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The camera angles of the frames the client really draws, for the checks that a turn is even on
 * screen. A frame draws the camera at the partial tick of its own moment, so a check that samples
 * the rider's rotation from the test thread measures the rotation the tick leaves behind, not the
 * one a frame shows: the test thread runs between the client's task queue and its tick ({@code
 * MinecraftMixin.postRunTasksHook} in the Fabric gametest API), and the tick copies the rotation
 * into the old one before any frame is drawn.
 */
final class CameraFrames {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    /** Client thread only, except {@link #recording}, which the test thread flips on it. */
    private static int clientTicks;

    private static boolean recording;
    private static List<Frame> frames = new ArrayList<>();

    /**
     * What a frame drew: the client tick it was drawn after plus the partial tick it was drawn at
     * ({@code gameTicks}, the camera's own time axis), the clock, and the camera's angles.
     */
    record Frame(double gameTicks, long nanos, float yaw, float pitch) {}

    /** Which of the camera's two angles a check reads. */
    enum Axis {
        YAW,
        PITCH;

        float of(Frame frame) {
            return this == YAW ? frame.yaw() : frame.pitch();
        }
    }

    /**
     * How a turn looked across the frames drawn during it. Speeds are degrees per tick of game
     * time, so a late frame does not read as a slower turn.
     */
    record Evenness(
            int frames,
            double totalDegrees,
            double meanSpeed,
            double peakSpeed,
            double worstFrameStep,
            double worstSpeedChange,
            double slowestMiddleSpeed,
            double framesPerSecond) {

        String describe() {
            return String.format(
                    "%d frames at %.1f a second, %.2f degrees in all, mean speed %.2f and peak"
                            + " speed %.2f degrees a tick, steepest frame step %.2f degrees, speed"
                            + " change between neighbouring frames up to %.2f degrees a tick,"
                            + " slowest pair of frames in the middle of the turn %.2f degrees a"
                            + " tick",
                    frames,
                    framesPerSecond,
                    totalDegrees,
                    meanSpeed,
                    peakSpeed,
                    worstFrameStep,
                    worstSpeedChange,
                    slowestMiddleSpeed);
        }
    }

    /** Frames closer than this in game time say nothing about speed, only about rounding. */
    private static final double MIN_FRAME_GAP_TICKS = 0.05D;

    /** The least share of the mean speed a pair of frames in the middle of a turn may move at. */
    private static final double MIN_MIDDLE_SHARE = 0.5D;

    /**
     * The most the speed may change between two neighbouring frames, as a share of the peak speed.
     * A turn eased with a smoothstep over 8 ticks or more changes by under a third of its peak
     * between neighbours; one that steps once a tick changes by all of it.
     */
    private static final double MAX_SPEED_CHANGE = 0.6D;

    private CameraFrames() {}

    static void install() {
        if (!INSTALLED.compareAndSet(false, true)) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> clientTicks++);
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.SCOREBOARD,
                Identifier.fromNamespaceAndPath(MagicCarpet.MOD_ID, "camera_frames"),
                CameraFrames::onFrame);
    }

    private static void onFrame(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!recording) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        frames.add(
                new Frame(
                        clientTicks + deltaTracker.getGameTimeDeltaPartialTick(false),
                        System.nanoTime(),
                        camera.yRot(),
                        camera.xRot()));
    }

    /** The client tick count, which only the client thread may change. */
    static int ticks() {
        return clientTicks;
    }

    static void start(ClientGameTestContext context) {
        install();
        context.runOnClient(
                client -> {
                    frames = new ArrayList<>();
                    recording = true;
                });
    }

    static List<Frame> stop(ClientGameTestContext context) {
        return context.computeOnClient(
                client -> {
                    recording = false;
                    return List.copyOf(frames);
                });
    }

    /**
     * Measures a turn of {@code totalDegrees} that began at client tick {@code startTick} and ran
     * for {@code turnTicks}, from the frames drawn during it. {@code slowestMiddleSpeed} is the
     * smallest speed between two frames in the middle half of the turn, where an eased turn moves
     * at least as fast as its mean: a camera that holds still between ticks reads 0 there.
     */
    static Evenness measure(
            List<Frame> frames, Axis axis, int startTick, int turnTicks, float totalDegrees) {
        double worstStep = 0.0D;
        double peak = 0.0D;
        double worstChange = 0.0D;
        double slowest = Double.POSITIVE_INFINITY;
        double previousSpeed = Double.NaN;
        double middleFrom = startTick + 0.25D * turnTicks + 1.0D;
        double middleTo = startTick + 0.75D * turnTicks;
        for (int i = 1; i < frames.size(); i++) {
            Frame before = frames.get(i - 1);
            Frame after = frames.get(i);
            double gap = after.gameTicks() - before.gameTicks();
            if (gap < MIN_FRAME_GAP_TICKS) {
                continue;
            }
            double step = Mth.wrapDegrees(axis.of(after) - axis.of(before));
            double speed = step / gap;
            worstStep = Math.max(worstStep, Math.abs(step));
            peak = Math.max(peak, Math.abs(speed));
            if (!Double.isNaN(previousSpeed)) {
                worstChange = Math.max(worstChange, Math.abs(speed - previousSpeed));
            }
            previousSpeed = speed;
            if (before.gameTicks() >= middleFrom && after.gameTicks() <= middleTo) {
                slowest = Math.min(slowest, Math.abs(speed));
            }
        }
        double seconds =
                frames.size() < 2
                        ? 0.0D
                        : (frames.getLast().nanos() - frames.getFirst().nanos()) / 1.0E9D;
        return new Evenness(
                frames.size(),
                frames.isEmpty()
                        ? 0.0D
                        : Mth.wrapDegrees(axis.of(frames.getLast()) - axis.of(frames.getFirst())),
                Math.abs(totalDegrees) / turnTicks,
                peak,
                worstStep,
                worstChange,
                slowest,
                seconds > 0.0D ? (frames.size() - 1) / seconds : 0.0D);
    }

    /**
     * Fails unless the frames drew the turn as an even motion: enough frames, every pair of frames
     * in the middle of the turn moving at about the turn's own speed, and no pair's speed more than
     * {@link #MAX_SPEED_CHANGE} of the peak away from its neighbour's.
     */
    static Evenness verify(
            String what,
            List<Frame> frames,
            Axis axis,
            int startTick,
            int turnTicks,
            float totalDegrees) {
        Evenness measured = measure(frames, axis, startTick, turnTicks, totalDegrees);
        MagicCarpet.LOGGER.info("Camera {} across the drawn frames: {}", what, measured.describe());
        if (measured.frames() < turnTicks / 2) {
            throw new AssertionError(
                    "The camera "
                            + what
                            + " must be drawn in at least "
                            + turnTicks / 2
                            + " frames, and "
                            + measured.frames()
                            + " were drawn: "
                            + measured.describe());
        }
        if (measured.slowestMiddleSpeed() < MIN_MIDDLE_SHARE * measured.meanSpeed()) {
            throw new AssertionError(
                    "The camera "
                            + what
                            + " must move between every two frames in the middle of the turn, at"
                            + " least "
                            + MIN_MIDDLE_SHARE
                            + " of its mean speed, and two frames drew it at "
                            + measured.slowestMiddleSpeed()
                            + " degrees a tick of "
                            + measured.meanSpeed()
                            + ": "
                            + measured.describe());
        }
        if (measured.worstSpeedChange() > MAX_SPEED_CHANGE * measured.peakSpeed()) {
            throw new AssertionError(
                    "The camera "
                            + what
                            + " must speed up and slow down evenly, and the speed changed by "
                            + measured.worstSpeedChange()
                            + " degrees a tick between two neighbouring frames, over "
                            + MAX_SPEED_CHANGE
                            + " of the peak "
                            + measured.peakSpeed()
                            + ": "
                            + measured.describe());
        }
        return measured;
    }
}
