package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PreeditHandler {

    private String composition = "";

    public String composition() {
        return this.composition;
    }

    public void handlePreedit(
        PreeditEvent event,
        String currentValue,
        int cursorPos,
        int highlightPos,
        int maxLength,
        Consumer<String> inserter,
        @Nullable Consumer<String> responder
    ) {
        String fullPreedit = (event != null) ? event.fullText() : "";
        if (fullPreedit.isEmpty()) {
            this.composition = "";
            if (responder != null) {
                responder.accept(currentValue);
            }
            return;
        }
        int available = PreeditComposer.availableSpace(
            currentValue.length(),
            cursorPos,
            highlightPos,
            maxLength
        );
        if (available == 0) {
            this.composition = "";
            return;
        }
        if (fullPreedit.length() > available) {
            PreeditComposer.commitAndResetIme(
                fullPreedit.substring(0, available),
                inserter
            );
            this.composition = "";
            return;
        }
        this.composition = fullPreedit;
        if (responder != null) {
            responder.accept(
                PreeditComposer.merge(
                    currentValue,
                    cursorPos,
                    this.composition
                ).text()
            );
        }
    }

    public void handlePreedit(
        PreeditEvent event,
        String currentValue,
        int cursorPos,
        int selectCursor,
        Predicate<String> validator,
        Consumer<String> inserter,
        @Nullable Consumer<String> responder
    ) {
        String fullPreedit = (event != null) ? event.fullText() : "";
        if (fullPreedit.isEmpty()) {
            this.composition = "";
            if (responder != null) {
                responder.accept(currentValue);
            }
            return;
        }
        String valid = PreeditComposer.fitComposition(
            fullPreedit,
            currentValue,
            cursorPos,
            selectCursor,
            validator
        );
        if (valid.isEmpty()) {
            this.composition = "";
            return;
        }
        if (valid.length() < fullPreedit.length()) {
            PreeditComposer.commitAndResetIme(
                valid,
                inserter
            );
            this.composition = "";
            return;
        }
        this.composition = valid;
        if (responder != null) {
            responder.accept(
                PreeditComposer.merge(
                    currentValue,
                    cursorPos,
                    this.composition
                ).text()
            );
        }
    }

    public void clear() {
        this.composition = "";
    }
}
