package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.Test;

/**
 * The dotted inner border runs round the top face at edge 10, and a motif that reaches it is cut
 * off: the border's own colour replaces the motif's last pixel. The four plus motifs and the two
 * small gems' black outlines did exactly that on all three tiers.
 *
 * <p>The check reads the shipped textures, so it fails on a texture that was not regenerated from
 * {@code tools/generate_carpet_assets.py} as well as on one whose design still reaches the border.
 */
class MagicCarpetTopBorderTest {
    /** The top face of segment {@code i} starts at this texture column and row. */
    private static final int TOP_U = 56;

    private static final int SEGMENT_V = 10;

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
            Set<Integer> border = borderColours(texture);
            int solid = solidBorderColour(texture);
            List<String> cut = new ArrayList<>();
            for (int z = 0; z < DESIGN_L; z++) {
                for (int x = 0; x < DESIGN_W; x++) {
                    if (edge(x, z) != BORDER_EDGE + 1 || border.contains(pixel(texture, x, z))) {
                        continue;
                    }
                    if (pixel(texture, outwardX(x), outwardZ(z)) == solid) {
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

    /** The colour of the design's art pixel (x, z), read from the segment that carries it. */
    private static int pixel(BufferedImage texture, int x, int z) {
        int segment = z / SEGMENT_ROWS;
        int row = SEGMENT_ROWS - 1 - z % SEGMENT_ROWS;
        return texture.getRGB(TOP_U + x, SEGMENT_V * segment + row) & 0xFFFFFF;
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
    private static Set<Integer> borderColours(BufferedImage texture) {
        Set<Integer> colours = new HashSet<>();
        for (int z = 0; z < DESIGN_L; z++) {
            for (int x = 0; x < DESIGN_W; x++) {
                if (edge(x, z) == BORDER_EDGE) {
                    colours.add(pixel(texture, x, z));
                }
            }
        }
        return colours;
    }

    /**
     * The colour the border draws on most of its ring, which is the one a cut motif pixel meets.
     */
    private static int solidBorderColour(BufferedImage texture) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int z = 0; z < DESIGN_L; z++) {
            for (int x = 0; x < DESIGN_W; x++) {
                if (edge(x, z) == BORDER_EDGE) {
                    counts.merge(pixel(texture, x, z), 1, Integer::sum);
                }
            }
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
    }
}
