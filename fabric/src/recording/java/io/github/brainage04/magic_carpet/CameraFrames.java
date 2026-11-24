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
 * into the old one ({@code Entity.commonTick}, {@code Entity.java:522} in 26.3) before any frame is
 * drawn.
 */
final class CameraFrames {
    private static final AtomicBoolean INSTALLED = new AtomicBoolean();

    /** Client thread only. */
    private static int clientTicks;

    private static boolean recording;
    private static List<Frame> frames = new ArrayList<>();

    /**
     * What a frame drew: the client tick it was drawn after plus the partial tick it was drawn at
     * ({@code gameTicks}, the camera's own time axis), the clocks, and the camera's angles.
     */
    record Frame(double gameTicks, long nanos, long epochMillis, float yaw, float pitch) {}

    /** Which of the camera's two angles a check reads. */
    enum Axis {
        YAW,
        PITCH;

        float of(Frame frame) {
            return this == YAW ? frame.yaw() : frame.pitch();
        }
    }

    /**
     * A turn of one angle by {@code degrees} over {@code ticks} client ticks, which began after
     * client tick {@code startTick} from the angle {@code from}. {@link CameraTurn} sets its angle
     * at the end of each tick, so the frames draw {@link #planned} one tick late.
     */
    record Turn(Axis axis, float from, float degrees, int ticks, int startTick) {

        /** The angle the turn sets at the end of its {@code done}th tick. */
        double set(int done) {
            return from + degrees * CameraTurn.ease(Mth.clamp((float) done / ticks, 0.0F, 1.0F));
        }

        /**
         * The angle a frame drawn at {@code gameTicks} shows: between the angles of the two ticks
         * around it, since the frame draws the old and the new rotation at its partial tick.
         */
        double planned(double gameTicks) {
            double done = gameTicks - startTick - 1;
            if (done <= 0.0D) {
                return from;
            }
            int whole = (int) done;
            return Mth.lerp(done - whole, set(whole), set(whole + 1));
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
            double worstPlanError,
            double longestGapMillis,
            double framesPerSecond) {

        String describe() {
            return String.format(
                    "%d frames at %.1f a second, %.2f degrees in all, mean speed %.2f and peak"
                            + " speed %.2f degrees a tick, steepest frame step %.2f degrees, speed"
                            + " change between neighbouring frames up to %.2f degrees a tick,"
                            + " slowest pair of frames in the middle of the turn %.2f degrees a"
                            + " tick, furthest frame from the planned curve %.2f degrees, longest"
                            + " gap between frames %.0f ms",
                    frames,
                    framesPerSecond,
                    totalDegrees,
                    meanSpeed,
                    peakSpeed,
                    worstFrameStep,
                    worstSpeedChange,
                    slowestMiddleSpeed,
                    worstPlanError,
                    longestGapMillis);
        }
    }

    /** Frames closer than this in game time say nothing about speed, only about rounding. */
    private static final double MIN_FRAME_GAP_TICKS = 0.05D;

    /** The least share of the mean speed a pair of frames in the middle of a turn may move at. */
    private static final double MIN_MIDDLE_SHARE = 0.5D;

    /**
     * The most the speed may change between two neighbouring frames, as a share of the peak speed.
     * Two frames {@code g} ticks apart that straddle one tick of a smoothstep over N ticks differ
     * by 4g/N of its peak speed: at the film's 30 frames a second (g = 2/3) 0.33 over 8 ticks and
     * 0.13 over 20. A turn that steps once a tick changes by all of it.
     */
    private static final double MAX_SPEED_CHANGE = 0.6D;

    /**
     * The furthest a frame's angle may be from the planned curve, in degrees. The frame and the
     * plan both come from the same partial tick, so the difference is float rounding; a turn drawn
     * in steps, or a tick late, is off by a degree or more at any frame rate.
     */
    private static final double MAX_PLAN_ERROR = 0.25D;

    /** The film's frame rate, which a recording's capture clock runs at. */
    private static final double FILM_FPS = 30.0D;

