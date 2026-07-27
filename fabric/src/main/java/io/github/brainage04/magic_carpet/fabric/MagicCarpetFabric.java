package io.github.brainage04.magic_carpet.fabric;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.fabricmc.api.ModInitializer;

public final class MagicCarpetFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new FabricPlatform().register();
        MagicCarpet.initialize();
    }
}
