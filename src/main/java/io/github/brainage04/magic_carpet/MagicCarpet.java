package io.github.brainage04.magic_carpet;

import io.github.brainage04.magic_carpet.entity.ModEntities;
import io.github.brainage04.magic_carpet.item.ModItems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MagicCarpet {
	public static final String MOD_ID = "magic_carpet";
	public static final String MOD_NAME = "MagicCarpet";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void initialize() {
		LOGGER.info("{} initialising...", MOD_NAME);

		ModEntities.initialize();
		ModItems.initialize();

		LOGGER.info("{} initialised.", MOD_NAME);
	}
}