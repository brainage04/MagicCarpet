package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;

public class MagicCarpetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ModEntities.initialize();
	}
}