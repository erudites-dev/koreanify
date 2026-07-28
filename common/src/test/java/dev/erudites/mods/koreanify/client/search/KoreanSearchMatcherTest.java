package dev.erudites.mods.koreanify.client.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KoreanSearchMatcherTest {

    @Test
    @DisplayName("choseong queries match the syllables they start")
    void matchesChoseong() {
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "ㄷㅇㅇ"));
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "ㅇㅁㄷ"));
        assertFalse(KoreanSearchMatcher.matches("다이아몬드", "ㄷㅁ"));
    }

    @Test
    @DisplayName("partly typed syllables match, including a trailing consonant")
    void matchesPartialSyllable() {
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "다이아"));
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "몬드"));
        assertTrue(KoreanSearchMatcher.matches("금 사과", "금ㅅ"));
    }

    @Test
    @DisplayName("latin queries typed with the ime off still match")
    void matchesKeyboardLayoutFallback() {
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "ekdldkahsem"));
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", "ekdl"));
        assertTrue(KoreanSearchMatcher.matches("과일", "rhk"));
        assertTrue(KoreanSearchMatcher.matches("앉다", "dkswek"));
        assertFalse(KoreanSearchMatcher.matches("다이아몬드", "rksk"));
    }

    @Test
    @DisplayName("plain latin queries keep matching latin names")
    void matchesLatinNames() {
        assertTrue(KoreanSearchMatcher.matches("Diamond Sword", "diamond"));
        assertTrue(KoreanSearchMatcher.matches("Diamond Sword", "DIAMOND"));
        assertFalse(KoreanSearchMatcher.matches("Diamond Sword", "emerald"));
    }

    @Test
    @DisplayName("spaces are ignored on both sides")
    void ignoresSpaces() {
        assertTrue(KoreanSearchMatcher.matches("금 사과", "금사과"));
        assertTrue(KoreanSearchMatcher.matches("금사과", "금 사과"));
    }

    @Test
    @DisplayName("empty query matches, empty target does not")
    void handlesEmptyInput() {
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", ""));
        assertTrue(KoreanSearchMatcher.matches("다이아몬드", null));
        assertFalse(KoreanSearchMatcher.matches("", "다"));
        assertFalse(KoreanSearchMatcher.matches(null, "다"));
    }

    @Test
    @DisplayName("decomposes syllables into the jamo a keyboard produces")
    void decomposesToJamo() {
        assertTrue(KoreanSearchMatcher.toJamo("한글").startsWith("ㅎㅏㄴ"));
        assertTrue(KoreanSearchMatcher.isChoseong('ㄱ'));
        assertFalse(KoreanSearchMatcher.isChoseong('ㅏ'));
    }
}
