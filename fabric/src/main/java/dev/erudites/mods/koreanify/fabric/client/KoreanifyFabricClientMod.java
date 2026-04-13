package dev.erudites.mods.koreanify.fabric.client;

import dev.erudites.mods.koreanify.client.KoreanifyClientMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class KoreanifyFabricClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KoreanifyClientMod.initializeConfig(FabricLoader.getInstance().getConfigDir());
    }
}
