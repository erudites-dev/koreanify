package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PreeditDispatcher {

    private final PreeditHandler handler = new PreeditHandler();

    public String composition() {
        return this.handler.composition();
    }

    public void clear() {
        this.handler.clear();
    }

    public void apply(
        PreeditEvent event,
        String currentValue,
        int cursorPos,
        int highlightPos,
        int maxLength,
        Consumer<String> inserter,
        @Nullable Consumer<String> responder
    ) {
        run(
            this.handler.handlePreedit(event, currentValue, cursorPos, highlightPos, maxLength),
            inserter,
            responder
        );
    }

    public void apply(
        PreeditEvent event,
        String currentValue,
        int cursorPos,
        int selectCursor,
        Predicate<String> validator,
        Consumer<String> inserter,
        @Nullable Consumer<String> responder
    ) {
        run(
            this.handler.handlePreedit(event, currentValue, cursorPos, selectCursor, validator),
            inserter,
            responder
        );
    }

    private static void run(
        PreeditResult result,
        Consumer<String> inserter,
        @Nullable Consumer<String> responder
    ) {
        switch (result) {
            case PreeditResult.Commit(String text) -> PreeditComposer.commitAndResetIme(text, inserter);
            case PreeditResult.Notify(String value) when responder != null -> responder.accept(value);
            case PreeditResult.Notify(_), PreeditResult.Unchanged() -> {}
        }
    }
}
