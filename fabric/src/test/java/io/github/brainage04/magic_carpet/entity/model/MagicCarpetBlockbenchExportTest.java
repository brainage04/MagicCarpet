package io.github.brainage04.magic_carpet.entity.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.brainage04.magic_carpet.entity.custom.CarpetAnimation;
import io.github.brainage04.magic_carpet.entity.renderer.state.MagicCarpetEntityRenderState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * The .bbmodel files in {@code tools/blockbench} are a second copy of the model and its animation,
 * written by {@code tools/blockbench_export.py}. This test bakes the Java model, runs {@code
 * setupAnim} and compares every cube corner of every part against the corners the .bbmodel
 * describes, so the copy cannot drift from the code it was made from.
 *
 * <p>It also measures the crack at each joint between two segments: each segment is a rigid box
 * pitched about its own pivot, so the two end faces at a joint separate at both edges. The
 * measurement is in model pixels (1/16 block) and is written per joint per sampled tick to {@code
 * build/blockbench-seams.txt}, with the angle between the two faces.
 */
class MagicCarpetBlockbenchExportTest {
    /** A corner may differ by this much, in model pixels, and still be the same corner. */
    private static final float TOLERANCE = 1.0E-3F;

    private static final int SEGMENTS = 8;
    private static final int CYCLE_TICKS = 80;
    private static final int SAMPLE_TICKS = 2;
    private static final float TICKS_PER_SECOND = 20.0F;

    /** The tiers' model flags and the speed each animation is sampled at. */
    private record Carpet(String name, boolean tassels, boolean curls, float speed) {}

    private static final List<Carpet> CARPETS =
            List.of(
                    new Carpet("basic", false, false, 0.0F),
                    new Carpet("basic", false, false, 1.0F),
                    new Carpet("advanced", true, false, 0.0F),
                    new Carpet("advanced", true, false, 1.0F),
                    new Carpet("legendary", true, true, 0.0F),
                    new Carpet("legendary", true, true, 1.0F));

    @Test
    void everyCubeCornerMatchesTheExport() throws IOException {
        Path directory = exportDirectory();
        float worst = 0.0F;
        String worstWhere = "";

        for (Carpet carpet : CARPETS) {
            JsonObject project = read(directory.resolve(carpet.name() + ".bbmodel"));
            Map<String, List<Vector3f[]>> exported = exportedCorners(project);
            String animation = carpet.speed() == 0.0F ? "idle" : "flying";

            for (int tick = 0; tick <= CYCLE_TICKS; tick += SAMPLE_TICKS) {
                Map<String, List<Vector3f[]>> baked = bakedCorners(carpet, tick);
                Map<String, List<Vector3f[]>> posed =
                        animatedCorners(project, exported, animation, tick / TICKS_PER_SECOND);

                assertEquals(
                        new java.util.TreeSet<>(baked.keySet()),
                        new java.util.TreeSet<>(posed.keySet()),
                        carpet.name() + " " + animation + " tick " + tick + ": part names");
                for (String part : baked.keySet()) {
                    List<Vector3f[]> expected = baked.get(part);
                    List<Vector3f[]> actual = posed.get(part);
                    assertEquals(
                            expected.size(),
                            actual.size(),
                            carpet.name() + " " + animation + " tick " + tick + ": " + part);
                    for (int cube = 0; cube < expected.size(); cube++) {
                        Vector3f[] expectedCorners = sorted(expected.get(cube));
                        Vector3f[] actualCorners = sorted(actual.get(cube));
                        for (int corner = 0; corner < 8; corner++) {
                            float error = expectedCorners[corner].distance(actualCorners[corner]);
                            if (error > worst) {
                                worst = error;
                                worstWhere =
                                        carpet.name()
                                                + " "
                                                + animation
                                                + " tick "
                                                + tick
                                                + " "
                                                + part
                                                + " cube "
                                                + cube
                                                + " corner "
                                                + corner;
                            }
                        }
                    }
                }
            }
        }

        assertTrue(
                worst <= TOLERANCE,
                "The .bbmodel corners differ from the baked model by "
                        + worst
                        + " pixels at "
                        + worstWhere
                        + "; rerun tools/blockbench_export.py");
    }

