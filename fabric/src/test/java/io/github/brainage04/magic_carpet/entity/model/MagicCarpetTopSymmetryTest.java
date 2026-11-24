package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Everything inside the dotted inner border (the border itself, the medallion and the end motifs)
 * is the same when the top face is mirrored left to right or front to back. The pattern's own
 * checkerboard, dots and weave once ran from one end to the other, so each of them read differently
 * on the two sides of a mirror line.
 *
 * <p>The check reads every shipped texture that paints the design: the three entity textures, the
 * three item textures and the legendary glow layer. Only segments 1 to 6 reach the field (it starts
 * 10 art pixels in from the front and back), and those are the plain shape in every tier.
 */
class MagicCarpetTopSymmetryTest {
    private static final int SEGMENT_ROWS = 8;

    private static final int DESIGN_W = 48;

    private static final int DESIGN_L = 64;

    /** The dotted inner border's ring; the field is this ring and everything inside it. */
    private static final int FIELD_EDGE = 10;

    private static final String[] TEXTURES = {
        "entity/basic_magic_carpet",
        "entity/advanced_magic_carpet",
        "entity/legendary_magic_carpet",
        "entity/legendary_magic_carpet_glow",
        "item/basic_magic_carpet",
        "item/advanced_magic_carpet",
        "item/legendary_magic_carpet"
    };

    @Test
    void fieldIsSymmetricInBothAxes() throws IOException {
        for (String name : TEXTURES) {
            BufferedImage texture = load(name);
            List<int[]> segments = topFaces(texture);
            List<String> lopsided = new ArrayList<>();
            for (int z = FIELD_EDGE; z < DESIGN_L - FIELD_EDGE; z++) {
                for (int x = FIELD_EDGE; x < DESIGN_W - FIELD_EDGE; x++) {
                    int here = pixel(texture, segments, x, z);
                    if (here != pixel(texture, segments, DESIGN_W - 1 - x, z)) {
                        lopsided.add("(" + x + ", " + z + ") left-right");
                    }
                    if (here != pixel(texture, segments, x, DESIGN_L - 1 - z)) {
                        lopsided.add("(" + x + ", " + z + ") front-back");
                    }
                }
            }
            assertTrue(
                    lopsided.isEmpty(),
                    name
                            + " has "
                            + lopsided.size()
                            + " field pixels that differ from their mirror image: "
                            + lopsided.subList(0, Math.min(12, lopsided.size())));
        }
    }

    private static BufferedImage load(String name) throws IOException {
        String path = "/assets/magic_carpet/textures/" + name + ".png";
        try (InputStream stream = MagicCarpetTopSymmetryTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, path + " is not on the classpath");
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image, path + " is not a readable PNG");
            return image;
        }
    }

    /**
     * Each segment's top face in image pixels, as {@code {u, v, width, height}}, read from the
     * mesh's own UVs so a change to the layout moves the test with it.
     */
    private static List<int[]> topFaces(BufferedImage texture) {
        List<int[]> faces = new ArrayList<>();
        for (float[] rect : MagicCarpetBodyMesh.topFaceRects()) {
            faces.add(
                    new int[] {
                        Math.round(rect[0] * texture.getWidth()),
                        Math.round(rect[1] * texture.getHeight()),
                        Math.round(rect[2] * texture.getWidth()),
                        Math.round(rect[3] * texture.getHeight())
                    });
        }
        return faces;
    }

    /** The ARGB of the design's art pixel (x, z), read from the segment that carries it. */
    private static int pixel(BufferedImage texture, List<int[]> segments, int x, int z) {
        int[] face = segments.get(z / SEGMENT_ROWS);
        int row = SEGMENT_ROWS - 1 - z % SEGMENT_ROWS;
        return texture.getRGB(face[0] + x, face[1] + row);
    }
}
