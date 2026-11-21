package io.github.brainage04.magic_carpet.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

/**
 * The carpet's body as one surface, bent at the nine joints {@link MagicCarpetPose} gives. Each
 * joint is a row of vertices that the quads either side of it share, so the body has no crack
 * between segments and no two quads in one plane.
 *
 * <p>The texture layout (96x48 texture units) is shared with {@code
 * tools/generate_carpet_assets.py}, which paints the textures and item models; change both
 * together. Every quad takes its UVs from the box the old rigid segments were built from, so the
 * texture is unchanged.
 */
public final class MagicCarpetBodyMesh {
    private static final int TEXTURE_WIDTH = 96;
    private static final int TEXTURE_HEIGHT = 48;

    private static final float HALF_WIDTH = 12.0F;
    private static final float INNER = 10.0F;
    private static final float CUT_HALF_WIDTH = 8.0F;
    private static final float BOTTOM = -1.0F;
    private static final float SLAB_TOP = 0.0F;
    private static final float BORDER_TOP = 1.0F;
    private static final float FRONT_BORDER_END = 2.0F;
    private static final float BACK_BORDER_START = 30.0F;
    private static final float LENGTH = 32.0F;

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

            horizontal(buffer, pose, light, overlay, color, slab, Side.DOWN, zFrom, zTo, hFrom, hTo, -half, half, BOTTOM);
            wall(buffer, pose, light, overlay, color, slab, Side.WEST, zFrom, zTo, hFrom, hTo, -half, BOTTOM, SLAB_TOP);
            wall(buffer, pose, light, overlay, color, slab, Side.EAST, zFrom, zTo, hFrom, hTo, half, BOTTOM, SLAB_TOP);

            if (to <= FRONT_BORDER_END || from >= BACK_BORDER_START) {
                if (half > inner) {
                    horizontal(buffer, pose, light, overlay, color, slab, Side.UP, zFrom, zTo, hFrom, hTo, -half, -inner, SLAB_TOP);
                    horizontal(buffer, pose, light, overlay, color, slab, Side.UP, zFrom, zTo, hFrom, hTo, inner, half, SLAB_TOP);
                }
            } else {
                horizontal(buffer, pose, light, overlay, color, slab, Side.UP, zFrom, zTo, hFrom, hTo, -half, half, SLAB_TOP);
            }

            if (!cut) {
                Box left = sideBorder(segment, -HALF_WIDTH);
                Box right = sideBorder(segment, INNER);
                horizontal(buffer, pose, light, overlay, color, left, Side.UP, zFrom, zTo, hFrom, hTo, -HALF_WIDTH, -INNER, BORDER_TOP);
                horizontal(buffer, pose, light, overlay, color, right, Side.UP, zFrom, zTo, hFrom, hTo, INNER, HALF_WIDTH, BORDER_TOP);
                wall(buffer, pose, light, overlay, color, left, Side.WEST, zFrom, zTo, hFrom, hTo, -HALF_WIDTH, SLAB_TOP, BORDER_TOP);
                wall(buffer, pose, light, overlay, color, left, Side.EAST, zFrom, zTo, hFrom, hTo, -INNER, SLAB_TOP, BORDER_TOP);
                wall(buffer, pose, light, overlay, color, right, Side.WEST, zFrom, zTo, hFrom, hTo, INNER, SLAB_TOP, BORDER_TOP);
                wall(buffer, pose, light, overlay, color, right, Side.EAST, zFrom, zTo, hFrom, hTo, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            }

            if (to <= FRONT_BORDER_END) {
                horizontal(buffer, pose, light, overlay, color, frontBorder(curls), Side.UP, zFrom, zTo, hFrom, hTo, -inner, inner, BORDER_TOP);
            }
            if (from >= BACK_BORDER_START) {
                horizontal(buffer, pose, light, overlay, color, backBorder(), Side.UP, zFrom, zTo, hFrom, hTo, -INNER, INNER, BORDER_TOP);
            }
        }

