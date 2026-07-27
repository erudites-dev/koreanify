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

    public void cancel() {
        this.handler.clear();
        PreeditComposer.resetIme();
    }

    public PreeditComposer.MergeResult merge(final String currentValue, final int cursorPos) {
        return PreeditComposer.merge(currentValue, cursorPos, this.handler.composition());
    }

    public void apply(
        final @Nullable PreeditEvent event,
        final String currentValue,
        final int cursorPos,
        final int highlightPos,
        final int maxLength,
        final Consumer<String> inserter,
        final @Nullable Consumer<String> responder
    ) {
        run(
            this.handler.handlePreedit(event, currentValue, cursorPos, highlightPos, maxLength),
            inserter,
            responder
        );
    }

    public void apply(
        final @Nullable PreeditEvent event,
        final String currentValue,
        final int cursorPos,
        final int selectCursor,
        final Predicate<String> validator,
        final Consumer<String> inserter,
        final @Nullable Consumer<String> responder
    ) {
        run(
            this.handler.handlePreedit(event, currentValue, cursorPos, selectCursor, validator),
            inserter,
            responder
        );
    }

    private static void run(
        final PreeditResult result,
        final Consumer<String> inserter,
        final @Nullable Consumer<String> responder
    ) {
        switch (result) {
            case PreeditResult.Commit(String text) -> PreeditComposer.commitAndResetIme(text, inserter);
            case PreeditResult.Notify(String value) when responder != null -> responder.accept(value);
            case PreeditResult.Notify(_), PreeditResult.Unchanged() -> {}
        }
    }
}
