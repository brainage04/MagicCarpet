package io.github.brainage04.magic_carpet.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * The carpet's body as one surface, bent at the nine joints {@link MagicCarpetPose} gives. Each
 * joint is a row of vertices that the quads either side of it share, so the body has no crack
 * between segments and no two quads overlap in one plane.
 *
 * <p>The texture layout (96x48 texture units) is shared with {@code
 * tools/generate_carpet_assets.py}, which paints the textures and item models; change both
 * together. Every quad takes its UVs from the box the old rigid segments were built from, so the
 * texture is unchanged.
 */
public final class MagicCarpetBodyMesh {
    private static final int TEXTURE_WIDTH = MagicCarpetEntityModel.TEXTURE_WIDTH;
    private static final int TEXTURE_HEIGHT = MagicCarpetEntityModel.TEXTURE_HEIGHT;

    private static final float HALF_WIDTH = 12.0F;
    private static final float INNER = 10.0F;
    private static final float CUT_HALF_WIDTH = 8.0F;
    private static final float BOTTOM = -1.0F;
    private static final float SLAB_TOP = 0.0F;
    private static final float BORDER_TOP = 1.0F;
    private static final float FRONT_BORDER_END = 2.0F;
    private static final float BACK_BORDER_START = 30.0F;
    private static final float LENGTH = MagicCarpetPose.SEGMENTS * MagicCarpetPose.SEGMENT_LENGTH;

    /**
     * The offsets along the body where a row of vertices sits: the nine joints, plus the two places
     * where the raised front and back borders end.
     */
    private static final float[] STATIONS = {
        0.0F, 2.0F, 4.0F, 8.0F, 12.0F, 16.0F, 20.0F, 24.0F, 28.0F, 30.0F, 32.0F
    };

    private MagicCarpetBodyMesh() {}

