package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntityRenderers;
import net.fabricmc.api.ClientModInitializer;

public class MagicCarpetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MagicCarpet.LOGGER.info("{} initialising client...", MagicCarpet.MOD_NAME);

		ModEntityRenderers.initialize();

		MagicCarpet.LOGGER.info("{} initialised client.", MagicCarpet.MOD_NAME);
	}
}