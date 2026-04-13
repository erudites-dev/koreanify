package dev.erudites.mods.koreanify.client.integration.sodium;

import dev.erudites.mods.koreanify.client.KoreanifyClientMod;
import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.network.chat.Component;

public class KoreanifySodiumConfigBuilder implements ConfigEntryPoint {

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        ModOptionsBuilder options = builder.registerModOptions(KoreanifyClientMod.MODID);
        options.setIcon(KoreanifyClientMod.id("config-icon.png"));
        options.addPage(builder.createOptionPage()
            .setName(Component.translatable("koreanify.config.page.commands"))
            .addOption(builder.createBooleanOption(KoreanifyClientMod.id("command_search_korean_only"))
                .setName(Component.translatable("koreanify.config.option.command_search_korean_only"))
                .setTooltip(Component.translatable("koreanify.config.option.command_search_korean_only.tooltip"))
                .setDefaultValue(true)
                .setBinding(
                    value -> KoreanifyConfig.INSTANCE.command.commandSearchKoreanOnly = value,
                    () -> KoreanifyConfig.INSTANCE.command.commandSearchKoreanOnly
                )
                .setStorageHandler(KoreanifyConfig::save)
            )
        );
    }
}
