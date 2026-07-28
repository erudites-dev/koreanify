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
    @DisplayName("available room counts the selection that is about to be replaced")
    void countsAvailableSpace() {
        assertEquals(2, PreeditComposer.availableSpace(8, 4, 4, 10));
        assertEquals(0, PreeditComposer.availableSpace(10, 4, 4, 10));
        assertEquals(3, PreeditComposer.availableSpace(10, 2, 5, 10));
        assertEquals(0, PreeditComposer.availableSpace(12, 0, 0, 10));
    }

    @Test
    @DisplayName("the composition is cut where the field stops accepting it")
    void fitsCompositionToValidator() {
        assertEquals("한", PreeditComposer.fitComposition("한글", "가나다", 3, 3, text -> text.length() <= 4));
        assertEquals("", PreeditComposer.fitComposition("한글", "가나다", 3, 3, text -> text.length() <= 3));
        assertEquals("한글", PreeditComposer.fitComposition("한글", "가나다", 3, 3, text -> text.length() <= 5));
    }

    @Test
    @DisplayName("a selection is replaced by the composition when fitting it")
    void fitsCompositionOverSelection() {
        assertEquals("한글", PreeditComposer.fitComposition("한글", "가나다", 1, 3, text -> text.length() <= 3));
    }

    @Test
    @DisplayName("search queries are merged and lowercased")
    void buildsSearchQuery() {
        assertEquals("다이아", PreeditComposer.mergedSearchQuery("다이", 2, "아"));
        assertEquals("dia", PreeditComposer.mergedSearchQuery("DI", 2, "A"));
        assertEquals("", PreeditComposer.mergedSearchQuery(null, 0, "아"));
    }
}
