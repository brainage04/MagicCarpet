package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;

import static com.mojang.text2speech.Narrator.LOGGER;
import static io.github.brainage04.magic_carpet.MagicCarpet.MOD_NAME;

public class MagicCarpetClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		LOGGER.info("{} initialising client...", MOD_NAME);

		ModEntities.initialize();

		LOGGER.info("{} initialised client.", MOD_NAME);
	}
}