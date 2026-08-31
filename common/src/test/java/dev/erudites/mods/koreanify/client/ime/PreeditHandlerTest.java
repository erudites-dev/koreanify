package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.input.PreeditEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class PreeditHandlerTest {

    private static final int CHAT_LIMIT = 256;

    private static PreeditEvent preedit(final String text) {
        return new PreeditEvent(text, text.length(), List.of(text), 0);
    }

    private static UnaryOperator<String> roomFor(final String value, final int cursorPos, final int maxLength) {
        return PreeditComposer.fitToLength(value, cursorPos, cursorPos, maxLength);
    }

    @Test
    @DisplayName("a new composition is reported with the text merged at the cursor")
    void notifiesMergedComposition() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(preedit("한"), "안녕", 2, roomFor("안녕", 2, CHAT_LIMIT));

        assertEquals(new PreeditResult.Notify("안녕한"), result);
        assertEquals("한", handler.composition());
    }

    @Test
    @DisplayName("an unchanged composition does not notify again")
    void skipsUnchangedComposition() {
        PreeditHandler handler = new PreeditHandler();
        handler.handlePreedit(preedit("한"), "", 0, roomFor("", 0, CHAT_LIMIT));

        PreeditResult result = handler.handlePreedit(preedit("한"), "", 0, roomFor("", 0, CHAT_LIMIT));

        assertSame(PreeditResult.UNCHANGED, result);
    }

    @Test
    @DisplayName("ending a composition reports the untouched value")
    void notifiesOnCompositionEnd() {
        PreeditHandler handler = new PreeditHandler();
        handler.handlePreedit(preedit("한"), "안녕", 2, roomFor("안녕", 2, CHAT_LIMIT));

        PreeditResult result = handler.handlePreedit(null, "안녕", 2, roomFor("안녕", 2, CHAT_LIMIT));

        assertEquals(new PreeditResult.Notify("안녕"), result);
        assertEquals("", handler.composition());
    }

    @Test
    @DisplayName("a full text field cancels the composition instead of hiding it")
    void cancelsWhenNoRoomLeft() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(preedit("한"), "가나다", 3, roomFor("가나다", 3, 3));

        assertSame(PreeditResult.CANCEL, result);
        assertEquals("", handler.composition());
    }

    @Test
    @DisplayName("only the part that fits is committed")
    void commitsWhatFits() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(preedit("한글"), "가나다", 3, roomFor("가나다", 3, 4));

        assertEquals(new PreeditResult.Commit("한"), result);
        assertEquals("", handler.composition());
    }

    @Test
    @DisplayName("a selection frees up room for the composition")
    void countsSelectionAsAvailableRoom() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(
            preedit("한"),
            "가나다",
            1,
            PreeditComposer.fitToLength("가나다", 1, 3, 3)
        );

        assertInstanceOf(PreeditResult.Notify.class, result);
    }

    @Test
    @DisplayName("validator based fields cancel when nothing fits")
    void cancelsWhenValidatorRejectsEverything() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(
            preedit("한"),
            "가나다",
            3,
            PreeditComposer.fitToValidator("가나다", 3, 3, text -> text.length() <= 3)
        );

        assertSame(PreeditResult.CANCEL, result);
    }

    @Test
    @DisplayName("validator based fields commit the part that passes")
    void commitsValidatedPrefix() {
        PreeditHandler handler = new PreeditHandler();

        PreeditResult result = handler.handlePreedit(
            preedit("한글"),
            "가나다",
            3,
            PreeditComposer.fitToValidator("가나다", 3, 3, text -> text.length() <= 4)
        );

        assertEquals(new PreeditResult.Commit("한"), result);
    }
}