    public static void render(
            PoseStack.Pose pose,
            VertexConsumer buffer,
            MagicCarpetPose carpet,
            boolean curls,
            int light,
            int overlay,
            int color) {
        Sink sink = new Sink(pose, buffer, light, overlay, color);
        float[] heights = carpet.heights();
        for (int station = 0; station < STATIONS.length - 1; station++) {
            float from = STATIONS[station];
            float to = STATIONS[station + 1];
            float zFrom = MagicCarpetPose.FRONT_Z + from;
            float zTo = MagicCarpetPose.FRONT_Z + to;
            float hFrom = height(heights, from);
            float hTo = height(heights, to);
            int segment = (int) (from / MagicCarpetPose.SEGMENT_LENGTH);
            boolean cut = curls && segment == 0;
            float half = cut ? CUT_HALF_WIDTH : HALF_WIDTH;
            float inner = cut ? CUT_HALF_WIDTH : INNER;
            Box slab = slab(segment, cut);

            sink.strip(slab, Side.DOWN, zFrom, zTo, hFrom, hTo).horizontal(-half, half, BOTTOM);
            sink.strip(slab, Side.WEST, zFrom, zTo, hFrom, hTo).wall(-half, BOTTOM, SLAB_TOP);
            sink.strip(slab, Side.EAST, zFrom, zTo, hFrom, hTo).wall(half, BOTTOM, SLAB_TOP);

            if (to <= FRONT_BORDER_END || from >= BACK_BORDER_START) {
                if (half > inner) {
                    sink.strip(slab, Side.UP, zFrom, zTo, hFrom, hTo)
                            .horizontal(-half, -inner, SLAB_TOP);
                    sink.strip(slab, Side.UP, zFrom, zTo, hFrom, hTo)
                            .horizontal(inner, half, SLAB_TOP);
                }
            } else {
                sink.strip(slab, Side.UP, zFrom, zTo, hFrom, hTo).horizontal(-half, half, SLAB_TOP);
            }

            if (!cut) {
                Box left = sideBorder(segment, -HALF_WIDTH);
                Box right = sideBorder(segment, INNER);
                sink.strip(left, Side.UP, zFrom, zTo, hFrom, hTo)
                        .horizontal(-HALF_WIDTH, -INNER, BORDER_TOP);
                sink.strip(right, Side.UP, zFrom, zTo, hFrom, hTo)
                        .horizontal(INNER, HALF_WIDTH, BORDER_TOP);
                sink.strip(left, Side.WEST, zFrom, zTo, hFrom, hTo)
                        .wall(-HALF_WIDTH, SLAB_TOP, BORDER_TOP);
                sink.strip(left, Side.EAST, zFrom, zTo, hFrom, hTo)
                        .wall(-INNER, SLAB_TOP, BORDER_TOP);
                sink.strip(right, Side.WEST, zFrom, zTo, hFrom, hTo)
                        .wall(INNER, SLAB_TOP, BORDER_TOP);
                sink.strip(right, Side.EAST, zFrom, zTo, hFrom, hTo)
                        .wall(HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            }

            if (to <= FRONT_BORDER_END) {
                sink.strip(frontBorder(curls), Side.UP, zFrom, zTo, hFrom, hTo)
                        .horizontal(-inner, inner, BORDER_TOP);
            }
            if (from >= BACK_BORDER_START) {
                sink.strip(backBorder(), Side.UP, zFrom, zTo, hFrom, hTo)
                        .horizontal(-INNER, INNER, BORDER_TOP);
            }
        }

        ends(sink, heights, curls);
    }

    /** The front and back of the body, and the two steps where the raised borders end. */
    private static void ends(Sink sink, float[] heights, boolean curls) {
        float front = heights[0];
        float back = heights[MagicCarpetPose.SEGMENTS];
        float half = curls ? CUT_HALF_WIDTH : HALF_WIDTH;
        float inner = curls ? CUT_HALF_WIDTH : INNER;
        float frontZ = MagicCarpetPose.FRONT_Z;
        float backZ = MagicCarpetPose.FRONT_Z + LENGTH;
        Box frontSlab = slab(0, curls);
        Box backSlab = slab(MagicCarpetPose.SEGMENTS - 1, false);
        Box frontBorder = frontBorder(curls);
        Box backBorder = backBorder();

        sink.cap(frontSlab, Side.NORTH, frontZ, front).face(-half, half, BOTTOM, SLAB_TOP);
        sink.cap(backSlab, Side.SOUTH, backZ, back).face(-HALF_WIDTH, HALF_WIDTH, BOTTOM, SLAB_TOP);
        sink.cap(frontBorder, Side.NORTH, frontZ, front).face(-inner, inner, SLAB_TOP, BORDER_TOP);
        sink.cap(backBorder, Side.SOUTH, backZ, back).face(-INNER, INNER, SLAB_TOP, BORDER_TOP);
        sink.cap(
                        frontBorder,
                        Side.SOUTH,
                        frontZ + FRONT_BORDER_END,
                        height(heights, FRONT_BORDER_END))
                .face(-inner, inner, SLAB_TOP, BORDER_TOP);
        sink.cap(
                        backBorder,
                        Side.NORTH,
                        frontZ + BACK_BORDER_START,
                        height(heights, BACK_BORDER_START))
                .face(-INNER, INNER, SLAB_TOP, BORDER_TOP);

        if (curls) {
            float stepZ = frontZ + MagicCarpetPose.SEGMENT_LENGTH;
            float step = height(heights, MagicCarpetPose.SEGMENT_LENGTH);
            Box second = slab(1, false);
            sink.cap(frontSlab, Side.SOUTH, stepZ, step)
                    .face(-CUT_HALF_WIDTH, CUT_HALF_WIDTH, BOTTOM, SLAB_TOP);
            sink.cap(second, Side.NORTH, stepZ, step)
                    .face(-HALF_WIDTH, -CUT_HALF_WIDTH, BOTTOM, SLAB_TOP);
            sink.cap(second, Side.NORTH, stepZ, step)
                    .face(CUT_HALF_WIDTH, HALF_WIDTH, BOTTOM, SLAB_TOP);
            sink.cap(sideBorder(1, -HALF_WIDTH), Side.NORTH, stepZ, step)
                    .face(-HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
            sink.cap(sideBorder(1, INNER), Side.NORTH, stepZ, step)
                    .face(INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            float borderEnd = height(heights, FRONT_BORDER_END);
            sink.strip(frontBorder, Side.WEST, frontZ, frontZ + FRONT_BORDER_END, front, borderEnd)
                    .wall(-CUT_HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            sink.strip(frontBorder, Side.EAST, frontZ, frontZ + FRONT_BORDER_END, front, borderEnd)
                    .wall(CUT_HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            return;
        }

        sink.cap(sideBorder(0, -HALF_WIDTH), Side.NORTH, frontZ, front)
                .face(-HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
        sink.cap(sideBorder(0, INNER), Side.NORTH, frontZ, front)
                .face(INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
        sink.cap(sideBorder(MagicCarpetPose.SEGMENTS - 1, -HALF_WIDTH), Side.SOUTH, backZ, back)
                .face(-HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
        sink.cap(sideBorder(MagicCarpetPose.SEGMENTS - 1, INNER), Side.SOUTH, backZ, back)
                .face(INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
    }

    /** The height of the body at {@code offset} along it, between the joints either side. */
    private static float height(float[] heights, float offset) {
        int segment = (int) (offset / MagicCarpetPose.SEGMENT_LENGTH);
        if (segment >= MagicCarpetPose.SEGMENTS) {
            return heights[MagicCarpetPose.SEGMENTS];
        }
        float along =
                (offset - segment * MagicCarpetPose.SEGMENT_LENGTH)
                        / MagicCarpetPose.SEGMENT_LENGTH;
        return Mth.lerp(along, heights[segment], heights[segment + 1]);
    }

    private static Box slab(int segment, boolean cut) {
        return cut
                ? new Box(
                        56.0F, 40.0F, 16.0F, 4.0F, -CUT_HALF_WIDTH, BOTTOM, MagicCarpetPose.FRONT_Z)
                : new Box(
                        0.0F,
                        5.0F * segment,
                        24.0F,
                        4.0F,
                        -HALF_WIDTH,
                        BOTTOM,
                        MagicCarpetPose.FRONT_Z + MagicCarpetPose.SEGMENT_LENGTH * segment);
    }

    private static Box sideBorder(int segment, float minX) {
        return new Box(
                56.0F,
                5.0F * segment,
                2.0F,
                4.0F,
                minX,
                SLAB_TOP,
                MagicCarpetPose.FRONT_Z + MagicCarpetPose.SEGMENT_LENGTH * segment);
    }

    private static Box frontBorder(boolean cut) {
        return cut
                ? new Box(
                        0.0F,
                        43.0F,
                        16.0F,
                        2.0F,
                        -CUT_HALF_WIDTH,
                        SLAB_TOP,
                        MagicCarpetPose.FRONT_Z)
                : new Box(0.0F, 40.0F, 20.0F, 2.0F, -INNER, SLAB_TOP, MagicCarpetPose.FRONT_Z);
    }

    private static Box backBorder() {
        return new Box(
                0.0F,
                40.0F,
                20.0F,
                2.0F,
                -INNER,
                SLAB_TOP,
                MagicCarpetPose.FRONT_Z + BACK_BORDER_START);
    }

    private enum Side {
        UP,
        DOWN,
        WEST,
        EAST,
        NORTH,
        SOUTH
    }

    /**
     * Where the body's quads go: the pose, the buffer, and the light, overlay and colour of the
     * pass.
     */
    private record Sink(
            PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        private Strip strip(Box box, Side side, float zFrom, float zTo, float hFrom, float hTo) {
            return new Strip(this, box, side, zFrom, zTo, hFrom, hTo);
        }

        private Cap cap(Box box, Side side, float z, float height) {
            return new Cap(this, box, side, z, height);
        }

        private void vertex(
                Box box,
                Side side,
                float x,
                float y,
                float z,
                float height,
                float nx,
                float ny,
                float nz) {
            float localZ = z - box.minZ();
            // Model units are 1/16 of a block, as ModelPart.Cube.compile divides them (26.3).
            this.buffer
                    .addVertex(this.pose, x / 16.0F, (height + y) / 16.0F, z / 16.0F)
                    .setColor(this.color)
                    .setUv(
                            box.u(side, x, localZ) / TEXTURE_WIDTH,
                            box.v(side, y, localZ) / TEXTURE_HEIGHT)
                    .setOverlay(this.overlay)
                    .setLight(this.light)
                    .setNormal(this.pose, nx, ny, nz);
        }
    }

    /** A face that runs along the body between two rows, on one side of one box. */
    private record Strip(
            Sink sink, Box box, Side side, float zFrom, float zTo, float hFrom, float hTo) {
        /** The strip as a top or bottom face, between two x and at one height. */
        private void horizontal(float x0, float x1, float y) {
            float dz = this.zTo - this.zFrom;
            float dh = this.hTo - this.hFrom;
            float length = Mth.sqrt(dz * dz + dh * dh);
            float sign = this.side == Side.UP ? 1.0F : -1.0F;
            float ny = sign * dz / length;
            float nz = -sign * dh / length;
            if (this.side == Side.UP) {
                vertex(x1, y, this.zFrom, this.hFrom, 0.0F, ny, nz);
                vertex(x0, y, this.zFrom, this.hFrom, 0.0F, ny, nz);
                vertex(x0, y, this.zTo, this.hTo, 0.0F, ny, nz);
                vertex(x1, y, this.zTo, this.hTo, 0.0F, ny, nz);
            } else {
                vertex(x1, y, this.zTo, this.hTo, 0.0F, ny, nz);
                vertex(x0, y, this.zTo, this.hTo, 0.0F, ny, nz);
                vertex(x0, y, this.zFrom, this.hFrom, 0.0F, ny, nz);
                vertex(x1, y, this.zFrom, this.hFrom, 0.0F, ny, nz);
            }
        }

        /** The strip as a west or east face, at one x between two heights. */
        private void wall(float x, float y0, float y1) {
            float nx = this.side == Side.WEST ? -1.0F : 1.0F;
            if (this.side == Side.WEST) {
                vertex(x, y0, this.zFrom, this.hFrom, nx, 0.0F, 0.0F);
                vertex(x, y0, this.zTo, this.hTo, nx, 0.0F, 0.0F);
                vertex(x, y1, this.zTo, this.hTo, nx, 0.0F, 0.0F);
                vertex(x, y1, this.zFrom, this.hFrom, nx, 0.0F, 0.0F);
            } else {
                vertex(x, y0, this.zTo, this.hTo, nx, 0.0F, 0.0F);
                vertex(x, y0, this.zFrom, this.hFrom, nx, 0.0F, 0.0F);
                vertex(x, y1, this.zFrom, this.hFrom, nx, 0.0F, 0.0F);
                vertex(x, y1, this.zTo, this.hTo, nx, 0.0F, 0.0F);
            }
        }

        private void vertex(float x, float y, float z, float height, float nx, float ny, float nz) {
            this.sink.vertex(this.box, this.side, x, y, z, height, nx, ny, nz);
        }
    }

    /** A face across the body at one z, on one side of one box. */
    private record Cap(Sink sink, Box box, Side side, float z, float height) {
        private void face(float x0, float x1, float y0, float y1) {
            float nz = this.side == Side.NORTH ? -1.0F : 1.0F;
            if (this.side == Side.NORTH) {
                vertex(x1, y0, 0.0F, 0.0F, nz);
                vertex(x0, y0, 0.0F, 0.0F, nz);
                vertex(x0, y1, 0.0F, 0.0F, nz);
                vertex(x1, y1, 0.0F, 0.0F, nz);
            } else {
                vertex(x0, y0, 0.0F, 0.0F, nz);
                vertex(x1, y0, 0.0F, 0.0F, nz);
                vertex(x1, y1, 0.0F, 0.0F, nz);
                vertex(x0, y1, 0.0F, 0.0F, nz);
            }
        }

        private void vertex(float x, float y, float nx, float ny, float nz) {
            this.sink.vertex(this.box, this.side, x, y, this.z, this.height, nx, ny, nz);
        }
    }

    /**
     * The texture rectangle and extent of one face group. {@code u} and {@code v} reproduce
     * vanilla's {@code ModelPart.Cube} polygon UVs (ModelPart.java:286-318 in 26.3).
     */
    private record Box(
            float texU, float texV, float width, float depth, float minX, float minY, float minZ) {
        private float u1() {
            return this.texU + this.depth;
        }

        private float u2() {
            return this.texU + this.depth + this.width;
        }

        private float u4() {
            return this.texU + this.depth + this.width + this.depth + this.width;
        }

        private float v0() {
            return this.texV;
        }

        private float v1() {
            return this.texV + this.depth;
        }

        private float u(Side side, float x, float localZ) {
            return switch (side) {
                case UP -> this.u2() + (x - this.minX);
                case DOWN -> this.u1() + (x - this.minX);
                case WEST -> this.u1() - localZ;
                case EAST -> this.u2() + localZ;
                case NORTH -> this.u1() + (x - this.minX);
                case SOUTH -> this.u4() - (x - this.minX);
            };
        }

        private float v(Side side, float y, float localZ) {
            return switch (side) {
                case UP, DOWN -> this.v0() + this.depth - localZ;
                case WEST, EAST, NORTH, SOUTH -> this.v1() + (y - this.minY);
            };
        }
    }
}
