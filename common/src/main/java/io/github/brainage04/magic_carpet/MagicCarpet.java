package io.github.brainage04.magic_carpet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MagicCarpet {
    public static final String MOD_ID = "magic_carpet";
    public static final String MOD_NAME = "MagicCarpet";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void initialize() {
        LOGGER.info("{} initialising...", MOD_NAME);
        LOGGER.info("{} initialised.", MOD_NAME);
    }
}
