package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The carpet has no hole you can see through. The body, the tassels and the curled corners are
 * drawn together as one set of quads. A hole is an edge that exactly one quad owns and that does
 * not run across the face of another quad: nothing is drawn on the other side of it, so the
 * background shows through the gap. The render type does not cull back faces
 * (RenderPipelines.java:507 in 26.3), so a face seen from behind is no hole.
 *
 * <p>A transparent texel is a second kind of hole, because the render type discards it. Every texel
 * a quad's UVs cover must be opaque in the tier's texture.
 *
 * <p>Each tier is checked at rest, at full speed and at a sharp bend.
 */
class MagicCarpetClosedSurfaceTest {
    /** Model units, 1/16 block. Float noise from the poses' rotations stays far below this. */
    private static final float TOLERANCE = 2.0E-3F;

    /** The tiers: plain, with tassels, with tassels and curled corners. */
    private static final boolean[][] SHAPES = {{false, false}, {true, false}, {true, true}};

    private static final String[] TIERS = {"basic", "advanced", "legendary"};

    /** Keeps a quad's UV edge from reaching into the texel next to the ones it covers. */
    private static final float UV_INSET = 1.0E-3F;

    private static final float[][] POSES = {
        {0.0F, 0.0F, 0.0F}, {80.0F, 1.0F, 0.0F}, {17.0F, 5.0F / 3.0F, 0.7F}
    };

    @Test
    void noEdgeOpensOntoTheBackground() {
        for (boolean[] shape : SHAPES) {
            for (float[] pose : POSES) {
                String name =
                        "tassels="
                                + shape[0]
                                + " curls="
                                + shape[1]
                                + " age="
                                + pose[0]
                                + " speed="
                                + pose[1]
                                + " phase="
                                + pose[2];
                List<Quad> quads = quads(shape[0], shape[1], pose);
                assertFalse(quads.isEmpty(), name + " draws nothing");
                List<String> holes = openEdges(quads);
                assertTrue(holes.isEmpty(), name + " has open edges: " + holes);
            }
        }
    }

    @Test
    void everyTexelAQuadCoversIsOpaque() throws IOException {
        for (int tier = 0; tier < TIERS.length; tier++) {
            BufferedImage texture = load(TIERS[tier]);
            List<String> clear = new ArrayList<>();
            for (float[] pose : POSES) {
                for (Quad quad : quads(SHAPES[tier][0], SHAPES[tier][1], pose)) {
                    int u0 = (int) Math.floor((quad.minU() + UV_INSET) * texture.getWidth());
                    int u1 = (int) Math.floor((quad.maxU() - UV_INSET) * texture.getWidth());
                    int v0 = (int) Math.floor((quad.minV() + UV_INSET) * texture.getHeight());
                    int v1 = (int) Math.floor((quad.maxV() - UV_INSET) * texture.getHeight());
                    for (int v = v0; v <= v1; v++) {
                        for (int u = u0; u <= u1; u++) {
                            if ((texture.getRGB(u, v) >>> 24) != 0xFF) {
                                clear.add("(" + u + ", " + v + ") at " + fmt(quad.positions));
                            }
                        }
                    }
                }
            }
            assertTrue(
                    clear.isEmpty(),
                    TIERS[tier]
                            + " draws "
                            + clear.size()
                            + " quads' texels that are not opaque: "
                            + clear.subList(0, Math.min(8, clear.size())));
        }
    }

