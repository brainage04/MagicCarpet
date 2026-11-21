package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.client.model.geom.ModelPart;
import org.junit.jupiter.api.Test;

/**
 * The dotted inner border runs round the top face at edge 10, and a motif that reaches it is cut
 * off: the border's own colour replaces the motif's last pixel. The four plus motifs, the two small
 * gems' black outlines and the medallion's outer diamond did exactly that on all three tiers.
 *
 * <p>The check reads the shipped textures, so it fails on a texture whose design still reaches the
 * border, whatever produced it.
 */
class MagicCarpetTopBorderTest {
    private static final int SEGMENTS = 8;

    private static final int SEGMENT_ROWS = 8;

    private static final int DESIGN_W = 48;

    private static final int DESIGN_L = 64;

    /** The ring the dotted border sits on, counted from the design's outer edge. */
    private static final int BORDER_EDGE = 10;

    private static final String[] TIERS = {"basic", "advanced", "legendary"};

    @Test
    void noMotifPixelIsCutByTheInnerBorder() throws IOException {
        for (String tier : TIERS) {
            BufferedImage texture = load(tier);
            List<int[]> segments = topFaces();
            Set<Integer> border = borderColours(texture, segments);
            int solid = solidBorderColour(texture, segments);
            List<String> cut = new ArrayList<>();
            for (int z = 0; z < DESIGN_L; z++) {
                for (int x = 0; x < DESIGN_W; x++) {
                    if (edge(x, z) != BORDER_EDGE + 1
                            || border.contains(pixel(texture, segments, x, z))) {
                        continue;
                    }
                    if (pixel(texture, segments, outwardX(x), outwardZ(z)) == solid) {
                        cut.add("(" + x + ", " + z + ")");
                    }
                }
            }
            assertTrue(
                    cut.isEmpty(),
                    tier
                            + " has "
                            + cut.size()
                            + " motif pixels cut off by the dotted inner border: "
                            + cut);
        }
    }

    private static BufferedImage load(String tier) throws IOException {
        String path = "/assets/magic_carpet/textures/entity/" + tier + "_magic_carpet.png";
        try (InputStream stream = MagicCarpetTopBorderTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path + " is not on the classpath");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " is not a readable PNG");
            return image;
        }
    }

    /**
     * Each segment's top face in texture pixels, as {@code {u, v, width, height}}, read from the
     * model's own UVs so a change to the layout moves the test with it.
     */
    private static List<int[]> topFaces() {
        ModelPart root = MagicCarpetEntityModel.createBodyLayer(false, false).bakeRoot();
        ModelPart carpet = root.getChild("carpet");
        List<int[]> faces = new ArrayList<>();
        for (int segment = 0; segment < SEGMENTS; segment++) {
            ModelPart part = carpet.getChild("segment_" + segment);
            float[] rect = upFace(part);
            faces.add(
                    new int[] {
                        Math.round(rect[0] * MagicCarpetEntityModel.TEXTURE_WIDTH),
                        Math.round(rect[1] * MagicCarpetEntityModel.TEXTURE_HEIGHT),
                        Math.round((rect[2] - rect[0]) * MagicCarpetEntityModel.TEXTURE_WIDTH),
                        Math.round((rect[3] - rect[1]) * MagicCarpetEntityModel.TEXTURE_HEIGHT)
                    });
        }
        return faces;
    }

    /** The UV rectangle of a part's upward face, in texture units from 0 to 1. */
    private static float[] upFace(ModelPart part) {
        float[] rect = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        part.visit(
                new PoseStack(),
                (pose, path, index, cube) -> {
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        if (polygon.normal().y() < 0.5F) {
                            continue;
                        }
                        for (ModelPart.Vertex vertex : polygon.vertices()) {
                            rect[0] = Math.min(rect[0], vertex.u());
                            rect[1] = Math.min(rect[1], vertex.v());
                            rect[2] = Math.max(rect[2], vertex.u());
                            rect[3] = Math.max(rect[3], vertex.v());
                        }
                    }
                });
        assertTrue(rect[0] <= rect[2], "the part has no upward face");
        return rect;
    }

    /** The colour of the design's art pixel (x, z), read from the segment that carries it. */
    private static int pixel(BufferedImage texture, List<int[]> segments, int x, int z) {
        int[] face = segments.get(z / SEGMENT_ROWS);
        int row = SEGMENT_ROWS - 1 - z % SEGMENT_ROWS;
        return texture.getRGB(face[0] + x, face[1] + row) & 0xFFFFFF;
    }

    private static int edge(int x, int z) {
        return Math.min(Math.min(x, DESIGN_W - 1 - x), Math.min(z, DESIGN_L - 1 - z));
    }

    /** The same pixel one step further from the design's centre, along the axis it is nearest. */
    private static int outwardX(int x) {
        if (x == BORDER_EDGE + 1) {
            return BORDER_EDGE;
        }
        return x == DESIGN_W - 2 - BORDER_EDGE ? DESIGN_W - 1 - BORDER_EDGE : x;
    }

    private static int outwardZ(int z) {
        if (z == BORDER_EDGE + 1) {
            return BORDER_EDGE;
        }
        return z == DESIGN_L - 2 - BORDER_EDGE ? DESIGN_L - 1 - BORDER_EDGE : z;
    }

    /**
     * Every colour the dotted border draws: the solid ring colour and the field showing between its
     * dots.
     */
    private static Set<Integer> borderColours(BufferedImage texture, List<int[]> segments) {
        Set<Integer> colours = new HashSet<>();
        for (int z = 0; z < DESIGN_L; z++) {
            for (int x = 0; x < DESIGN_W; x++) {
                if (edge(x, z) == BORDER_EDGE) {
                    colours.add(pixel(texture, segments, x, z));
                }
            }
        }
        return colours;
    }

    /**
     * The colour the border draws on most of its ring, which is the one a cut motif pixel meets.
     */
    private static int solidBorderColour(BufferedImage texture, List<int[]> segments) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int z = 0; z < DESIGN_L; z++) {
            for (int x = 0; x < DESIGN_W; x++) {
                if (edge(x, z) == BORDER_EDGE) {
                    counts.merge(pixel(texture, segments, x, z), 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }
}