    /**
     * The longest real time between two drawn frames of a turn, in film frames. A longer gap is a
     * stall, which the film shows as a repeated picture followed by a jump.
     */
    private static final double MAX_GAP_FRAMES = 1.5D;

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
        // True, as the camera's own update reads it (Camera.java:111-112 in 26.3).
        frames.add(
                new Frame(
                        clientTicks + deltaTracker.getGameTimeDeltaPartialTick(true),
                        System.nanoTime(),
                        System.currentTimeMillis(),
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

    /** True in a recording, where a stall is a defect of the film and not only of the run. */
    static boolean isRecording() {
        return System.getenv("GTR_RECORDING_PROFILE") != null;
    }

    /**
     * Measures a turn from the frames drawn during it. {@code slowestMiddleSpeed} is the smallest
     * speed between two frames in the middle half of the turn, where an eased turn moves at least
     * as fast as its mean: a camera that holds still between ticks reads 0 there, and a frame pair
     * that turns back reads below 0.
     */
    static Evenness measure(List<Frame> frames, Turn turn) {
        double worstStep = 0.0D;
        double peak = 0.0D;
        double worstChange = 0.0D;
        double slowest = Double.POSITIVE_INFINITY;
        double worstPlan = 0.0D;
        double longestGap = 0.0D;
        double previousSpeed = Double.NaN;
        double direction = Math.signum(turn.degrees());
        double middleFrom = turn.startTick() + 0.25D * turn.ticks() + 1.0D;
        double middleTo = turn.startTick() + 0.75D * turn.ticks();
        for (int i = 0; i < frames.size(); i++) {
            Frame after = frames.get(i);
            worstPlan =
                    Math.max(
                            worstPlan,
                            Math.abs(
                                    Mth.wrapDegrees(
                                            turn.axis().of(after)
                                                    - turn.planned(after.gameTicks()))));
            if (i == 0) {
                continue;
            }
            Frame before = frames.get(i - 1);
            longestGap = Math.max(longestGap, (after.nanos() - before.nanos()) / 1.0E6D);
            double gap = after.gameTicks() - before.gameTicks();
            if (gap < MIN_FRAME_GAP_TICKS) {
                continue;
            }
            double step = Mth.wrapDegrees(turn.axis().of(after) - turn.axis().of(before));
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
                        : Mth.wrapDegrees(
                                turn.axis().of(frames.getLast())
                                        - turn.axis().of(frames.getFirst())),
                Math.abs(turn.degrees()) / turn.ticks(),
                peak,
                worstStep,
                worstChange,
                slowest,
                worstPlan,
                longestGap,
                seconds > 0.0D ? (frames.size() - 1) / seconds : 0.0D);
    }

    /**
     * Fails unless the frames drew the turn as an even motion: a turn of some size, enough frames,
     * every pair in the middle of the turn moving at about the turn's own speed, no pair's speed
     * more than {@link #MAX_SPEED_CHANGE} of the peak away from its neighbour's, in a recording no
     * gap between frames over {@link #MAX_GAP_FRAMES} film frames, and every frame on the planned
     * curve. The last test does not depend on the frame rate, so it fails a turn drawn in steps
     * even when every frame spans a tick or more.
     *
     * <p>The log line {@code TURN_WINDOW} gives the wall-clock span of the frames, for the
     * acceptance check of a finished film.
     */
    static Evenness verify(String what, List<Frame> frames, Turn turn, boolean strictTiming) {
        Evenness measured = measure(frames, turn);
        MagicCarpet.LOGGER.info("Camera {} across the drawn frames: {}", what, measured.describe());
        MagicCarpet.LOGGER.info(
                "Camera {}: the gap between frames is {}",
                what,
                strictTiming ? "enforced" : "logged only");
        if (!frames.isEmpty()) {
            MagicCarpet.LOGGER.info(
                    "TURN_WINDOW {} {} {}",
                    what.replace(' ', '_'),
                    frames.getFirst().epochMillis(),
                    frames.getLast().epochMillis());
        }
        String detail = ": " + measured.describe();
        String prefix = "The camera " + what;
        if (Math.abs(turn.degrees()) < 1.0F) {
            throw new AssertionError(
                    prefix + " must turn by more than 1 degree, not 0 degrees" + detail);
        }
        if (measured.frames() < turn.ticks() / 2) {
            throw new AssertionError(
                    prefix
                            + " must be drawn in at least "
                            + turn.ticks() / 2
                            + " frames, and "
                            + measured.frames()
                            + " were drawn"
                            + detail);
        }
        if (Double.isInfinite(measured.slowestMiddleSpeed())) {
            throw new AssertionError(
                    prefix
                            + " must be drawn in at least two frames in the middle of the turn"
                            + detail);
        }
        double maxGapMillis = MAX_GAP_FRAMES * 1000.0D / FILM_FPS;
        if (strictTiming && measured.longestGapMillis() > maxGapMillis) {
            throw new AssertionError(
                    prefix
                            + " must have no gap between frames longer than "
                            + maxGapMillis
                            + " ms, a stall the film shows as a repeated picture"
                            + detail);
        }
        if (measured.slowestMiddleSpeed() < MIN_MIDDLE_SHARE * measured.meanSpeed()) {
            throw new AssertionError(
                    prefix
                            + " must move between every two frames in the middle of the turn, at"
                            + " least "
                            + MIN_MIDDLE_SHARE
                            + " of its mean speed"
                            + detail);
        }
        if (measured.worstSpeedChange() > MAX_SPEED_CHANGE * measured.peakSpeed()) {
            throw new AssertionError(
                    prefix
                            + " must speed up and slow down evenly, with no change over "
                            + MAX_SPEED_CHANGE
                            + " of the peak speed between neighbouring frames"
                            + detail);
        }
        if (measured.worstPlanError() > MAX_PLAN_ERROR) {
            throw new AssertionError(
                    prefix
                            + " must follow the planned curve to within "
                            + MAX_PLAN_ERROR
                            + " degrees at every frame"
                            + detail);
        }
        return measured;
    }

    /**
     * Runs {@link #verify} on made-up frames of a turn, so that a check that stops reaching what it
     * guards fails the film before the film shows it. A turn eased between ticks must pass, also
     * with a frame drawn twice. Each way a turn goes wrong must fail for its own reason, named by
     * the message: a jump once a tick, a staircase drawn at 13 frames a second (which only the
     * planned curve sees), a turn a half tick late, one that turns back for a frame, one with too
     * few frames, one with no frame in its middle, one of 0 degrees, and, in a recording, a stall.
     */
    static void selfTest() {
        int ticks = 20;
        float degrees = 90.0F;
        Turn turn = new Turn(Axis.YAW, 0.0F, degrees, ticks, 0);
        double filmGap = 20.0D / FILM_FPS;
        List<Frame> eased = drawn(turn, filmGap, true, 0.0D);
        verify("self-test, eased", eased, turn, true);
        List<Frame> twice = new ArrayList<>(eased);
        twice.add(twice.size() / 2, twice.get(twice.size() / 2));
        verify("self-test, a frame drawn twice", twice, turn, true);
        List<Frame> turnsBack = new ArrayList<>(eased);
        Frame middle = turnsBack.get(turnsBack.size() / 2);
        turnsBack.set(
                turnsBack.size() / 2,
                new Frame(
                        middle.gameTicks(),
                        middle.nanos(),
                        middle.epochMillis(),
                        middle.yaw() - 8.0F,
                        middle.pitch()));
        List<Frame> stalled = new ArrayList<>(eased);
        for (int i = 0; i < 3; i++) {
            stalled.remove(stalled.size() / 2);
        }
        expectFailure(
                "jumps once a tick",
                "move between every two frames",
                drawn(turn, filmGap, false, 0.0D),
                turn,
                true);
        expectFailure(
                "is a staircase at 13 frames a second",
                "planned curve",
                drawn(turn, 1.5D, false, 0.0D),
                turn,
                true);
        expectFailure(
                "is half a tick late",
                "planned curve",
                drawn(turn, filmGap, true, 0.5D),
                turn,
                true);
        expectFailure(
                "turns back for a frame", "move between every two frames", turnsBack, turn, true);
        expectFailure(
                "has too few frames", "at least", drawn(turn, 4 * filmGap, true, 0.0D), turn, true);
        expectFailure(
                "has no frame in its middle",
                "in the middle of the turn",
                eased.stream().filter(f -> f.gameTicks() < 5.0D || f.gameTicks() > 16.0D).toList(),
                turn,
                true);
        expectFailure(
                "is of 0 degrees",
                "0 degrees",
                eased,
                new Turn(Axis.YAW, 0.0F, 0.0F, ticks, 0),
                true);
        expectFailure("stalls in a recording", "no gap between frames", stalled, turn, true);
        verify("self-test, a stall outside a recording", stalled, turn, false);
    }

    private static void expectFailure(
            String how, String because, List<Frame> frames, Turn turn, boolean strictTiming) {
        try {
            verify("self-test, " + how, frames, turn, strictTiming);
        } catch (AssertionError failure) {
            if (failure.getMessage() != null && failure.getMessage().contains(because)) {
                return;
            }
            throw new AssertionError(
                    "The frame check must fail a turn that "
                            + how
                            + " because of \""
                            + because
                            + "\", and it failed with: "
                            + failure.getMessage());
        }
        throw new AssertionError(
                "The frame check must fail a turn that " + how + ", and it passed");
    }

    /**
     * The frames of {@code turn} drawn {@code gap} ticks apart, from the first turn tick to three
     * after the last, at the planned curve shifted {@code late} ticks: between two ticks' angles
     * when {@code interpolated}, and holding the tick's angle when not, the way a turn set before
     * the tick is drawn. A tick is 50 ms.
     */
    private static List<Frame> drawn(Turn turn, double gap, boolean interpolated, double late) {
        List<Frame> frames = new ArrayList<>();
        for (double game = 0.0D; game <= turn.ticks() + 3; game += gap) {
            double angle =
                    interpolated
                            ? turn.planned(game - late)
                            : turn.set((int) game - turn.startTick());
            frames.add(new Frame(game, (long) (game * 5.0E7D), 0L, (float) angle, 0.0F));
        }
        return frames;
    }
}
