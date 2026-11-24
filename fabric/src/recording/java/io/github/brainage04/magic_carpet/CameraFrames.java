package io.github.brainage04.magic_carpet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * into the old one ({@code Entity.commonTick}, {@code Entity.java:522} in 26.3) before any frame is
 * drawn.
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
     * Two frames that straddle one tick of a smoothstep over N ticks differ by 4/N of its peak
     * speed: 0.33 over 12 ticks and 0.2 over 20. A turn that steps once a tick changes by all of
     * it.
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
        double direction = Math.signum(totalDegrees);
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
            double speed = step / gap * direction;
            worstStep = Math.max(worstStep, Math.abs(step));
            peak = Math.max(peak, Math.abs(speed));
            if (!Double.isNaN(previousSpeed)) {
                worstChange = Math.max(worstChange, Math.abs(speed - previousSpeed));
            }
            previousSpeed = speed;
            if (before.gameTicks() >= middleFrom && after.gameTicks() <= middleTo) {
                slowest = Math.min(slowest, speed);
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
        if (Double.isInfinite(measured.slowestMiddleSpeed())) {
            throw new AssertionError(
                    "The camera "
                            + what
                            + " must be drawn in at least two frames in the middle of the turn: "
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

    /**
     * Runs {@link #verify} on made-up frames of a turn drawn two ways, so a check that stops
     * reaching what it guards fails the film before the film shows it. A turn eased between ticks
     * must pass; one that jumps once a tick, as a turn set before the tick does, a turn with no
     * frame in its middle, and one that turns back for a frame must each fail.
     */
    static void selfTest() {
        int ticks = 20;
        float degrees = 90.0F;
        List<Frame> eased = drawn(ticks, degrees, true);
        verify("self-test, eased", eased, Axis.YAW, 0, ticks, degrees);
        List<Frame> turnsBack = new ArrayList<>(eased);
        Frame middle = turnsBack.get(turnsBack.size() / 2);
        turnsBack.set(
                turnsBack.size() / 2,
                new Frame(middle.gameTicks(), middle.nanos(), middle.yaw() - 8.0F, middle.pitch()));
        Map<String, List<Frame>> broken =
                Map.of(
                        "jumps once a tick", drawn(ticks, degrees, false),
                        "turns back for a frame", turnsBack,
                        "has no frame in its middle",
                                eased.stream()
                                        .filter(f -> f.gameTicks() < 5.0D || f.gameTicks() > 16.0D)
                                        .toList());
        for (Map.Entry<String, List<Frame>> entry : broken.entrySet()) {
            try {
                verify(
                        "self-test, " + entry.getKey(),
                        entry.getValue(),
                        Axis.YAW,
                        0,
                        ticks,
                        degrees);
            } catch (AssertionError expected) {
                continue;
            }
            throw new AssertionError(
                    "The frame check must fail a turn that " + entry.getKey() + ", and it passed");
        }
    }

    /**
     * The frames of a smoothstep turn of {@code degrees} over {@code ticks} ticks, drawn at 30 a
     * second: between two ticks' angles when {@code interpolated}, and holding the tick's angle
     * when not, the way a turn set before the tick is drawn.
     */
    private static List<Frame> drawn(int ticks, float degrees, boolean interpolated) {
        List<Frame> frames = new ArrayList<>();
        for (double game = 0.0D; game <= ticks + 3; game += 20.0D / 30.0D) {
            int tick = (int) game;
            double along = interpolated ? game - tick : 0.0D;
            double angle =
                    degrees * Mth.lerp(along, planned(tick, ticks), planned(tick + 1, ticks));
            frames.add(new Frame(game, (long) (game * 5.0E7D), (float) angle, 0.0F));
        }
        return frames;
    }

    private static double planned(int tick, int ticks) {
        return CameraTurn.ease(Math.min(1.0F, (float) tick / ticks));
    }
}
