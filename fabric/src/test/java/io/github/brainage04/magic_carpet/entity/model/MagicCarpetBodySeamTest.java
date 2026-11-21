package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The body bends at eight joints. Where one segment ends and the next begins, the two must share
 * the same ring of vertices, and no face may be drawn across the joint: a gap there shows the
 * inside of the carpet as a lighter line, and two end faces meeting at an angle show a V of their
 * own texture, because the render type does not cull back faces (RenderPipelines.java:507 in 26.3).
 */
class MagicCarpetBodySeamTest {
    private static final int SEGMENTS = 8;
    private static final float SEGMENT_LENGTH = 4.0F;
    private static final float FRONT_Z = -16.0F;
    private static final float BOTTOM = -1.0F / 16.0F;
    private static final float SLAB_TOP = 0.0F;
    private static final float CUT_HALF_WIDTH = 8.0F / 16.0F;

    /**
     * The widest step between neighbouring vertices of a closed ring: the body is 24 units wide.
     */
    private static final double MAX_STEP = 24.0F / 16.0F;

    /** Model units are 1/16 of a block, so a vertex is placed to well under a texel. */
    private static final float EPSILON = 1.0E-4F;

    /**
     * A face whose normal points along the body is an end face, which only the two ends may draw.
     */
    private static final float END_FACE = 0.5F;

    @Test
    void bodySurfaceMeetsAtEveryJoint() {
        for (int shape = 0; shape < 4; shape++) {
            boolean curls = (shape & 2) != 0;
            Body body = body(shape);
            List<Quad> quads = body.quads();
            assertFalse(quads.isEmpty(), "Shape " + shape + " draws no body");

            for (int joint = 1; joint < SEGMENTS; joint++) {
                float z = (FRONT_Z + SEGMENT_LENGTH * joint) / 16.0F;
                Set<String> before = ring(quads, z, true);
                Set<String> after = ring(quads, z, false);
                assertTrue(
                        before.size() >= 4,
                        "Shape " + shape + " joint " + joint + " has no ring before it: " + before);
                assertEquals(
                        before,
                        after,
                        "Shape "
                                + shape
                                + " joint "
                                + joint
                                + " leaves a gap: the segment before ends at "
                                + before
                                + " and the one after starts at "
                                + after);
                float top = body.heights()[joint] / 16.0F;
                assertTrue(
                        hasHeight(before, top) && hasHeight(before, top + BOTTOM - SLAB_TOP),
                        "Shape "
                                + shape
                                + " joint "
                                + joint
                                + " does not close the body top and bottom: "
                                + before);
                assertTrue(
                        closed(before, top),
                        "Shape "
                                + shape
                                + " joint "
                                + joint
                                + " leaves the body open between its top and bottom: "
                                + before);
                List<Quad> endFaces = endFaces(quads, z);
                if (curls && joint == 1) {
                    // The curled corners cut the first segment's corners, so the body steps out to
                    // its full width there and the step's faces are drawn. They must stay outside
                    // the cut segment's own cross-section, which continues into the next segment.
                    assertFalse(
                            endFaces.isEmpty(),
                            "Shape " + shape + " draws no step where the cut corners end");
                    for (Quad face : endFaces) {
                        assertTrue(
                                face.outsideCutCorner(),
                                "Shape "
                                        + shape
                                        + " draws an end face inside the cut segment at joint "
                                        + joint
                                        + ": "
                                        + face.name());
                    }
                } else {
                    assertTrue(
                            endFaces.isEmpty(),
                            "Shape "
                                    + shape
                                    + " joint "
                                    + joint
                                    + " draws end faces across the joint: "
                                    + endFaces);
                }
            }
        }
    }

    private record Body(List<Quad> quads, float[] heights) {}

    /**
     * The body at the worst case the rigid boxes had: full speed, where the ripple is deepest, at
     * the tick the ripple's phase puts the sharpest bend at the middle joints.
     */
    private static Body body(int shape) {
        MagicCarpetEntityModel model =
                new MagicCarpetEntityModel(
                        MagicCarpetEntityModel.createBodyLayer((shape & 1) != 0, (shape & 2) != 0)
                                .bakeRoot());
        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.ageInTicks = 80.0F;
        state.speed = 1.0F;
        model.setupAnim(state);

        Recorder recorder = new Recorder();
        model.renderBody(new PoseStack().last(), recorder, state.pose, 0, 0, -1);
        return new Body(recorder.quads, state.pose.heights().clone());
    }

