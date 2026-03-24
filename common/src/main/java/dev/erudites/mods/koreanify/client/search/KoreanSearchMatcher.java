package dev.erudites.mods.koreanify.client.search;

public final class KoreanSearchMatcher {

    private static final int HANGUL_BASE = 0xAC00;
    private static final int HANGUL_END = 0xD7A3;
    private static final int JUNGSEONG_COUNT = 21;
    private static final int JONGSEONG_COUNT = 28;
    private static final int SYLLABLE_BLOCK = JUNGSEONG_COUNT * JONGSEONG_COUNT; // 588

    private static final char[] CHOSEONG_TABLE = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();
    private static final char[] JUNGSEONG_TABLE = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();
    private static final char[] JONGSEONG_TABLE = "\0ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ".toCharArray();

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
    private record CachedQuery(String original, char[] normalized, int length, boolean allChoseong) {}

    private KoreanSearchMatcher() {}

    public static boolean isChoseong(char ch) {
        int index = ch - 'ㄱ';
        return index >= 0 && index < IS_CHOSEONG.length && IS_CHOSEONG[index];
    }

    // Reverse the syllable formula: choseong_index = (syllable - BASE) / 588
    public static char getChoseong(char ch) {
        if (ch >= HANGUL_BASE && ch <= HANGUL_END) {
            return CHOSEONG_TABLE[(ch - HANGUL_BASE) / SYLLABLE_BLOCK];
        }
        return ch;
    }

    public static boolean matches(String target, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        if (target == null || target.isEmpty()) {
            return false;
        }
        char[] queryBuf;
        int queryLen;
        boolean allChoseong;
        CachedQuery cached = cachedQuery;
        if (cached != null && query.equals(cached.original)) {
            queryBuf = cached.normalized;
            queryLen = cached.length;
            allChoseong = cached.allChoseong;
        } else {
            queryBuf = new char[query.length()];
            queryLen = normalizeInto(query, queryBuf);
            allChoseong = checkAllChoseong(queryBuf, queryLen);
            cachedQuery = new CachedQuery(query, queryBuf, queryLen, allChoseong);
        }
        if (queryLen == 0) {
            return true;
        }
        // Normalize into raw char[] — avoids String allocation from toLowerCase()/replace()
        char[] targetBuf = new char[target.length()];
        int targetLen = normalizeInto(target, targetBuf);
        if (queryLen > targetLen) {
            return false;
        }
        // Pure choseong queries use a dedicated loop that skips the three-way
        // branching in mismatchChar — just getChoseong() + equality.
        return allChoseong
            ? matchesChoseongOnly(targetBuf, targetLen, queryBuf, queryLen)
            : matchesGeneral(targetBuf, targetLen, queryBuf, queryLen);
    }

    // Decomposes 한글 → ㅎㅏㄴㄱㅡㄹ
    public static String toJamo(String text) {
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

    private static boolean matchesChoseongOnly(char[] targetBuf, int targetLen, char[] queryBuf, int queryLen) {
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

    private static boolean matchesGeneral(char[] targetBuf, int targetLen, char[] queryBuf, int queryLen) {
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

    private static boolean mismatchChar(char targetChar, char queryChar) {
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
    private static int normalizeInto(String source, char[] buf) {
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

    private static boolean checkAllChoseong(char[] buf, int len) {
        for (int i = 0; i < len; i++) {
            if (!isChoseong(buf[i])) {
                return false;
            }
        }
        return len > 0;
    }
}
