package dev.erudites.mods.koreanify.client.ime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreeditComposerTest {

    @Test
    @DisplayName("the composition is inserted at the cursor and the cursor follows it")
    void mergesAtCursor() {
        PreeditComposer.MergeResult result = PreeditComposer.merge("안녕세요", 2, "하");

        assertEquals("안녕하세요", result.text());
        assertEquals(3, result.cursor());
    }

    @Test
    @DisplayName("an out of range cursor is clamped to the value")
    void clampsCursor() {
        assertEquals("안녕하", PreeditComposer.merge("안녕", 9, "하").text());
        assertEquals("하안녕", PreeditComposer.merge("안녕", -1, "하").text());
    }

    @Test
    @DisplayName("without a composition the value is returned untouched")
    void keepsValueWithoutComposition() {
        PreeditComposer.MergeResult result = PreeditComposer.merge("안녕", 1, null);

        assertEquals("안녕", result.text());
        assertEquals(1, result.cursor());
    }

    @Test
    @DisplayName("the composition is cut at the character limit")
    void fitsCompositionToLength() {
        assertEquals("한글", PreeditComposer.fitToLength("가나", 2, 2, 10).apply("한글"));
        assertEquals("한", PreeditComposer.fitToLength("가나다", 3, 3, 4).apply("한글"));
        assertEquals("", PreeditComposer.fitToLength("가나다", 3, 3, 3).apply("한글"));
        assertEquals("", PreeditComposer.fitToLength("가나다", 0, 0, 2).apply("한글"));
    }

    @Test
    @DisplayName("the selection about to be replaced counts as free room")
    void fitsCompositionOverSelectedLength() {
        assertEquals("한글", PreeditComposer.fitToLength("가나다", 0, 3, 3).apply("한글"));
    }

    @Test
    @DisplayName("the composition is cut where the field stops accepting it")
    void fitsCompositionToValidator() {
        assertEquals("한", PreeditComposer.fitToValidator("가나다", 3, 3, text -> text.length() <= 4).apply("한글"));
        assertEquals("", PreeditComposer.fitToValidator("가나다", 3, 3, text -> text.length() <= 3).apply("한글"));
        assertEquals("한글", PreeditComposer.fitToValidator("가나다", 3, 3, text -> text.length() <= 5).apply("한글"));
    }

    @Test
    @DisplayName("a selection is replaced by the composition when fitting it")
    void fitsCompositionOverSelection() {
        assertEquals("한글", PreeditComposer.fitToValidator("가나다", 1, 3, text -> text.length() <= 3).apply("한글"));
    }

    @Test
    @DisplayName("search queries are merged and lowercased")
    void buildsSearchQuery() {
        assertEquals("다이아", PreeditComposer.mergedSearchQuery("다이", 2, "아"));
        assertEquals("dia", PreeditComposer.mergedSearchQuery("DI", 2, "A"));
        assertEquals("", PreeditComposer.mergedSearchQuery(null, 0, "아"));
    }
}
