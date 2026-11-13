package io.github.brainage04.magic_carpet.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagicCarpetFlightSoundTest {
    private static final Object CARPET = new Object();
    private static final Object OTHER_CARPET = new Object();

    @Test
    void aLoopStillPlayingForTheSameCarpetKeepsIt() {
        assertFalse(MagicCarpetFlightSound.shouldStartLoop(CARPET, CARPET, true));
    }

    @Test
    void aLoopTheSoundEngineDroppedIsStartedAgain() {
        assertTrue(MagicCarpetFlightSound.shouldStartLoop(CARPET, CARPET, false));
    }

    @Test
    void anotherCarpetGetsItsOwnLoop() {
        assertTrue(MagicCarpetFlightSound.shouldStartLoop(OTHER_CARPET, CARPET, true));
    }
}
