package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.UnaryOperator;

public final class PreeditHandler {

    private String composition = "";

    public String composition() {
        return this.composition;
    }

    public void clear() {
        this.composition = "";
    }

    public PreeditResult handlePreedit(
        final @Nullable PreeditEvent event,
        final String currentValue,
        final int cursorPos,
        final UnaryOperator<String> fit
    ) {
        String prevComposition = this.composition;
        String fullPreedit = (event != null) ? event.fullText() : "";
        if (fullPreedit.isEmpty()) {
            return this.endComposition(prevComposition, currentValue);
        }
        String fittedPreedit = fit.apply(fullPreedit);
        if (fittedPreedit.isEmpty()) {
            this.composition = "";
            return PreeditResult.CANCEL;
        }
        if (fittedPreedit.length() < fullPreedit.length()) {
            this.composition = "";
            return new PreeditResult.Commit(fittedPreedit);
        }
        return this.updateComposition(prevComposition, fittedPreedit, currentValue, cursorPos);
    }

    private PreeditResult endComposition(final String prevComposition, final String currentValue) {
        this.composition = "";
        return prevComposition.isEmpty()
            ? PreeditResult.UNCHANGED
            : new PreeditResult.Notify(currentValue);
    }

    private PreeditResult updateComposition(
        final String prevComposition,
        final String newComposition,
        final String currentValue,
        final int cursorPos
    ) {
        if (prevComposition.equals(newComposition)) {
            return PreeditResult.UNCHANGED;
        }
        this.composition = newComposition;
        return new PreeditResult.Notify(
            PreeditComposer.merge(currentValue, cursorPos, newComposition).text()
        );
    }
}
