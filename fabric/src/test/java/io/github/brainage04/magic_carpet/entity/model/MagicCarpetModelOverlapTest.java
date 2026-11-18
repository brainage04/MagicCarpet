package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
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
                    "Shape " + shape + " draws " + overlaps.size() + " coplanar face pairs: "
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
        return faces;
    }

    private static boolean overlaps(Face first, Face second) {
        if (Math.abs(first.normal().dot(second.normal())) < 1.0F - EPSILON) {
            return false;
        }
        for (Vector3f point : second.points()) {
            if (Math.abs(point.sub(first.points()[0], new Vector3f()).dot(first.normal()))
                    > EPSILON) {
                return false;
            }
        }
        return projectedArea(first, second) > EPSILON;
    }

    /** The area the two faces share, measured in the plane they lie in. */
    private static float projectedArea(Face first, Face second) {
        Vector3f along = first.points()[1].sub(first.points()[0], new Vector3f()).normalize();
        Vector3f across = first.normal().cross(along, new Vector3f()).normalize();
        float[] firstRect = bounds(first, along, across);
        float[] secondRect = bounds(second, along, across);
        float width =
                Math.min(firstRect[2], secondRect[2]) - Math.max(firstRect[0], secondRect[0]);
        float height =
                Math.min(firstRect[3], secondRect[3]) - Math.max(firstRect[1], secondRect[1]);
        return width > EPSILON && height > EPSILON ? width * height : 0.0F;
    }

    private static float[] bounds(Face face, Vector3f along, Vector3f across) {
        float[] rect = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (Vector3f point : face.points()) {
            float u = point.dot(along);
            float v = point.dot(across);
            rect[0] = Math.min(rect[0], u);
            rect[1] = Math.min(rect[1], v);
            rect[2] = Math.max(rect[2], u);
            rect[3] = Math.max(rect[3], v);
        }
        return rect;
    }
}
