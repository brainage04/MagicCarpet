package io.github.brainage04.magic_carpet.platform;

import java.util.ServiceLoader;

public interface Platform {
    void register();

    static void initialize() {
        ServiceLoader.load(Platform.class).findFirst()
                .orElseThrow(() -> new IllegalStateException("No MagicCarpet platform adapter found"))
                .register();
    }
}
