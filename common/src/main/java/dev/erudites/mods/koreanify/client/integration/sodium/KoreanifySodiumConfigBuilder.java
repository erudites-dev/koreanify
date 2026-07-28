package dev.erudites.mods.koreanify.client.integration.sodium;

import dev.erudites.mods.koreanify.client.KoreanifyClientMod;
import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.caffeinemc.mods.sodium.api.config.structure.ModOptionsBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

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
                    value -> KoreanifyConfig.get().command.commandSearchKoreanOnly = value,
                    () -> KoreanifyConfig.get().command.commandSearchKoreanOnly
                )
                .setStorageHandler(KoreanifyConfig::save)
            )
        );
        options.addPage(builder.createOptionPage()
            .setName(Component.translatable("koreanify.config.page.search"))
            .addOption(builder.createBooleanOption(KoreanifyClientMod.id("latin_as_hangul_search"))
                .setName(Component.translatable("koreanify.config.option.latin_as_hangul_search"))
                .setTooltip(Component.translatable("koreanify.config.option.latin_as_hangul_search.tooltip"))
                .setDefaultValue(true)
                .setBinding(
                    value -> KoreanifyConfig.get().search.latinAsHangulSearch = value,
                    () -> KoreanifyConfig.get().search.latinAsHangulSearch
                )
                .setStorageHandler(KoreanifyConfig::save)
            )
        );
        options.addPage(builder.createOptionPage()
            .setName(Component.translatable("koreanify.config.page.input"))
            .addOption(builder.createBooleanOption(KoreanifyClientMod.id("prevent_windows_fullwidth_switching"))
                .setName(Component.translatable("koreanify.config.option.prevent_windows_fullwidth_switching"))
                .setTooltip(Component.translatable("koreanify.config.option.prevent_windows_fullwidth_switching.tooltip"))
                .setEnabled(Util.getPlatform() == Util.OS.WINDOWS)
                .setControlHiddenWhenDisabled(false)
                .setDefaultValue(true)
                .setBinding(
                    value -> KoreanifyConfig.get().input.preventWindowsFullwidthSwitching = value,
                    () -> KoreanifyConfig.get().input.preventWindowsFullwidthSwitching
                )
                .setStorageHandler(KoreanifyConfig::save)
            )
        );
    }
}