    /**
     * The crack at each joint, in model pixels, measured on the baked model and on the export.
     *
     * <p>Each segment is pitched about its own front-bottom pivot, so the two end faces at a joint
     * separate at both edges: the top edge and the bottom edge each move by a different amount.
     * Both are measured, and the export must show the same numbers, or Blockbench would hide a gap
     * the game draws.
     */
    @Test
    void seamGapsMatchTheExport() throws IOException {
        Path directory = exportDirectory();
        StringBuilder report = new StringBuilder();
        float worstDifference = 0.0F;

        for (Carpet carpet : CARPETS) {
            JsonObject project = read(directory.resolve(carpet.name() + ".bbmodel"));
            Map<String, List<Vector3f[]>> exported = exportedCorners(project);
            String animation = carpet.speed() == 0.0F ? "idle" : "flying";
            report.append(carpet.name()).append(' ').append(animation).append('\n');
            report.append("  columns: joint 0..6, top edge then bottom edge, in model pixels;");
            report.append(" then the crease angle in degrees\n");
            float widestTop = 0.0F;
            float widestBottom = 0.0F;

            for (int tick = 0; tick <= CYCLE_TICKS; tick += SAMPLE_TICKS) {
                Map<String, List<Vector3f[]>> corners = bakedCorners(carpet, tick);
                float[][] baked = seamGaps(corners);
                float[][] shown =
                        seamGaps(
                                animatedCorners(
                                        project, exported, animation, tick / TICKS_PER_SECOND));
                float[] creases = creaseAngles(corners);
                report.append("  tick ").append(tick).append(':');
                for (int joint = 0; joint < baked[0].length; joint++) {
                    report.append(' ').append(String.format("%.3f", baked[0][joint]));
                    widestTop = Math.max(widestTop, baked[0][joint]);
                    worstDifference =
                            Math.max(worstDifference, Math.abs(baked[0][joint] - shown[0][joint]));
                }
                report.append(" |");
                for (int joint = 0; joint < baked[1].length; joint++) {
                    report.append(' ').append(String.format("%.3f", baked[1][joint]));
                    widestBottom = Math.max(widestBottom, baked[1][joint]);
                    worstDifference =
                            Math.max(worstDifference, Math.abs(baked[1][joint] - shown[1][joint]));
                }
                report.append("  crease");
                for (int joint = 0; joint < creases.length; joint++) {
                    report.append(' ').append(String.format("%.2f", creases[joint]));
                }
                report.append('\n');
            }
            report.append("  widest top ")
                    .append(String.format("%.3f", widestTop))
                    .append(", bottom ")
                    .append(String.format("%.3f", widestBottom))
                    .append('\n');
        }

        Path out = Path.of("build", "blockbench-seams.txt");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.toString());
        System.out.println(report);

