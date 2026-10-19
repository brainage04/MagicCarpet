package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.entity.custom.MagicCarpetEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Loader-neutral server GameTest bodies. Both loaders compile this source set into their GameTest
 * mods: Fabric runs them through {@code @GameTest} methods, NeoForge through registered test
 * functions and {@code test_instance} data.
 */
public final class MagicCarpetGameTests {
    private MagicCarpetGameTests() {
    }

    public static void sustainedMovement(GameTestHelper helper) {
        ServerPlayer rider = helper.makeMockServerPlayerInLevel();
        try {
            EntityType<?>[] types = {ModEntities.basic(), ModEntities.advanced(), ModEntities.legendary()};
            double[] speeds = {0.6, 1.2, 2.4};
            for (int tier = 0; tier < types.length; tier++) {
                MagicCarpetEntity carpet = (MagicCarpetEntity) types[tier].create(helper.getLevel(), EntitySpawnReason.COMMAND);
                Vec3 origin = helper.absoluteVec(new Vec3(3, 16, 3));
                carpet.setPos(origin);
                assertTrue(rider.startRiding(carpet), "Rider could not mount " + types[tier]);
                rider.setYRot(0);
                rider.setXRot(0);
                rider.setLastClientInput(new Input(true, false, false, false, false, false, false));
                double distance = 0;
                for (int tick = 0; tick < 160; tick++) {
                    // Keep the real collision/movement path inside an isolated, empty test column.
                    carpet.setPos(origin);
                    carpet.tick();
                    if (tick >= 140) distance += carpet.getZ() - origin.z;
                }
                assertNear(speeds[tier] * 20, distance, 0.02, "Sustained horizontal blocks per second");
                rider.setLastClientInput(new Input(true, false, true, false, true, false, false));
                for (int tick = 0; tick < 100; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(speeds[tier], carpet.getDeltaMovement().horizontalDistance(), 0.002, "Diagonal input must not boost speed");
                assertNear(speeds[tier] / 2, carpet.getDeltaMovement().y, 0.002, "Climb speed");
                rider.setXRot(60);
                for (int tick = 0; tick < 100; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(-speeds[tier] / 2, carpet.getDeltaMovement().y, 0.002, "Descent speed");
                rider.stopRiding();
                for (int tick = 0; tick < 200; tick++) {
                    carpet.setPos(origin);
                    carpet.tick();
                }
                assertNear(0, carpet.getDeltaMovement().length(), 0, "Dismounted carpet must coast to a complete stop");
                carpet.discard();
            }
        } finally {
            rider.stopRiding();
            helper.getLevel().getServer().getPlayerList().remove(rider);
            rider.discard();
        }
        helper.succeed();
    }

    public static void passengersAndFallDamage(GameTestHelper helper) {
        MagicCarpetEntity carpet = ModEntities.basic().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Sheep first = helper.spawn(EntityTypes.SHEEP, new BlockPos(2, 2, 2));
        Sheep second = helper.spawn(EntityTypes.SHEEP, new BlockPos(4, 2, 2));
        Sheep third = helper.spawn(EntityTypes.SHEEP, new BlockPos(6, 2, 2));
        carpet.setPos(first.position());
        assertTrue(first.startRiding(carpet), "First passenger must mount");
        assertTrue(second.startRiding(carpet), "Second passenger must mount");
        assertTrue(!third.startRiding(carpet), "A third passenger must be rejected");
        assertTrue(carpet.getControllingPassenger() == first, "First passenger must control the carpet");
        float health = first.getHealth();
        first.hurtServer(helper.getLevel(), first.damageSources().fall(), 5);
        assertNear(health, first.getHealth(), 0, "Mounted passenger must not take fall damage");
        first.hurtServer(helper.getLevel(), first.damageSources().generic(), 2);
        assertNear(health - 2, first.getHealth(), 0, "Other damage must still apply");
        first.invulnerableTime = 0;
        first.stopRiding();
        assertTrue(carpet.getControllingPassenger() == second, "Remaining passenger must take over");
        first.hurtServer(helper.getLevel(), first.damageSources().fall(), 5);
        assertNear(health - 7, first.getHealth(), 0, "Dismounted passenger must take fall damage");
        carpet.ejectPassengers();
        carpet.discard();
        helper.succeed();
    }

    public static void collision(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        MagicCarpetEntity carpet = ModEntities.legendary().create(helper.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 start = helper.absoluteVec(new Vec3(4.5, 2, 1.5));
        carpet.setPos(start);
        carpet.setDeltaMovement(0, 0, 2.4);
        carpet.tick();
        assertTrue(carpet.getBoundingBox().maxZ <= helper.absolutePos(wall).getZ() + 0.0001,
                "Fast carpet must not pass through a wall");
        assertTrue(carpet.horizontalCollision, "Wall contact must register as a collision");
        assertNear(0, carpet.getDeltaMovement().z, 0, "Collision must cancel velocity into the wall");
        carpet.discard();
        helper.succeed();
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertNear(double expected, double actual, double tolerance, String message) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected " + expected + ", found " + actual);
        }
    }
}
