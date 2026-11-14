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

    @GameTest
    public void tierData(GameTestHelper helper) {
        MagicCarpetGameTests.tierData(helper);
    }

    @GameTest
    public void crafting(GameTestHelper helper) {
        MagicCarpetGameTests.crafting(helper);
    }

    @GameTest
    public void placement(GameTestHelper helper) {
        MagicCarpetGameTests.placement(helper);
    }

    @GameTest
    public void persistenceAndDrops(GameTestHelper helper) {
        MagicCarpetGameTests.persistenceAndDrops(helper);
    }

    @GameTest
    public void playerTakesControl(GameTestHelper helper) {
        MagicCarpetGameTests.playerTakesControl(helper);
    }

    @GameTest
    public void mountRequiresAPlainUse(GameTestHelper helper) {
        MagicCarpetGameTests.mountRequiresAPlainUse(helper);
    }

    @GameTest
    public void releasedKeysCoastToAStop(GameTestHelper helper) {
        MagicCarpetGameTests.releasedKeysCoastToAStop(helper);
    }

    @GameTest(skyAccess = true)
    public void survivalRiderLandsWithoutFallDamage(GameTestHelper helper) {
        MagicCarpetGameTests.survivalRiderLandsWithoutFallDamage(helper);
    }

    @GameTest(skyAccess = true)
    public void midAirDismountTakesFallDamage(GameTestHelper helper) {
        MagicCarpetGameTests.midAirDismountTakesFallDamage(helper);
    }

    @GameTest(skyAccess = true)
    public void landingAddsNoDistanceFallenStatistic(GameTestHelper helper) {
        MagicCarpetGameTests.landingAddsNoDistanceFallenStatistic(helper);
    }

    @GameTest(skyAccess = true)
    public void landingPlaysNoFallSound(GameTestHelper helper) {
        MagicCarpetGameTests.landingPlaysNoFallSound(helper);
    }
}