        ends(buffer, pose, light, overlay, color, heights, curls);
    }

    /** The front and back of the body, and the two steps where the raised borders end. */
    private static void ends(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int light,
            int overlay,
            int color,
            float[] heights,
            boolean curls) {
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

        cap(buffer, pose, light, overlay, color, frontSlab, Side.NORTH, frontZ, front, -half, half, BOTTOM, SLAB_TOP);
        cap(buffer, pose, light, overlay, color, backSlab, Side.SOUTH, backZ, back, -HALF_WIDTH, HALF_WIDTH, BOTTOM, SLAB_TOP);
        cap(buffer, pose, light, overlay, color, frontBorder, Side.NORTH, frontZ, front, -inner, inner, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, backBorder, Side.SOUTH, backZ, back, -INNER, INNER, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, frontBorder, Side.SOUTH, frontZ + FRONT_BORDER_END, height(heights, FRONT_BORDER_END), -inner, inner, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, backBorder, Side.NORTH, frontZ + BACK_BORDER_START, height(heights, BACK_BORDER_START), -INNER, INNER, SLAB_TOP, BORDER_TOP);

        if (curls) {
            float stepZ = frontZ + MagicCarpetPose.SEGMENT_LENGTH;
            float step = height(heights, MagicCarpetPose.SEGMENT_LENGTH);
            Box second = slab(1, false);
            cap(buffer, pose, light, overlay, color, frontSlab, Side.SOUTH, stepZ, step, -CUT_HALF_WIDTH, CUT_HALF_WIDTH, BOTTOM, SLAB_TOP);
            cap(buffer, pose, light, overlay, color, second, Side.NORTH, stepZ, step, -HALF_WIDTH, -CUT_HALF_WIDTH, BOTTOM, SLAB_TOP);
            cap(buffer, pose, light, overlay, color, second, Side.NORTH, stepZ, step, CUT_HALF_WIDTH, HALF_WIDTH, BOTTOM, SLAB_TOP);
            cap(buffer, pose, light, overlay, color, sideBorder(1, -HALF_WIDTH), Side.NORTH, stepZ, step, -HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
            cap(buffer, pose, light, overlay, color, sideBorder(1, INNER), Side.NORTH, stepZ, step, INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            float borderEnd = height(heights, FRONT_BORDER_END);
            wall(buffer, pose, light, overlay, color, frontBorder, Side.WEST, frontZ, frontZ + FRONT_BORDER_END, front, borderEnd, -CUT_HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            wall(buffer, pose, light, overlay, color, frontBorder, Side.EAST, frontZ, frontZ + FRONT_BORDER_END, front, borderEnd, CUT_HALF_WIDTH, SLAB_TOP, BORDER_TOP);
            return;
        }

        cap(buffer, pose, light, overlay, color, sideBorder(0, -HALF_WIDTH), Side.NORTH, frontZ, front, -HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, sideBorder(0, INNER), Side.NORTH, frontZ, front, INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, sideBorder(MagicCarpetPose.SEGMENTS - 1, -HALF_WIDTH), Side.SOUTH, backZ, back, -HALF_WIDTH, -INNER, SLAB_TOP, BORDER_TOP);
        cap(buffer, pose, light, overlay, color, sideBorder(MagicCarpetPose.SEGMENTS - 1, INNER), Side.SOUTH, backZ, back, INNER, HALF_WIDTH, SLAB_TOP, BORDER_TOP);
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
                ? new Box(56.0F, 40.0F, 16.0F, 1.0F, 4.0F, -CUT_HALF_WIDTH, BOTTOM, MagicCarpetPose.FRONT_Z)
                : new Box(0.0F, 5.0F * segment, 24.0F, 1.0F, 4.0F, -HALF_WIDTH, BOTTOM, MagicCarpetPose.FRONT_Z + MagicCarpetPose.SEGMENT_LENGTH * segment);
    }

    private static Box sideBorder(int segment, float minX) {
        return new Box(56.0F, 5.0F * segment, 2.0F, 1.0F, 4.0F, minX, SLAB_TOP, MagicCarpetPose.FRONT_Z + MagicCarpetPose.SEGMENT_LENGTH * segment);
    }

    private static Box frontBorder(boolean cut) {
        return cut
                ? new Box(0.0F, 43.0F, 16.0F, 1.0F, 2.0F, -CUT_HALF_WIDTH, SLAB_TOP, MagicCarpetPose.FRONT_Z)
                : new Box(0.0F, 40.0F, 20.0F, 1.0F, 2.0F, -INNER, SLAB_TOP, MagicCarpetPose.FRONT_Z);
    }

    private static Box backBorder() {
        return new Box(0.0F, 40.0F, 20.0F, 1.0F, 2.0F, -INNER, SLAB_TOP, MagicCarpetPose.FRONT_Z + BACK_BORDER_START);
    }

    private static void horizontal(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int light,
            int overlay,
            int color,
            Box box,
            Side side,
            float zFrom,
            float zTo,
            float hFrom,
            float hTo,
            float x0,
            float x1,
            float y) {
        float dz = zTo - zFrom;
        float dh = hTo - hFrom;
        float length = (float) Math.sqrt(dz * dz + dh * dh);
        float sign = side == Side.UP ? 1.0F : -1.0F;
        float ny = sign * dz / length;
        float nz = -sign * dh / length;
        if (side == Side.UP) {
            vertex(buffer, pose, light, overlay, color, box, side, x1, y, zFrom, hFrom, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y, zFrom, hFrom, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y, zTo, hTo, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x1, y, zTo, hTo, 0.0F, ny, nz);
        } else {
            vertex(buffer, pose, light, overlay, color, box, side, x1, y, zTo, hTo, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y, zTo, hTo, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y, zFrom, hFrom, 0.0F, ny, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x1, y, zFrom, hFrom, 0.0F, ny, nz);
        }
    }

    private static void wall(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int light,
            int overlay,
            int color,
            Box box,
            Side side,
            float zFrom,
            float zTo,
            float hFrom,
            float hTo,
            float x,
            float y0,
            float y1) {
        float nx = side == Side.WEST ? -1.0F : 1.0F;
        if (side == Side.WEST) {
            vertex(buffer, pose, light, overlay, color, box, side, x, y0, zFrom, hFrom, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y0, zTo, hTo, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y1, zTo, hTo, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y1, zFrom, hFrom, nx, 0.0F, 0.0F);
        } else {
            vertex(buffer, pose, light, overlay, color, box, side, x, y0, zTo, hTo, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y0, zFrom, hFrom, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y1, zFrom, hFrom, nx, 0.0F, 0.0F);
            vertex(buffer, pose, light, overlay, color, box, side, x, y1, zTo, hTo, nx, 0.0F, 0.0F);
        }
    }

    private static void cap(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int light,
            int overlay,
            int color,
            Box box,
            Side side,
            float z,
            float height,
            float x0,
            float x1,
            float y0,
            float y1) {
        float nz = side == Side.NORTH ? -1.0F : 1.0F;
        if (side == Side.NORTH) {
            vertex(buffer, pose, light, overlay, color, box, side, x1, y0, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y0, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y1, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x1, y1, z, height, 0.0F, 0.0F, nz);
        } else {
            vertex(buffer, pose, light, overlay, color, box, side, x0, y0, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x1, y0, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x1, y1, z, height, 0.0F, 0.0F, nz);
            vertex(buffer, pose, light, overlay, color, box, side, x0, y1, z, height, 0.0F, 0.0F, nz);
        }
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            int light,
            int overlay,
            int color,
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
        buffer.addVertex(pose, x / 16.0F, (height + y) / 16.0F, z / 16.0F)
                .setColor(color)
                .setUv(
                        box.u(side, x, localZ) / TEXTURE_WIDTH,
                        box.v(side, y, localZ) / TEXTURE_HEIGHT)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
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
     * One of the boxes the body used to be built from. Its UVs are the ones vanilla gives that
     * box's faces (ModelPart.Cube, 26.3), so the texture is unchanged; only the corners move, onto
     * the shared rows.
     */
    private record Box(
            float texU,
            float texV,
            float width,
            float height,
            float depth,
            float minX,
            float minY,
            float minZ) {
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
