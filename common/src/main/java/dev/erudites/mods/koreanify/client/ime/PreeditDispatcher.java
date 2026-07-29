package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class PreeditDispatcher {

    private final PreeditHandler handler = new PreeditHandler();

    public String composition() {
        return this.handler.composition();
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
        this.apply(
            event,
            currentValue,
            cursorPos,
            PreeditComposer.fitToLength(currentValue, cursorPos, highlightPos, maxLength),
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
        this.apply(
            event,
            currentValue,
            cursorPos,
            PreeditComposer.fitToValidator(currentValue, cursorPos, selectCursor, validator),
            inserter,
            responder
        );
    }

    private void apply(
        final @Nullable PreeditEvent event,
        final String currentValue,
        final int cursorPos,
        final UnaryOperator<String> fit,
        final Consumer<String> inserter,
        final @Nullable Consumer<String> responder
    ) {
        dispatch(this.handler.handlePreedit(event, currentValue, cursorPos, fit), inserter, responder);
    }

    private static void dispatch(
        final PreeditResult result,
        final Consumer<String> inserter,
        final @Nullable Consumer<String> responder
    ) {
        switch (result) {
            case PreeditResult.Commit(String text) -> PreeditComposer.commitAndResetIme(text, inserter);
            case PreeditResult.Cancel() -> PreeditComposer.resetIme();
            case PreeditResult.Notify(String value) when responder != null -> responder.accept(value);
            case PreeditResult.Notify(_), PreeditResult.Unchanged() -> {}
        }
    }
}
