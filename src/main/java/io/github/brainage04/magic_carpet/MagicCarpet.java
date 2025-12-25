package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.item.ModItemGroups;
import io.github.brainage04.magic_carpet.item.ModItems;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicCarpet implements ModInitializer {
	public static final String MOD_ID = "magic_carpet";
	public static final String MOD_NAME = "MagicCarpet";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("{} initialising server...", MOD_NAME);

		ModItems.initialize();
		ModItemGroups.initialize();

		LOGGER.info("{} initialised server.", MOD_NAME);
	}
}