    private static BufferedImage load(String tier) throws IOException {
        String path = "/assets/magic_carpet/textures/entity/" + tier + "_magic_carpet.png";
        try (InputStream stream = MagicCarpetClosedSurfaceTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path + " is not on the classpath");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " is not a readable PNG");
            return image;
        }
    }

    /** Every quad the carpet draws, positions in model units and UVs from 0 to 1. */
    private static List<Quad> quads(boolean tassels, boolean curls, float[] pose) {
        ModelPart root = MagicCarpetEntityModel.createBodyLayer(tassels, curls).bakeRoot();
        MagicCarpetEntityModel model = new MagicCarpetEntityModel(root);
        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.ageInTicks = pose[0];
        state.speed = pose[1];
        state.ripplePhase = pose[2];
        model.setupAnim(state);

        List<Quad> quads = new ArrayList<>();
        PoseStack body = new PoseStack();
        MagicCarpetEntityModel.transformBody(body, state.pose);
        Recorder recorder = new Recorder(quads);
        model.renderBody(body.last(), recorder, state.pose, 0, 0, -1);

        root.visit(
                new PoseStack(),
                (poseStack, path, index, cube) -> {
                    Matrix4f matrix = poseStack.pose();
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        float[] quad = new float[12];
                        float[] uv = new float[8];
                        for (int i = 0; i < 4; i++) {
                            ModelPart.Vertex vertex = polygon.vertices()[i];
                            uv[i * 2] = vertex.u();
                            uv[i * 2 + 1] = vertex.v();
                            Vector3f point =
                                    matrix.transformPosition(
                                            vertex.worldX(),
                                            vertex.worldY(),
                                            vertex.worldZ(),
                                            new Vector3f());
                            quad[i * 3] = point.x * 16.0F;
                            quad[i * 3 + 1] = point.y * 16.0F;
                            quad[i * 3 + 2] = point.z * 16.0F;
                        }
                        quads.add(new Quad(quad, uv));
                    }
                });
        return quads;
    }

    /**
     * The edges of the quads that no other quad shares, after each edge is split where another
     * quad's corner lies on it, and that do not lie on another quad's face.
     */
    private static List<String> openEdges(List<Quad> quads) {
        List<float[]> corners = new ArrayList<>();
        Map<String, Integer> ids = new HashMap<>();
        int[][] quadIds = new int[quads.size()][4];
        for (int q = 0; q < quads.size(); q++) {
            for (int i = 0; i < 4; i++) {
                float[] quad = quads.get(q).positions;
                float[] point = {quad[i * 3], quad[i * 3 + 1], quad[i * 3 + 2]};
                String key =
                        Math.round(point[0] / TOLERANCE)
                                + ","
                                + Math.round(point[1] / TOLERANCE)
                                + ","
                                + Math.round(point[2] / TOLERANCE);
                Integer id = ids.get(key);
                if (id == null) {
                    id = corners.size();
                    ids.put(key, id);
                    corners.add(point);
                }
                quadIds[q][i] = id;
            }
        }

        Map<Long, List<Integer>> owners = new HashMap<>();
        for (int q = 0; q < quads.size(); q++) {
            for (int i = 0; i < 4; i++) {
                int from = quadIds[q][i];
                int to = quadIds[q][(i + 1) % 4];
                if (from == to) {
                    continue;
                }
                List<Integer> chain = split(corners, from, to);
                for (int step = 0; step + 1 < chain.size(); step++) {
                    long key = edgeKey(chain.get(step), chain.get(step + 1));
                    owners.computeIfAbsent(key, k -> new ArrayList<>()).add(q);
                }
            }
        }

        List<String> open = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : owners.entrySet()) {
            if (entry.getValue().size() != 1) {
                continue;
            }
            int a = (int) (entry.getKey() >> 32);
            int b = (int) (long) entry.getKey();
            float[] from = corners.get(a);
            float[] to = corners.get(b);
            float[] middle = {(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2};
            int owner = entry.getValue().get(0);
            boolean covered = false;
            for (int q = 0; q < quads.size() && !covered; q++) {
                covered = q != owner && onFace(quads.get(q).positions, middle);
            }
            if (!covered) {
                open.add(fmt(from) + " to " + fmt(to) + " of quad " + owner);
            }
        }
        open.sort(null);
        return open;
    }

    /** The corners on the way from {@code from} to {@code to}, ends included, in order. */
    private static List<Integer> split(List<float[]> corners, int from, int to) {
        float[] a = corners.get(from);
        float[] b = corners.get(to);
        float[] edge = {b[0] - a[0], b[1] - a[1], b[2] - a[2]};
        float length2 = edge[0] * edge[0] + edge[1] * edge[1] + edge[2] * edge[2];
        List<float[]> found = new ArrayList<>();
        for (int id = 0; id < corners.size(); id++) {
            if (id == from || id == to) {
                continue;
            }
            float[] c = corners.get(id);
            float[] toC = {c[0] - a[0], c[1] - a[1], c[2] - a[2]};
            float t = (toC[0] * edge[0] + toC[1] * edge[1] + toC[2] * edge[2]) / length2;
            if (t <= 0.0F || t >= 1.0F) {
                continue;
            }
            float dx = toC[0] - t * edge[0];
            float dy = toC[1] - t * edge[1];
            float dz = toC[2] - t * edge[2];
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) <= TOLERANCE) {
                found.add(new float[] {t, id});
            }
        }
        found.sort((x, y) -> Float.compare(x[0], y[0]));
        List<Integer> chain = new ArrayList<>();
        chain.add(from);
        for (float[] hit : found) {
            chain.add((int) hit[1]);
        }
        chain.add(to);
        return chain;
    }

    private static long edgeKey(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    /** True when {@code point} is on the quad, inside its outline and in its plane. */
    private static boolean onFace(float[] quad, float[] point) {
        Vector3f p0 = new Vector3f(quad[0], quad[1], quad[2]);
        Vector3f p1 = new Vector3f(quad[3], quad[4], quad[5]);
        Vector3f p2 = new Vector3f(quad[6], quad[7], quad[8]);
        Vector3f p3 = new Vector3f(quad[9], quad[10], quad[11]);
        Vector3f normal = p1.sub(p0, new Vector3f()).cross(p3.sub(p0, new Vector3f()));
        if (normal.lengthSquared() < 1.0E-12F) {
            return false;
        }
        normal.normalize();
        Vector3f at = new Vector3f(point).sub(p0);
        if (Math.abs(at.dot(normal)) > TOLERANCE) {
            return false;
        }
        Vector3f[] ring = {p0, p1, p2, p3};
        for (int i = 0; i < 4; i++) {
            Vector3f from = ring[i];
            Vector3f to = ring[(i + 1) % 4];
            Vector3f inward = normal.cross(to.sub(from, new Vector3f()), new Vector3f());
            Vector3f side = new Vector3f(point).sub(from);
            // The ring runs the same way round for each of the quad's edges, so the point's
            // distance to every edge line has one sign, up to the tolerance.
            float winding = inward.dot(side);
            Vector3f reference = p0.add(p1, new Vector3f()).add(p2).add(p3).mul(0.25F);
            float inside = inward.dot(new Vector3f(reference).sub(from));
            if (winding * Math.signum(inside) < -TOLERANCE * inward.length()) {
                return false;
            }
        }
        return true;
    }

    private static String fmt(float[] point) {
        return String.format("(%.3f, %.3f, %.3f)", point[0], point[1], point[2]);
    }

    /** One quad: four positions in model units and four UV pairs. */
    private record Quad(float[] positions, float[] uvs) {
        private float minU() {
            return Math.min(Math.min(this.uvs[0], this.uvs[2]), Math.min(this.uvs[4], this.uvs[6]));
        }

        private float maxU() {
            return Math.max(Math.max(this.uvs[0], this.uvs[2]), Math.max(this.uvs[4], this.uvs[6]));
        }

        private float minV() {
            return Math.min(Math.min(this.uvs[1], this.uvs[3]), Math.min(this.uvs[5], this.uvs[7]));
        }

        private float maxV() {
            return Math.max(Math.max(this.uvs[1], this.uvs[3]), Math.max(this.uvs[5], this.uvs[7]));
        }
    }

    /**
     * Keeps the corners and UVs of each quad the body draws, in model units. A vertex is complete
     * once its normal arrives, which is the last thing the mesh sets.
     */
    private static final class Recorder implements VertexConsumer {
        private final List<Quad> quads;
        private final float[] corners = new float[12];
        private final float[] uvs = new float[8];
        private int vertices;

        private Recorder(List<Quad> quads) {
            this.quads = quads;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.corners[this.vertices * 3] = x * 16.0F;
            this.corners[this.vertices * 3 + 1] = y * 16.0F;
            this.corners[this.vertices * 3 + 2] = z * 16.0F;
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
            this.uvs[this.vertices * 2] = u;
            this.uvs[this.vertices * 2 + 1] = v;
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
            this.vertices++;
            if (this.vertices == 4) {
                this.quads.add(new Quad(this.corners.clone(), this.uvs.clone()));
                this.vertices = 0;
            }
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
