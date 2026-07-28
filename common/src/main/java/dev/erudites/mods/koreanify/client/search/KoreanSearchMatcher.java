package dev.erudites.mods.koreanify.client.search;

import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import org.jspecify.annotations.Nullable;

public final class KoreanSearchMatcher {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int JUNGSEONG_COUNT = 21;
    private static final int JONGSEONG_COUNT = 28;
    private static final int SYLLABLE_BLOCK = JUNGSEONG_COUNT * JONGSEONG_COUNT; // 588

    private static final char[] CHOSEONG_TABLE = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
    private static final char[] JUNGSEONG_TABLE = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
    private static final char[] JONGSEONG_TABLE = "\0ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();

    // Compound jamo split into the keys that type them: ㅘ → ㅗㅏ, ㄵ → ㄴㅈ
    private static final String[] JUNGSEONG_PARTS = {
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅗㅏ", "ㅗㅐ", "ㅗㅣ", "ㅛ",
        "ㅜ", "ㅜㅓ", "ㅜㅔ", "ㅜㅣ", "ㅠ", "ㅡ", "ㅡㅣ", "ㅣ"
    };
    private static final String[] JONGSEONG_PARTS = {
        "", "ㄱ", "ㄲ", "ㄱㅅ", "ㄴ", "ㄴㅈ", "ㄴㅎ", "ㄷ", "ㄹ", "ㄹㄱ", "ㄹㅁ", "ㄹㅂ", "ㄹㅅ", "ㄹㅌ",
        "ㄹㅍ", "ㄹㅎ", "ㅁ", "ㅂ", "ㅂㅅ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final int MAX_JAMO_PER_CHAR = 5;

    private static final boolean[] IS_CHOSEONG;

    static {
        IS_CHOSEONG = new boolean['ㅎ' - 'ㄱ' + 1];
        for (char c : CHOSEONG_TABLE) {
            IS_CHOSEONG[c - 'ㄱ'] = true;
        }
    }

    @SuppressWarnings("java:S3077")
    private static volatile CachedQuery cachedQuery;

    @SuppressWarnings("java:S6218")
    private record CachedQuery(
        String original,
        char[] normalized,
        int length,
        boolean allChoseong,
        char[] layoutJamo,
        int layoutLength
    ) {}

    private KoreanSearchMatcher() {}

    public static boolean isChoseong(final char ch) {
        int index = ch - 'ㄱ';
        return index >= 0 && index < IS_CHOSEONG.length && IS_CHOSEONG[index];
    }

    // Reverse the syllable formula: choseong_index = (syllable - BASE) / 588
    public static char getChoseong(final char ch) {
        if (ch >= HANGUL_BASE && ch <= HANGUL_END) {
            return CHOSEONG_TABLE[(ch - HANGUL_BASE) / SYLLABLE_BLOCK];
        }
        return ch;
    }

    public static boolean matches(final @Nullable String target, final @Nullable String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (target == null || target.isEmpty()) {
            return false;
        }
        CachedQuery cached = cachedQuery;
        if (cached == null || !query.equals(cached.original)) {
            cached = cacheQuery(query);
        }
        char[] queryBuf = cached.normalized;
        int queryLen = cached.length;
        if (queryLen == 0) {
            return true;
        }
        // Normalize into raw char[] — avoids String allocation from toLowerCase()/replace()
        char[] targetBuf = new char[target.length()];
        int targetLen = normalizeInto(target, targetBuf);
        if (queryLen <= targetLen) {
            // Pure choseong queries use a dedicated loop that skips the three-way
            // branching in mismatchChar — just getChoseong() + equality.
            boolean matched = cached.allChoseong
                ? matchesChoseongOnly(targetBuf, targetLen, queryBuf, queryLen)
                : matchesGeneral(targetBuf, targetLen, queryBuf, queryLen);
            if (matched) {
                return true;
            }
        }
        return matchesKeyboardLayout(targetBuf, targetLen, cached);
    }

    private static CachedQuery cacheQuery(final String query) {
        char[] normalized = new char[query.length()];
        int length = normalizeInto(query, normalized);
        String layout = KoreanifyConfig.get().search.latinAsHangulSearch
            ? KoreanKeyboardLayout.toJamo(query)
            : "";
        char[] layoutJamo = new char[layout.length()];
        int layoutLength = normalizeInto(layout, layoutJamo);
        CachedQuery cached = new CachedQuery(
            query,
            normalized,
            length,
            checkAllChoseong(normalized, length),
            layoutJamo,
            layoutLength
        );
        cachedQuery = cached;
        return cached;
    }

    // Compares both sides as jamo, so the query typed with the ime off still matches: rksk → 가나
    private static boolean matchesKeyboardLayout(final char[] targetBuf, final int targetLen, final CachedQuery cached) {
        int layoutLen = cached.layoutLength;
        if (layoutLen == 0) {
            return false;
        }
        char[] jamoBuf = new char[targetLen * MAX_JAMO_PER_CHAR];
        int jamoLen = expandJamoInto(targetBuf, targetLen, jamoBuf);
        if (layoutLen > jamoLen) {
            return false;
        }
        char[] layoutJamo = cached.layoutJamo;
        int limit = jamoLen - layoutLen;
        for (int i = 0; i <= limit; i++) {
            boolean found = true;
            for (int j = 0; j < layoutLen; j++) {
                if (jamoBuf[i + j] != layoutJamo[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    // Decomposes down to the keys that type each syllable: 과 → ㄱㅗㅏ, 앉 → ㅇㅏㄴㅈ
    private static int expandJamoInto(final char[] source, final int length, final char[] target) {
        int written = 0;
        for (int i = 0; i < length; i++) {
            char ch = source[i];
            if (ch < HANGUL_BASE || ch > HANGUL_END) {
                target[written++] = ch;
                continue;
            }
            int offset = ch - HANGUL_BASE;
            target[written++] = CHOSEONG_TABLE[offset / SYLLABLE_BLOCK];
            written = appendParts(target, written, JUNGSEONG_PARTS[(offset % SYLLABLE_BLOCK) / JONGSEONG_COUNT]);
            written = appendParts(target, written, JONGSEONG_PARTS[offset % JONGSEONG_COUNT]);
        }
        return written;
    }

    private static int appendParts(final char[] target, final int written, final String parts) {
        parts.getChars(0, parts.length(), target, written);
        return written + parts.length();
    }

    // Decomposes 한글 → ㅎㅏㄴㄱㅡㄹ
    public static String toJamo(final @Nullable String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length() * 3);
        for (int i = 0, n = text.length(); i < n; i++) {
            char ch = text.charAt(i);
            if (ch >= HANGUL_BASE && ch <= HANGUL_END) {
                int offset = ch - HANGUL_BASE;
                builder.append(CHOSEONG_TABLE[offset / SYLLABLE_BLOCK]);
                builder.append(JUNGSEONG_TABLE[(offset % SYLLABLE_BLOCK) / JONGSEONG_COUNT]);
                int jongseong = offset % JONGSEONG_COUNT;
                if (jongseong != 0) {
                    builder.append(JONGSEONG_TABLE[jongseong]);
                }
            } else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static boolean matchesChoseongOnly(final char[] targetBuf, final int targetLen, final char[] queryBuf, final int queryLen) {
        char queryFirst = queryBuf[0];
        int limit = targetLen - queryLen;
        for (int i = 0; i <= limit; i++) {
            if (getChoseong(targetBuf[i]) != queryFirst) {
                continue;
            }
            boolean found = true;
            for (int j = 1; j < queryLen; j++) {
                if (getChoseong(targetBuf[i + j]) != queryBuf[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesGeneral(final char[] targetBuf, final int targetLen, final char[] queryBuf, final int queryLen) {
        char queryFirst = queryBuf[0];
        int limit = targetLen - queryLen;
        for (int i = 0; i <= limit; i++) {
            if (mismatchChar(targetBuf[i], queryFirst)) {
                continue;
            }
            boolean found = true;
            for (int j = 1; j < queryLen; j++) {
                if (mismatchChar(targetBuf[i + j], queryBuf[j])) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return true;
            }
        }
        return false;
    }

    private static boolean mismatchChar(final char targetChar, final char queryChar) {
        if (targetChar == queryChar) {
            return false;
        }
        if (isChoseong(queryChar)) {
            return getChoseong(targetChar) != queryChar;
        }
        if (queryChar >= HANGUL_BASE && queryChar <= HANGUL_END && targetChar >= HANGUL_BASE && targetChar <= HANGUL_END) {
            int queryOffset = queryChar - HANGUL_BASE;
            if (queryOffset % JONGSEONG_COUNT == 0) {
                int targetOffset = targetChar - HANGUL_BASE;
                return queryOffset != targetOffset - (targetOffset % JONGSEONG_COUNT);
            }
        }
        return true;
    }

    // Strips spaces, lowercases ASCII in a single pass into pre-allocated buf.
    private static int normalizeInto(final String source, final char[] buf) {
        int len = 0;
        for (int i = 0, n = source.length(); i < n; i++) {
            char ch = source.charAt(i);
            if (ch == ' ') {
                continue;
            }
            if (ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch + 32);
            }
            buf[len++] = ch;
        }
        return len;
    }

    private static boolean checkAllChoseong(final char[] buf, final int len) {
        for (int i = 0; i < len; i++) {
            if (!isChoseong(buf[i])) {
                return false;
            }
        }
        return len > 0;
    }
}
