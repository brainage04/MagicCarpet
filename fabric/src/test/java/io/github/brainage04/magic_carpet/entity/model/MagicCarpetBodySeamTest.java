package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.joml.Vector3f;
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
    /** Model units are 1/16 of a block, so a vertex is placed to well under a texel. */
    private static final float EPSILON = 1.0E-4F;

    @Test
    void bodySurfaceMeetsAtEveryJoint() {
        for (int shape = 0; shape < 4; shape++) {
            Map<Integer, List<float[]>> quads = bodyQuads(shape);
            assertFalse(quads.isEmpty(), "Shape " + shape + " draws no body");

            for (int joint = 1; joint < SEGMENTS; joint++) {
                Set<String> before = vertices(quads.get(joint - 1));
                Set<String> after = vertices(quads.get(joint));
                before.retainAll(after);
                assertTrue(
                        before.size() >= 4,
                        "Shape "
                                + shape
                                + " joint "
                                + joint
                                + " shares only "
                                + before.size()
                                + " vertices between the segments either side of it: "
                                + before);
            }
        }
    }

    /**
     * The body at a fixed bend. The carpet's own sway is scaled by {@code calm}, which is zero at
     * this speed, so its transform is a translation and the joints lie on planes of constant z.
     */
    private static MagicCarpetEntityRenderState bend() {
        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.speed = 5.0F / 3.0F;
        state.ripplePhase = 0.7F;
        return state;
    }

    /** The body's quads, four positions each, grouped by the segment that draws them. */
    private static Map<Integer, List<float[]>> bodyQuads(int shape) {
        LayerDefinition layer =
                MagicCarpetEntityModel.createBodyLayer((shape & 1) != 0, (shape & 2) != 0);
        ModelPart root = layer.bakeRoot();
        new MagicCarpetEntityModel(root).setupAnim(bend());

        Map<Integer, List<float[]>> quads = new TreeMap<>();
        root.visit(
                new PoseStack(),
                (pose, path, index, cube) -> {
                    int segment = segment(path);
                    if (segment < 0) {
                        return;
                    }
                    List<float[]> segmentQuads =
                            quads.computeIfAbsent(segment, key -> new ArrayList<>());
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        float[] positions = new float[12];
                        for (int vertex = 0; vertex < 4; vertex++) {
                            ModelPart.Vertex corner = polygon.vertices()[vertex];
                            Vector3f position =
                                    pose.pose()
                                            .transformPosition(
                                                    corner.worldX(),
                                                    corner.worldY(),
                                                    corner.worldZ(),
                                                    new Vector3f());
                            positions[vertex * 3] = position.x();
                            positions[vertex * 3 + 1] = position.y();
                            positions[vertex * 3 + 2] = position.z();
                        }
                        segmentQuads.add(positions);
                    }
                });
        return quads;
    }

    private static int segment(String path) {
        int at = path.lastIndexOf("segment_");
        return at < 0 ? -1 : Integer.parseInt(path.substring(at + "segment_".length()));
    }

    private static Set<String> vertices(List<float[]> quads) {
        Set<String> vertices = new LinkedHashSet<>();
        for (float[] quad : quads) {
            for (int vertex = 0; vertex < 4; vertex++) {
                vertices.add(
                        key(quad[vertex * 3], quad[vertex * 3 + 1], quad[vertex * 3 + 2]));
            }
        }
        return vertices;
    }

    private static String key(float x, float y, float z) {
        return Math.round(x * 1.0E4F) + "," + Math.round(y * 1.0E4F) + "," + Math.round(z * 1.0E4F);
    }
}
