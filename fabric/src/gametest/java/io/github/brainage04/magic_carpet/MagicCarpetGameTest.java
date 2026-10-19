package io.github.brainage04.magic_carpet;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class MagicCarpetGameTest {
    @GameTest
    public void sustainedMovement(GameTestHelper helper) {
        MagicCarpetGameTests.sustainedMovement(helper);
    }

    @GameTest
    public void passengersAndFallDamage(GameTestHelper helper) {
        MagicCarpetGameTests.passengersAndFallDamage(helper);
    }

    @GameTest
    public void collision(GameTestHelper helper) {
        MagicCarpetGameTests.collision(helper);
    }
}
