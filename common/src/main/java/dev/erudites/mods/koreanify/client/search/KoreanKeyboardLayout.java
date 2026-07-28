package dev.erudites.mods.koreanify.client.search;

import org.jspecify.annotations.Nullable;

public final class KoreanKeyboardLayout {

    private static final int ASCII_RANGE = 128;
    private static final char[] DUBEOLSIK = new char[ASCII_RANGE];

    static {
        mapKeys("qwertyuiop", "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ");
        mapKeys("asdfghjkl", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ");
        mapKeys("zxcvbnm", "ㅋㅌㅊㅍㅠㅜㅡ");
        for (char key = 'a'; key <= 'z'; key++) {
            DUBEOLSIK[Character.toUpperCase(key)] = DUBEOLSIK[key];
        }
        mapKeys("QWERTOP", "ㅃㅉㄸㄲㅆㅒㅖ");
    }

    private KoreanKeyboardLayout() {}

    private static void mapKeys(final String keys, final String jamo) {
        for (int i = 0; i < keys.length(); i++) {
            DUBEOLSIK[keys.charAt(i)] = jamo.charAt(i);
        }
    }

    // rksk → ㄱㅏㄴㅏ, empty unless the query is latin letters and spaces only.
    public static String toJamo(final @Nullable String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(query.length());
        boolean converted = false;
        for (int i = 0, n = query.length(); i < n; i++) {
            char ch = query.charAt(i);
            if (ch == ' ') {
                builder.append(ch);
                continue;
            }
            char jamo = ch < ASCII_RANGE ? DUBEOLSIK[ch] : 0;
            if (jamo == 0) {
                return "";
            }
            builder.append(jamo);
            converted = true;
        }
        return converted ? builder.toString() : "";
    }
}
