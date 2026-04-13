package dev.erudites.mods.koreanify.neoforge.client;

import dev.erudites.mods.koreanify.client.KoreanifyClientMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(value = KoreanifyClientMod.MODID, dist = Dist.CLIENT)
public class KoreanifyNeoForgeClientMod {

    public KoreanifyNeoForgeClientMod() {
        KoreanifyClientMod.initializeConfig(FMLPaths.CONFIGDIR.get());
    }
}
