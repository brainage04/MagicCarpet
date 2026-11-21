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
 * the same ring of vertices: a gap there shows the inside of the carpet as a lighter line across
 * the top, the sides and the underside.
 */
class MagicCarpetBodySeamTest {
    private static final int SEGMENTS = 8;
    private static final float SEGMENT_LENGTH = 4.0F;
    private static final float FRONT_Z = -16.0F;
    private static final float BOTTOM = -1.0F;
    private static final float SLAB_TOP = 0.0F;
    /** Model units are 1/16 of a block, so a vertex is placed to well under a texel. */
    private static final float EPSILON = 1.0E-4F;

    @Test
    void bodySurfaceMeetsAtEveryJoint() {
        for (int shape = 0; shape < 4; shape++) {
            Body body = body(shape);
            List<float[]> quads = body.quads();
            assertFalse(quads.isEmpty(), "Shape " + shape + " draws no body");

            for (int joint = 1; joint < SEGMENTS; joint++) {
                float z = FRONT_Z + SEGMENT_LENGTH * joint;
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
                float top = body.heights()[joint];
                assertTrue(
                        hasHeight(before, top) && hasHeight(before, top + BOTTOM - SLAB_TOP),
                        "Shape "
                                + shape
                                + " joint "
                                + joint
                                + " does not close the body top and bottom: "
                                + before);
            }
        }
    }

    /**
     * The body at a fixed bend. The carpet's own sway is scaled by {@code calm}, which is zero at
     * this speed, so its transform is a translation and the joints lie on planes of constant z.
     */
    private record Body(List<float[]> quads, float[] heights) {}

    private static Body body(int shape) {
        MagicCarpetEntityModel model =
                new MagicCarpetEntityModel(
                        MagicCarpetEntityModel.createBodyLayer(
                                        (shape & 1) != 0, (shape & 2) != 0)
                                .bakeRoot());
        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.speed = 5.0F / 3.0F;
        state.ripplePhase = 0.7F;
        model.setupAnim(state);

        Recorder recorder = new Recorder();
        model.renderBody(new PoseStack().last(), recorder, state.pose, 0, 0, -1);
        return new Body(recorder.quads, state.pose.heights().clone());
    }

    /**
     * The vertices on the joint's plane, from the quads that end there or start there. A quad that
     * spans the plane without a vertex on it would leave the joint unclosed, so it is not counted.
     */
    private static Set<String> ring(List<float[]> quads, float z, boolean before) {
        Set<String> ring = new LinkedHashSet<>();
        for (float[] quad : quads) {
            float min = Float.MAX_VALUE;
            float max = -Float.MAX_VALUE;
            for (int vertex = 0; vertex < 4; vertex++) {
                min = Math.min(min, quad[vertex * 3 + 2]);
                max = Math.max(max, quad[vertex * 3 + 2]);
            }
            if (before ? max > z + EPSILON : min < z - EPSILON) {
                continue;
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                if (Math.abs(quad[vertex * 3 + 2] - z) <= EPSILON) {
                    ring.add(
                            key(
                                    quad[vertex * 3],
                                    quad[vertex * 3 + 1],
                                    quad[vertex * 3 + 2]));
                }
            }
        }
        return ring;
    }

    private static boolean hasHeight(Set<String> ring, float y) {
        String height = "," + Math.round(y * 1.0E4F) + ",";
        return ring.stream().anyMatch(vertex -> vertex.contains(height));
    }

    private static String key(float x, float y, float z) {
        return Math.round(x * 1.0E4F) + "," + Math.round(y * 1.0E4F) + "," + Math.round(z * 1.0E4F);
    }

    /** Keeps the four positions of each quad the body draws. */
    private static final class Recorder implements VertexConsumer {
        private final List<float[]> quads = new ArrayList<>();
        private final float[] quad = new float[12];
        private int vertices;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.quad[this.vertices * 3] = x;
            this.quad[this.vertices * 3 + 1] = y;
            this.quad[this.vertices * 3 + 2] = z;
            this.vertices++;
            if (this.vertices == 4) {
                this.quads.add(this.quad.clone());
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
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
