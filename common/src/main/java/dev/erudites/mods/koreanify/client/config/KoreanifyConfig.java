package dev.erudites.mods.koreanify.client.config;

import java.nio.file.Path;

public class KoreanifyConfig extends JsonConfig<KoreanifyConfig> {

    public static KoreanifyConfig INSTANCE = new KoreanifyConfig();

    @Override
    protected String fileName() {
        return "koreanify.json5";
    }

    public static void initialize(Path configDir) {
        INSTANCE = INSTANCE.loadFrom(configDir);
    }

    public CommandConfig command = new CommandConfig();

    public static class CommandConfig {
        @Comment({
            "커맨드 탭 자동완성에서 한글 입력 전용 모드",
            "true (기본값): 입력에 한글이 포함된 경우에만 한글/초성 매칭을 적용합니다.",
            "false: 영문 입력에도 substring 매칭을 적용합니다. (/mode → /gamemode 등)"
        })
        public boolean commandSearchKoreanOnly = true;
    }
}
