package dev.erudites.mods.koreanify.client.ime;

import com.mojang.blaze3d.platform.TextInputManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class PreeditComposer {

    public record PreeditResult(String text, int cursor) {}

    private PreeditComposer() {}

    public static PreeditResult merge(String original, int cursor, String composition) {
        String safeComposition = Objects.requireNonNullElse(composition, "");
        if (safeComposition.isEmpty()) {
            return new PreeditResult(original, cursor);
        }
        int safeCursor = Mth.clamp(cursor, 0, original.length());
        String merged = original.substring(0, safeCursor) + safeComposition + original.substring(safeCursor);
        return new PreeditResult(merged, safeCursor + safeComposition.length());
    }

    public static String mergedSearchQuery(String value, int cursor, String composition) {
        if (value == null) {
            return "";
        }
        return merge(value, cursor, composition).text().toLowerCase(Locale.ROOT);
    }

    public static int availableSpace(int currentLength, int selectionStartPos, int selectionEndPos, int maxLength) {
        int selectionLength = Mth.abs(selectionStartPos - selectionEndPos);
        return Math.max(0, maxLength - (currentLength - selectionLength));
    }

    public static String fitComposition(String fullPreedit, String baseValue, int selectionStartPos, int selectionEndPos, Predicate<String> validator) {
        StringBuilder builder = new StringBuilder();
        int minSelectionPos = Math.min(selectionStartPos, selectionEndPos);
        int maxSelectionPos = Math.max(selectionStartPos, selectionEndPos);
        for (char ch : fullPreedit.toCharArray()) {
            String composition = builder.toString() + ch;
            String mergedText = new StringBuilder(baseValue)
                .replace(minSelectionPos, maxSelectionPos, composition)
                .toString();
            if (!validator.test(mergedText)) {
                break;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    public static void commitAndResetIme(String text, Consumer<String> inserter) {
        inserter.accept(text);
        resetIme();
    }

    public static void resetIme() {
        TextInputManager textInputManager = Minecraft.getInstance().textInputManager();
        textInputManager.onTextInputFocusChange(false);
        textInputManager.onTextInputFocusChange(true);
    }
}