    /**
     * The vertices on the joint's plane, from the quads that end there or start there. A quad that
     * spans the plane without a vertex on it would leave the joint unclosed, so it is not counted.
     */
    private static Set<String> ring(List<Quad> quads, float z, boolean before) {
        Set<String> ring = new LinkedHashSet<>();
        for (Quad quad : quads) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (int vertex = 0; vertex < 4; vertex++) {
                min = Math.min(min, quad.z(vertex));
                max = Math.max(max, quad.z(vertex));
            }
            if (before ? max > z + EPSILON : min < z - EPSILON) {
                continue;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                if (Math.abs(quad.z(vertex) - z) <= EPSILON) {
                    ring.add(key(quad.x(vertex), quad.y(vertex), quad.z(vertex)));
                }
            }
        }
        return ring;
    }

    /** The end faces drawn on the joint's plane. */
    private static List<Quad> endFaces(List<Quad> quads, float z) {
        List<Quad> faces = new ArrayList<>();
        for (Quad quad : quads) {
            if (Math.abs(quad.normalZ()) < END_FACE) {
                continue;
            }
            boolean onPlane = true;
            for (int vertex = 0; vertex < 4; vertex++) {
                onPlane &= Math.abs(quad.z(vertex) - z) <= EPSILON;
            }
            if (onPlane) {
                faces.add(quad);
            }
        }
        return faces;
    }

    /**
     * True when the ring's vertices, walked in order of angle around the body's cross-section, step
     * from one to the next by no more than the body's own spacing: a missing side wall or top strip
     * leaves a jump across the cross-section.
     */
    private static boolean closed(Set<String> ring, float top) {
        List<double[]> points = new ArrayList<>();
        for (String vertex : ring) {
            String[] parts = vertex.split(",");
            points.add(
                    new double[] {
                        Double.parseDouble(parts[0]) / 1.0E4F, Double.parseDouble(parts[1]) / 1.0E4F
                    });
        }
        double centreY = top + (BOTTOM - SLAB_TOP) / 2.0F;
        points.sort(
                java.util.Comparator.comparingDouble(
                        point -> Math.atan2(point[1] - centreY, point[0])));
        for (int i = 0; i < points.size(); i++) {
            double[] from = points.get(i);
            double[] to = points.get((i + 1) % points.size());
            double step = Math.hypot(to[0] - from[0], to[1] - from[1]);
            if (step > MAX_STEP) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasHeight(Set<String> ring, float y) {
        String height = "," + Math.round(y * 1.0E4F) + ",";
        return ring.stream().anyMatch(vertex -> vertex.contains(height));
    }

    private static String key(float x, float y, float z) {
        return Math.round(x * 1.0E4F) + "," + Math.round(y * 1.0E4F) + "," + Math.round(z * 1.0E4F);
    }

    /** One quad the body draws: its four positions and the normal it was given. */
    private record Quad(String name, float[] positions, float normalZ) {
        /** True when no corner of the quad is inside the cut first segment's cross-section. */
        private boolean outsideCutCorner() {
            for (int vertex = 0; vertex < 4; vertex++) {
                if (Math.abs(this.x(vertex)) < CUT_HALF_WIDTH - EPSILON
                        && this.y(vertex) < SLAB_TOP - EPSILON) {
                    return false;
                }
            }
            return true;
        }

        private float x(int vertex) {
            return this.positions[vertex * 3];
        }

        private float y(int vertex) {
            return this.positions[vertex * 3 + 1];
        }

        private float z(int vertex) {
            return this.positions[vertex * 3 + 2];
        }
    }

    /** Keeps the four positions and the normal of each quad the body draws. */
    private static final class Recorder implements VertexConsumer {
        private final List<Quad> quads = new ArrayList<>();
        private final float[] positions = new float[12];
        private float normalZ;
        private int vertices;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.positions[this.vertices * 3] = x;
            this.positions[this.vertices * 3 + 1] = y;
            this.positions[this.vertices * 3 + 2] = z;
            this.vertices++;
            if (this.vertices == 4) {
                this.quads.add(
                        new Quad(
                                "quad#" + this.quads.size(), this.positions.clone(), this.normalZ));
                this.vertices = 0;
            }
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.normalZ = z;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
