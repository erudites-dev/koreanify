package dev.erudites.mods.koreanify.client;

import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public final class KoreanifyClientMod {

    public static final String MODID = "koreanify";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    private KoreanifyClientMod() {}

    public static void initializeConfig(Path configDir) {
        KoreanifyConfig.initialize(configDir);
    }
}
