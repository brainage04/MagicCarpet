package io.github.brainage04.magic_carpet.neoforge;

import io.github.brainage04.magic_carpet.MagicCarpet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(MagicCarpet.MOD_ID)
public final class MagicCarpetNeoForge {
    public MagicCarpetNeoForge(IEventBus modBus) {
        NeoForgePlatform.register(modBus);
        MagicCarpet.initialize();
    }
}
