package dev.erudites.mods.koreanify.client.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KoreanKeyboardLayoutTest {

    @Test
    @DisplayName("latin keys are read as the jamo they would have typed")
    void convertsLatinKeysToJamo() {
        assertEquals("ㄱㅏㄴㅏ", KoreanKeyboardLayout.toJamo("rksk"));
        assertEquals("ㄷㅏㅇㅣㅇㅏㅁㅗㄴㄷㅡ", KoreanKeyboardLayout.toJamo("ekdldkahsem"));
    }

    @Test
    @DisplayName("shifted keys map to double jamo, other uppercase keeps its jamo")
    void convertsShiftedKeys() {
        assertEquals("ㄲㅏ", KoreanKeyboardLayout.toJamo("Rk"));
        assertEquals("ㅃㅒ", KoreanKeyboardLayout.toJamo("QO"));
        assertEquals("ㄱㅏ", KoreanKeyboardLayout.toJamo("rk"));
        assertEquals("ㅁㅏ", KoreanKeyboardLayout.toJamo("Ak"));
    }

    @Test
    @DisplayName("spaces are kept so multi word queries still line up")
    void keepsSpaces() {
        assertEquals("ㄱㅏ ㄴㅏ", KoreanKeyboardLayout.toJamo("rk sk"));
    }

    @Test
    @DisplayName("jamo are read back as the latin keys that typed them")
    void convertsJamoToLatinKeys() {
        assertEquals("rksk", KoreanKeyboardLayout.toLatin("ㄱㅏㄴㅏ"));
        assertEquals("keep", KoreanKeyboardLayout.toLatin("ㅏㄷㄷㅔ"));
        assertEquals("rk sk", KoreanKeyboardLayout.toLatin("ㄱㅏ ㄴㅏ"));
    }

    @Test
    @DisplayName("double jamo read back as the unshifted key, so lowercase targets still line up")
    void convertsDoubleJamoToLatinKeys() {
        assertEquals("rk", KoreanKeyboardLayout.toLatin("ㄲㅏ"));
        assertEquals("qo", KoreanKeyboardLayout.toLatin("ㅃㅒ"));
    }

    @Test
    @DisplayName("syllables and compound jamo have to be split by the caller first")
    void rejectsUnsplitJamoQueries() {
        assertEquals("", KoreanKeyboardLayout.toLatin("가나"));
        assertEquals("", KoreanKeyboardLayout.toLatin("ㅘ"));
        assertEquals("", KoreanKeyboardLayout.toLatin("ㄱ1"));
        assertEquals("", KoreanKeyboardLayout.toLatin("  "));
        assertEquals("", KoreanKeyboardLayout.toLatin(""));
        assertEquals("", KoreanKeyboardLayout.toLatin(null));
    }

    @Test
    @DisplayName("queries that are not plain latin text are left to the normal matcher")
    void rejectsNonLatinQueries() {
        assertEquals("", KoreanKeyboardLayout.toJamo("다이아"));
        assertEquals("", KoreanKeyboardLayout.toJamo("rk1"));
        assertEquals("", KoreanKeyboardLayout.toJamo("  "));
        assertEquals("", KoreanKeyboardLayout.toJamo(""));
        assertEquals("", KoreanKeyboardLayout.toJamo(null));
    }
}
