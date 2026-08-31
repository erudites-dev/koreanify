package dev.erudites.mods.koreanify.client.config;

import java.nio.file.Path;

public class KoreanifyConfig extends JsonConfig<KoreanifyConfig> {

    private static KoreanifyConfig INSTANCE = new KoreanifyConfig();

    @Override
    protected String fileName() {
        return "koreanify.json5";
    }

    public static void initialize(final Path configDir) {
        INSTANCE = INSTANCE.setup(configDir);
    }

    public static void save() {
        INSTANCE.saveConfig();
    }

    public static KoreanifyConfig get() {
        return INSTANCE;
    }

    public CommandConfig command = new CommandConfig();
    public InputConfig input = new InputConfig();
    public SearchConfig search = new SearchConfig();

    @Category("명령어 관련 설정 카테고리")
    public static class CommandConfig {
        @Comment({
            "커맨드 탭 자동완성에서 한글 입력 전용 모드",
            "true (기본값): 입력에 한글이 포함된 경우에만 한글/초성 매칭을 적용합니다.",
            "false: 영문 입력에도 substring 매칭을 적용합니다. (/mode → /gamemode 등)"
        })
        public boolean commandSearchKoreanOnly = true;
    }

    @Category("검색 관련 설정 카테고리")
    public static class SearchConfig {
        @Comment({
            "영문 자판 입력을 한글로 해석해 검색 (ekdl → 다이아)",
            "true (기본값): IME를 켜지 않고 입력한 영문도 해당 자판의 한글로 해석해 함께 검색합니다.",
            "false: 영문 입력은 영문 그대로만 검색합니다."
        })
        public boolean latinAsHangulSearch = true;

        @Comment({
            "한글 입력을 영문 자판으로 해석해 검색 (ㅏㄷ데 → keep)",
            "true (기본값): IME를 끄지 않고 입력한 한글도 해당 자판의 영문으로 해석해 함께 검색합니다.",
            "false: 한글 입력은 한글 그대로만 검색합니다."
        })
        public boolean hangulAsLatinSearch = true;
    }

    @Category("입력 관련 설정 카테고리")
    public static class InputConfig {
        @Comment({
            "Windows IME 전각 전환 방지",
            "true (기본값): Windows에서 IME가 전각 모드로 전환되면 즉시 반각 모드로 되돌립니다.",
            "false: 운영체제/IME의 전각/반각 전환을 그대로 둡니다."
        })
        public boolean preventWindowsFullwidthSwitching = true;
    }
}