        assertTrue(
                worstDifference <= TOLERANCE,
                "The export's seam gaps differ from the baked model's by " + worstDifference);
    }

    /**
     * Every face must put the same corner of the texture on the same corner of the cube.
     *
     * <p>Comparing the four UV numbers cannot see a flip: the same rectangle describes a face and
     * its mirror image. So this takes the texture corner at the rectangle's first end, finds the
     * cube corner Blockbench draws it at (js/outliner/types/cube.js, UVToLocal at uv[0], uv[1]),
     * and compares that with the cube corner ModelPart.Cube gives the vertex with the same texture
     * coordinates. A flipped rectangle puts the texture corner on the opposite cube corner.
     */
    @Test
    void everyFacePutsTheTextureCornerOnTheSameCubeCorner() throws IOException {
        Path directory = exportDirectory();
        for (Carpet carpet : CARPETS) {
            JsonObject project = read(directory.resolve(carpet.name() + ".bbmodel"));
            Map<String, List<JsonObject>> exported = exportedCubes(project);
            Map<String, List<ModelPart.Cube>> baked = bakedCubes(carpet);

            assertEquals(
                    new java.util.TreeSet<>(baked.keySet()),
                    new java.util.TreeSet<>(exported.keySet()),
                    carpet.name() + ": part names");
            for (String part : baked.keySet()) {
                List<ModelPart.Cube> expected = baked.get(part);
                List<JsonObject> actual = exported.get(part);
                assertEquals(expected.size(), actual.size(), carpet.name() + " " + part);
                for (int i = 0; i < expected.size(); i++) {
                    ModelPart.Cube cube = expected.get(i);
                    JsonObject json = actual.get(i);
                    for (ModelPart.Polygon polygon : cube.polygons) {
                        String face = faceName(polygon.normal());
                        JsonArray uv =
                                json.getAsJsonObject("faces")
                                        .getAsJsonObject(face)
                                        .getAsJsonArray("uv");
                        float u = uv.get(0).getAsFloat();
                        float v = uv.get(1).getAsFloat();
                        Vector3f fromJava = vertexAt(polygon, u, v);
                        Vector3f fromBlockbench = blockbenchCorner(json, face);
                        String where = carpet.name() + " " + part + " cube " + i + " " + face;
                        assertEquals(fromJava.x, fromBlockbench.x, TOLERANCE, where + " x");
                        assertEquals(fromJava.y, fromBlockbench.y, TOLERANCE, where + " y");
                        assertEquals(fromJava.z, fromBlockbench.z, TOLERANCE, where + " z");

                        // The corner alone would pass for a rectangle of the wrong size, so the
                        // rectangle's extent is compared too. Blockbench writes up and down
                        // backwards, so the extent is the larger number minus the smaller.
                        float[] expectedExtent = extent(polygon);
                        float[] actualExtent =
                                extent(
                                        new float[] {
                                            uv.get(0).getAsFloat(),
                                            uv.get(1).getAsFloat(),
                                            uv.get(2).getAsFloat(),
                                            uv.get(3).getAsFloat()
                                        });
                        assertEquals(expectedExtent[0], actualExtent[0], TOLERANCE, where + " u");
                        assertEquals(expectedExtent[1], actualExtent[1], TOLERANCE, where + " v");
                    }
                }
            }
        }
    }

    /**
     * A polygon's UV rectangle in texture units, as its width and height. The four vertices are the
     * rectangle's four corners, so the smallest and largest of each coordinate give the extent.
     */
    private static float[] extent(ModelPart.Polygon polygon) {
        float minU = Float.MAX_VALUE;
        float minV = Float.MAX_VALUE;
        float maxU = -Float.MAX_VALUE;
        float maxV = -Float.MAX_VALUE;
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            minU = Math.min(minU, vertex.u() * MagicCarpetEntityModel.TEXTURE_WIDTH);
            maxU = Math.max(maxU, vertex.u() * MagicCarpetEntityModel.TEXTURE_WIDTH);
            minV = Math.min(minV, vertex.v() * MagicCarpetEntityModel.TEXTURE_HEIGHT);
            maxV = Math.max(maxV, vertex.v() * MagicCarpetEntityModel.TEXTURE_HEIGHT);
        }
        return new float[] {maxU - minU, maxV - minV};
    }

    /** A rectangle's width and height, whichever way round it is written. */
    private static float[] extent(float[] uv) {
        return new float[] {Math.abs(uv[2] - uv[0]), Math.abs(uv[3] - uv[1])};
    }

    /** The cube corner of the polygon's vertex at texture coordinates (u, v), in texture units. */
    private static Vector3f vertexAt(ModelPart.Polygon polygon, float u, float v) {
        for (ModelPart.Vertex vertex : polygon.vertices()) {
            if (Math.abs(vertex.u() * MagicCarpetEntityModel.TEXTURE_WIDTH - u) < TOLERANCE
                    && Math.abs(vertex.v() * MagicCarpetEntityModel.TEXTURE_HEIGHT - v)
                            < TOLERANCE) {
                return new Vector3f(vertex.x(), vertex.y(), vertex.z());
            }
        }
        throw new AssertionError("no vertex at texture corner " + u + ", " + v);
    }

    /**
     * The cube corner Blockbench draws the face's first texture corner at, in Minecraft's model
     * space. This is Blockbench's UVToLocal at (uv[0], uv[1]), where both lerps are zero, so only
     * the face's own formula is left. The result is relative to the cube's origin, and the mirror
     * into Minecraft's space acts on that offset as (-x, -y, z).
     */
    private static Vector3f blockbenchCorner(JsonObject cube, String face) {
        JsonArray from = cube.getAsJsonArray("from");
        JsonArray to = cube.getAsJsonArray("to");
        JsonArray origin = cube.getAsJsonArray("origin");
        float[] f = new float[3];
        float[] t = new float[3];
        for (int i = 0; i < 3; i++) {
            f[i] = from.get(i).getAsFloat();
            t[i] = to.get(i).getAsFloat();
        }
        float[] point = new float[3];
        switch (face) {
            case "east" -> {
                point[0] = t[0];
                point[1] = t[1];
                point[2] = t[2];
            }
            case "west" -> {
                point[0] = f[0];
                point[1] = t[1];
                point[2] = f[2];
            }
            case "up" -> {
                point[0] = f[0];
                point[1] = t[1];
                point[2] = f[2];
            }
            case "down" -> {
                point[0] = f[0];
                point[1] = f[1];
                point[2] = t[2];
            }
            case "south" -> {
                point[0] = f[0];
                point[1] = t[1];
                point[2] = t[2];
            }
            default -> {
                point[0] = t[0];
                point[1] = t[1];
                point[2] = f[2];
            }
        }
        for (int i = 0; i < 3; i++) {
            point[i] -= origin.get(i).getAsFloat();
        }
        return new Vector3f(-point[0], -point[1], point[2]);
    }

    /**
     * The Blockbench face name for a polygon's normal, in Minecraft's model space. Blockbench's
     * layout is mirrored from ModelPart.Cube's, so its up rectangle is Java's down face and its
     * east is Java's west.
     */
    private static String faceName(org.joml.Vector3fc normal) {
        if (normal.y() > 0.5F) {
            return "down";
        }
        if (normal.y() < -0.5F) {
            return "up";
        }
        if (normal.x() > 0.5F) {
            return "west";
        }
        if (normal.x() < -0.5F) {
            return "east";
        }
        return normal.z() > 0.0F ? "south" : "north";
    }

    /** Each part's cubes, in the order the export writes them. */
    private static Map<String, List<ModelPart.Cube>> bakedCubes(Carpet carpet) {
        ModelPart root =
                MagicCarpetEntityModel.createBodyLayer(carpet.tassels(), carpet.curls()).bakeRoot();
        Map<String, List<ModelPart.Cube>> cubes = new LinkedHashMap<>();
        root.visit(
                new PoseStack(),
                (pose, path, index, cube) ->
                        cubes.computeIfAbsent(path, key -> new ArrayList<>()).add(cube));
        return cubes;
    }

    /** Each part's cubes as the .bbmodel writes them. */
    private static Map<String, List<JsonObject>> exportedCubes(JsonObject project) {
        Map<String, JsonObject> groups = new LinkedHashMap<>();
        for (JsonElement element : project.getAsJsonArray("groups")) {
            groups.put(
                    element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }
        Map<String, JsonObject> cubes = new LinkedHashMap<>();
        for (JsonElement element : project.getAsJsonArray("elements")) {
            cubes.put(
                    element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }
        Map<String, List<JsonObject>> out = new LinkedHashMap<>();
        collectCubes(project.getAsJsonArray("outliner"), "", groups, cubes, out);
        return out;
    }

    private static void collectCubes(
            JsonArray outliner,
            String path,
            Map<String, JsonObject> groups,
            Map<String, JsonObject> cubes,
            Map<String, List<JsonObject>> out) {
        for (JsonElement element : outliner) {
            if (element.isJsonPrimitive()) {
                continue;
            }
            JsonObject group = groups.get(element.getAsJsonObject().get("uuid").getAsString());
            String childPath = path + "/" + group.get("name").getAsString();
            List<JsonObject> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonObject().getAsJsonArray("children")) {
                if (child.isJsonPrimitive() && cubes.containsKey(child.getAsString())) {
                    list.add(cubes.get(child.getAsString()));
                }
            }
            if (!list.isEmpty()) {
                out.put(childPath, list);
            }
            collectCubes(
                    element.getAsJsonObject().getAsJsonArray("children"),
                    childPath,
                    groups,
                    cubes,
                    out);
        }
    }

    /** The eight corners in a fixed order, so two corner sets can be compared index by index. */
    private static Vector3f[] sorted(Vector3f[] corners) {
        Vector3f[] copy = corners.clone();
        java.util.Arrays.sort(
                copy,
                (a, b) -> {
                    int byX = Float.compare(a.x, b.x);
                    if (byX != 0) {
                        return byX;
                    }
                    int byY = Float.compare(a.y, b.y);
                    return byY != 0 ? byY : Float.compare(a.z, b.z);
                });
        return copy;
    }

    /** The phase after {@code ticks} ticks at a constant frequency, as advancePhase leaves it. */
    private static float phase(float frequency, int ticks) {
        float phase = 0.0F;
        for (int tick = 0; tick < ticks; tick++) {
            phase = CarpetAnimation.advancePhase(phase, frequency);
        }
        return phase;
    }

    // --- The baked model --------------------------------------------------------------------

    private static Map<String, List<Vector3f[]>> bakedCorners(Carpet carpet, int tick) {
        LayerDefinition layer =
                MagicCarpetEntityModel.createBodyLayer(carpet.tassels(), carpet.curls());
        ModelPart root = layer.bakeRoot();
        MagicCarpetEntityModel model = new MagicCarpetEntityModel(root);

        MagicCarpetEntityRenderState state = new MagicCarpetEntityRenderState();
        state.ageInTicks = tick;
        state.animationOffset = 0.0F;
        state.speed = carpet.speed();
        // The entity advances both phases one tick at a time (CarpetAnimation.advancePhase), and
        // the render state carries the phase of the tick being drawn.
        state.ripplePhase = phase(CarpetAnimation.rippleFrequency(carpet.speed()), tick);
        state.flutterPhase = phase(CarpetAnimation.flutterFrequency(carpet.speed()), tick);
        model.setupAnim(state);

        Map<String, List<Vector3f[]>> corners = new LinkedHashMap<>();
        root.visit(
                new PoseStack(),
                (pose, path, index, cube) -> {
                    Matrix4f matrix = pose.pose();
                    Vector3f[] points = new Vector3f[8];
                    int i = 0;
                    for (float x : new float[] {cube.minX, cube.maxX}) {
                        for (float y : new float[] {cube.minY, cube.maxY}) {
                            for (float z : new float[] {cube.minZ, cube.maxZ}) {
                                points[i++] =
                                        matrix.transformPosition(
                                                x / 16.0F, y / 16.0F, z / 16.0F, new Vector3f());
                                points[i - 1].mul(16.0F);
                            }
                        }
                    }
                    corners.computeIfAbsent(path, key -> new ArrayList<>()).add(points);
                });
        return corners;
    }

    /**
     * The crack at each of the seven joints, in model pixels: the top edge and the bottom edge.
     *
     * <p>The back segment's end face is at its largest local z and the front segment's at its
     * smallest. Both faces are centred on x = 0, so their middles coincide when the segments are
     * level. The top edge is the pair of corners with the smallest y, the bottom edge the pair with
     * the largest.
     */
    private static float[][] seamGaps(Map<String, List<Vector3f[]>> baked) {
        float[][] gaps = new float[2][SEGMENTS - 1];
        for (int joint = 0; joint < SEGMENTS - 1; joint++) {
            Vector3f[] back = baked.get("/carpet/segment_" + joint).get(0);
            Vector3f[] front = baked.get("/carpet/segment_" + (joint + 1)).get(0);
            for (int edge = 0; edge < 2; edge++) {
                boolean top = edge == 0;
                gaps[edge][joint] =
                        edgeMiddle(back, true, top).distance(edgeMiddle(front, false, top));
            }
        }
        return gaps;
    }

    /**
     * The middle of one end face's top or bottom edge. Corners are indexed x, then y, then z, so
     * the odd indices are the local z = max face and the even ones the z = min face; of that face's
     * four corners, the two with the smallest y are the top edge and the two with the largest the
     * bottom.
     */
    private static Vector3f edgeMiddle(Vector3f[] corners, boolean back, boolean top) {
        int first = back ? 1 : 0;
        Vector3f a = corners[first];
        Vector3f b = corners[first + 2];
        Vector3f c = corners[first + 4];
        Vector3f d = corners[first + 6];
        if (top) {
            if (a.y > b.y) {
                Vector3f swap = a;
                a = b;
                b = swap;
            }
            if (c.y > d.y) {
                Vector3f swap = c;
                c = d;
                d = swap;
            }
        } else {
            if (a.y < b.y) {
                Vector3f swap = a;
                a = b;
                b = swap;
            }
            if (c.y < d.y) {
                Vector3f swap = c;
                c = d;
                d = swap;
            }
        }
        return a.add(c, new Vector3f()).mul(0.5F);
    }

    /**
     * The angle, in degrees, between each joint's two end faces. The faces are not coplanar, so the
     * crease is what shows them: the render type does not cull back faces, so both are drawn and
     * the crease is a V of end-face texture.
     */
    private static float[] creaseAngles(Map<String, List<Vector3f[]>> baked) {
        float[] angles = new float[SEGMENTS - 1];
        for (int joint = 0; joint < angles.length; joint++) {
            Vector3f back = endNormal(baked.get("/carpet/segment_" + joint).get(0), true);
            Vector3f front = endNormal(baked.get("/carpet/segment_" + (joint + 1)).get(0), false);
            angles[joint] =
                    (float) Math.toDegrees(Math.acos(Math.min(1.0, Math.abs(back.dot(front)))));
        }
        return angles;
    }

    /** A cube's end-face normal, from the cross product of the face's two diagonals. */
    private static Vector3f endNormal(Vector3f[] corners, boolean back) {
        int first = back ? 1 : 0;
        Vector3f along = corners[first + 4].sub(corners[first], new Vector3f());
        Vector3f up = corners[first + 2].sub(corners[first], new Vector3f());
        return along.cross(up, new Vector3f()).normalize();
    }

    // --- The exported model ----------------------------------------------------------------

    private static Path exportDirectory() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("tools/blockbench/basic.bbmodel");
            if (Files.exists(candidate)) {
                return candidate.getParent();
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException(
                "tools/blockbench/basic.bbmodel not found; run tools/blockbench_export.py");
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    /** Each cube's eight corners in Blockbench space, mapped back to Minecraft's model space. */
    private static Map<String, List<Vector3f[]>> exportedCorners(JsonObject project) {
        Map<String, JsonObject> groups = new LinkedHashMap<>();
        for (JsonElement element : project.getAsJsonArray("groups")) {
            groups.put(
                    element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }
        Map<String, JsonObject> cubes = new LinkedHashMap<>();
        for (JsonElement element : project.getAsJsonArray("elements")) {
            cubes.put(
                    element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }

        Map<String, List<Vector3f[]>> corners = new LinkedHashMap<>();
        collect(project.getAsJsonArray("outliner"), "", groups, cubes, corners);
        return corners;
    }

    private static void collect(
            JsonArray outliner,
            String path,
            Map<String, JsonObject> groups,
            Map<String, JsonObject> cubes,
            Map<String, List<Vector3f[]>> corners) {
        for (JsonElement element : outliner) {
            if (element.isJsonPrimitive()) {
                continue;
            }
            JsonObject group = groups.get(element.getAsJsonObject().get("uuid").getAsString());
            String name = group.get("name").getAsString();
            String childPath = path + "/" + name;
            List<Vector3f[]> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonObject().getAsJsonArray("children")) {
                if (!child.isJsonPrimitive()) {
                    continue;
                }
                JsonObject cube = cubes.get(child.getAsString());
                if (cube == null) {
                    continue;
                }
                list.add(cubeCorners(cube));
            }
            if (!list.isEmpty()) {
                corners.put(childPath, list);
            }
            collect(
                    element.getAsJsonObject().getAsJsonArray("children"),
                    childPath,
                    groups,
                    cubes,
                    corners);
        }
    }

    private static Vector3f[] cubeCorners(JsonObject cube) {
        JsonArray from = cube.getAsJsonArray("from");
        JsonArray to = cube.getAsJsonArray("to");
        Vector3f[] points = new Vector3f[8];
        int i = 0;
        for (float x : new float[] {from.get(0).getAsFloat(), to.get(0).getAsFloat()}) {
            for (float y : new float[] {from.get(1).getAsFloat(), to.get(1).getAsFloat()}) {
                for (float z : new float[] {from.get(2).getAsFloat(), to.get(2).getAsFloat()}) {
                    points[i++] = new Vector3f(x, y, z);
                }
            }
        }
        return points;
    }

    /**
     * The corners the .bbmodel draws at {@code time}, in Minecraft's model space.
     *
     * <p>Blockbench places a group at its origin minus its parent's origin, rotates it in ZYX
     * order, and adds the animation's position and rotation keyframes to those rest values
     * (js/outliner/outliner.js, updateTransform; js/animations/timeline_animators.js,
     * displayPosition and displayRotation). A cube's corners are stored absolute and its mesh sits
     * at its origin, which the export sets to its group's origin, so the geometry the group's
     * transform applies to is the corner minus that origin.
     */
    private static Map<String, List<Vector3f[]>> animatedCorners(
            JsonObject project,
            Map<String, List<Vector3f[]>> exported,
            String animation,
            float time) {
        Map<String, JsonObject> groups = new LinkedHashMap<>();
        for (JsonElement element : project.getAsJsonArray("groups")) {
            groups.put(
                    element.getAsJsonObject().get("uuid").getAsString(), element.getAsJsonObject());
        }
        JsonObject animators = animation(project, animation);

        Map<String, List<Vector3f[]>> corners = new LinkedHashMap<>();
        walk(
                project.getAsJsonArray("outliner"),
                "",
                null,
                new Matrix4f(),
                groups,
                exported,
                animators,
                time,
                corners);
        return corners;
    }

    private static void walk(
            JsonArray outliner,
            String path,
            JsonObject parent,
            Matrix4f parentMatrix,
            Map<String, JsonObject> groups,
            Map<String, List<Vector3f[]>> exported,
            JsonObject animators,
            float time,
            Map<String, List<Vector3f[]>> corners) {
        for (JsonElement element : outliner) {
            if (element.isJsonPrimitive()) {
                continue;
            }
            JsonObject group = groups.get(element.getAsJsonObject().get("uuid").getAsString());
            String name = group.get("name").getAsString();
            String childPath = path + "/" + name;

            JsonArray origin = group.getAsJsonArray("origin");
            JsonArray rotation = group.getAsJsonArray("rotation");
            float restX = origin.get(0).getAsFloat();
            float restY = origin.get(1).getAsFloat();
            float restZ = origin.get(2).getAsFloat();
            float x = restX;
            float y = restY;
            float z = restZ;
            float rx = (float) Math.toRadians(rotation.get(0).getAsFloat());
            float ry = (float) Math.toRadians(rotation.get(1).getAsFloat());
            float rz = (float) Math.toRadians(rotation.get(2).getAsFloat());

            JsonElement found = animators.get(group.get("uuid").getAsString());
            JsonObject animator = found == null ? null : found.getAsJsonObject();
            if (animator != null) {
                float[] position = sample(animator, "position", time);
                float[] angles = sample(animator, "rotation", time);
                x += position[0];
                y += position[1];
                z += position[2];
                rx += (float) Math.toRadians(angles[0]);
                ry += (float) Math.toRadians(angles[1]);
                rz += (float) Math.toRadians(angles[2]);
            }

            Matrix4f matrix = new Matrix4f(parentMatrix);
            if (parent == null) {
                matrix.translate(x, y, z);
            } else {
                JsonArray parentOrigin = parent.getAsJsonArray("origin");
                matrix.translate(
                        x - parentOrigin.get(0).getAsFloat(),
                        y - parentOrigin.get(1).getAsFloat(),
                        z - parentOrigin.get(2).getAsFloat());
            }
            matrix.rotate(new org.joml.Quaternionf().rotationZYX(rz, ry, rx));

            List<Vector3f[]> list = new ArrayList<>();
            for (Vector3f[] cube : exported.getOrDefault(childPath, List.of())) {
                Vector3f[] points = new Vector3f[8];
                for (int i = 0; i < 8; i++) {
                    // A cube's mesh sits at its origin, so its geometry is the corner minus that.
                    Vector3f point =
                            matrix.transformPosition(
                                    cube[i].sub(restX, restY, restZ, new Vector3f()),
                                    new Vector3f());
                    // Blockbench's Java entity space is Minecraft's mirrored in X and Y.
                    points[i] = new Vector3f(-point.x, 24.0F - point.y, point.z);
                }
                list.add(points);
            }
            if (!list.isEmpty()) {
                corners.put(childPath, list);
            }

            walk(
                    element.getAsJsonObject().getAsJsonArray("children"),
                    childPath,
                    group,
                    matrix,
                    groups,
                    exported,
                    animators,
                    time,
                    corners);
        }
    }

    private static JsonObject animation(JsonObject project, String name) {
        for (JsonElement element : project.getAsJsonArray("animations")) {
            JsonObject animation = element.getAsJsonObject();
            if (animation.get("name").getAsString().equals(name)) {
                return animation.getAsJsonObject("animators");
            }
        }
        throw new IllegalStateException("no animation named " + name);
    }

    /** A channel's value at {@code time}, interpolated linearly between its keyframes. */
    private static float[] sample(JsonObject animator, String channel, float time) {
        List<JsonObject> keyframes = new ArrayList<>();
        for (JsonElement element : animator.getAsJsonArray("keyframes")) {
            JsonObject keyframe = element.getAsJsonObject();
            if (keyframe.get("channel").getAsString().equals(channel)) {
                keyframes.add(keyframe);
            }
        }
        keyframes.sort(
                (a, b) -> Float.compare(a.get("time").getAsFloat(), b.get("time").getAsFloat()));

        JsonObject before = null;
        JsonObject after = null;
        for (JsonObject keyframe : keyframes) {
            if (keyframe.get("time").getAsFloat() <= time) {
                before = keyframe;
            } else if (after == null) {
                after = keyframe;
            }
        }
        if (before == null) {
            return point(after);
        }
        if (after == null) {
            return point(before);
        }
        float span = after.get("time").getAsFloat() - before.get("time").getAsFloat();
        float alpha = span == 0.0F ? 0.0F : (time - before.get("time").getAsFloat()) / span;
        float[] from = point(before);
        float[] to = point(after);
        return new float[] {
            from[0] + alpha * (to[0] - from[0]),
            from[1] + alpha * (to[1] - from[1]),
            from[2] + alpha * (to[2] - from[2])
        };
    }

    private static float[] point(JsonObject keyframe) {
        JsonObject data = keyframe.getAsJsonArray("data_points").get(0).getAsJsonObject();
        return new float[] {
            Float.parseFloat(data.get("x").getAsString()),
            Float.parseFloat(data.get("y").getAsString()),
            Float.parseFloat(data.get("z").getAsString())
        };
    }
}
