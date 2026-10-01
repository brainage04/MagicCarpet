package io.github.brainage04.magic_carpet;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class MagicCarpetGameTest {
    @GameTest
    public void registrations(GameTestHelper helper) {
        MagicCarpetGameTests.registrations(helper);
    }
}
