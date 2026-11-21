package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Two faces of one part that lie in the same plane and cover the same pixels are drawn on top of
 * each other. The carpet's render type does not cull back faces (RenderPipelines.java:507 in 26.3),
 * so both are rasterised and the depth test picks between them per pixel: the carpet flickers.
 * Faces of different parts are left alone, because the ripple bends the segments apart and their
 * touching end faces then show.
 *
 * <p>The body is one surface drawn as custom geometry, so its quads are checked as one part.
 */
class MagicCarpetModelOverlapTest {
    /** Model units are 1/16 of a block; a face is 1 unit thick at the least. */
    private static final float EPSILON = 1.0E-4F;

    private record Face(String part, String name, Vector3f[] points, Vector3f normal) {}

    @Test
    void noPartDrawsTwoFacesInOnePlane() {
        for (int shape = 0; shape < 4; shape++) {
            List<Face> faces = faces(shape);
            List<String> overlaps = new ArrayList<>();
            for (int a = 0; a < faces.size(); a++) {
                for (int b = a + 1; b < faces.size(); b++) {
                    if (faces.get(a).part().equals(faces.get(b).part())
                            && overlaps(faces.get(a), faces.get(b))) {
                        overlaps.add(faces.get(a).name() + " and " + faces.get(b).name());
                    }
                }
            }
            assertTrue(
                    overlaps.isEmpty(),
                    "Shape "
                            + shape
                            + " draws "
                            + overlaps.size()
                            + " coplanar face pairs: "
                            + overlaps);
        }
    }

    private static List<Face> faces(int shape) {
        LayerDefinition layer =
                MagicCarpetEntityModel.createBodyLayer((shape & 1) != 0, (shape & 2) != 0);
        ModelPart root = layer.bakeRoot();
        List<Face> faces = new ArrayList<>();
        root.visit(
                new PoseStack(),
                (pose, path, index, cube) -> {
                    Matrix4f matrix = pose.pose();
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        Vector3f[] points = new Vector3f[4];
                        for (int i = 0; i < points.length; i++) {
                            ModelPart.Vertex vertex = polygon.vertices()[i];
                            points[i] =
                                    matrix.transformPosition(
                                            vertex.x(), vertex.y(), vertex.z(), new Vector3f());
                        }
                        Vector3f normal = pose.transformNormal(polygon.normal(), new Vector3f());
                        faces.add(new Face(path, path + "#" + index, points, normal.normalize()));
                    }
                });
        faces.addAll(bodyFaces(shape));
        return faces;
    }

    /** The body's quads at a fixed bend, each with the normal the mesh gives it. */
    private static List<Face> bodyFaces(int shape) {
        MagicCarpetEntityModel model =
                new MagicCarpetEntityModel(
                        MagicCarpetEntityModel.createBodyLayer((shape & 1) != 0, (shape & 2) != 0)
                                .bakeRoot());
        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.speed = 5.0F / 3.0F;
        state.ripplePhase = 0.7F;
        model.setupAnim(state);

        Recorder recorder = new Recorder();
        model.renderBody(new PoseStack().last(), recorder, state.pose, 0, 0, -1);
        return recorder.faces;
    }

    private static boolean overlaps(Face first, Face second) {
        if (Math.abs(first.normal().dot(second.normal())) < 1.0F - EPSILON) {
            return false;
        }
        for (Vector3f point : second.points()) {
            if (Math.abs(new Vector3f(point).sub(first.points()[0]).dot(first.normal()))
                    > EPSILON) {
                return false;
            }
        }
        return projectedArea(first, second) > EPSILON;
    }

    /**
     * The area the two faces share, measured in the plane they lie in: the first face clipped by
     * each edge of the second. Two faces that only touch along an edge share no area.
     */
    private static float projectedArea(Face first, Face second) {
        Vector3f along = first.points()[1].sub(first.points()[0], new Vector3f()).normalize();
        Vector3f across = first.normal().cross(along, new Vector3f()).normalize();
        List<Vector3f> clipped = new ArrayList<>(List.of(first.points()));
        for (int edge = 0; edge < second.points().length; edge++) {
            Vector3f from = second.points()[edge];
            Vector3f to = second.points()[(edge + 1) % second.points().length];
            Vector3f inward = second.normal().cross(to.sub(from, new Vector3f()), new Vector3f());
            clipped = clip(clipped, from, inward);
            if (clipped.isEmpty()) {
                return 0.0F;
            }
        }
        float area = 0.0F;
        for (int i = 0; i < clipped.size(); i++) {
            Vector3f a = clipped.get(i);
            Vector3f b = clipped.get((i + 1) % clipped.size());
            area += a.dot(along) * b.dot(across) - b.dot(along) * a.dot(across);
        }
        return Math.abs(area) / 2.0F;
    }

    /** Keeps the part of {@code polygon} on the inner side of the plane through {@code point}. */
    private static List<Vector3f> clip(List<Vector3f> polygon, Vector3f point, Vector3f inward) {
        List<Vector3f> kept = new ArrayList<>();
        for (int i = 0; i < polygon.size(); i++) {
            Vector3f from = polygon.get(i);
            Vector3f to = polygon.get((i + 1) % polygon.size());
            float fromSide = new Vector3f(from).sub(point).dot(inward);
            float toSide = new Vector3f(to).sub(point).dot(inward);
            if (fromSide >= -EPSILON) {
                kept.add(from);
            }
            if ((fromSide > EPSILON && toSide < -EPSILON)
                    || (fromSide < -EPSILON && toSide > EPSILON)) {
                float along = fromSide / (fromSide - toSide);
                kept.add(new Vector3f(from).lerp(to, along));
            }
        }
        return kept;
    }

    /** Keeps each quad the body draws, with its normal. */
    private static final class Recorder implements VertexConsumer {
        private final List<Face> faces = new ArrayList<>();
        private final Vector3f[] points = new Vector3f[4];
        private final Vector3f normal = new Vector3f();
        private int vertices;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.points[this.vertices] = new Vector3f(x, y, z);
            this.vertices++;
            if (this.vertices == 4) {
                this.faces.add(
                        new Face(
                                "body",
                                "body#" + this.faces.size(),
                                this.points.clone(),
                                new Vector3f(this.normal).normalize()));
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
            this.normal.set(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
