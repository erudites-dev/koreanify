package dev.erudites.mods.koreanify.client.search;

import org.jspecify.annotations.Nullable;

public final class KoreanKeyboardLayout {

    private static final int ASCII_RANGE = 128;
    private static final int JAMO_BASE = 0x3131;
    private static final int JAMO_END = 0x3163;
    private static final char[] DUBEOLSIK = new char[ASCII_RANGE];
    private static final char[] REVERSE_DUBEOLSIK = new char[JAMO_END - JAMO_BASE + 1];

    static {
        mapKeys("qwertyuiop", "ㅂㅈㄷㄱㅅㅛㅕㅑㅐㅔ");
        mapKeys("asdfghjkl", "ㅁㄴㅇㄹㅎㅗㅓㅏㅣ");
        mapKeys("zxcvbnm", "ㅋㅌㅊㅍㅠㅜㅡ");
        for (char key = 'a'; key <= 'z'; key++) {
            DUBEOLSIK[Character.toUpperCase(key)] = DUBEOLSIK[key];
        }
        mapKeys("QWERTOP", "ㅃㅉㄸㄲㅆㅒㅖ");
        for (char key = 0; key < ASCII_RANGE; key++) {
            char jamo = DUBEOLSIK[key];
            if (jamo != 0) {
                REVERSE_DUBEOLSIK[jamo - JAMO_BASE] = Character.toLowerCase(key);
            }
        }
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

    // ㄱㅏㄴㅏ → rksk, empty unless the query is single key jamo and spaces only — callers split syllables first.
    public static String toLatin(final @Nullable String jamo) {
        if (jamo == null || jamo.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(jamo.length());
        boolean converted = false;
        for (int i = 0, n = jamo.length(); i < n; i++) {
            char ch = jamo.charAt(i);
            if (ch == ' ') {
                builder.append(ch);
                continue;
            }
            int index = ch - JAMO_BASE;
            char key = index >= 0 && index < REVERSE_DUBEOLSIK.length ? REVERSE_DUBEOLSIK[index] : 0;
            if (key == 0) {
                return "";
            }
            builder.append(key);
            converted = true;
        }
        return converted ? builder.toString() : "";
    }
}
