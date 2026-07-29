package dev.erudites.mods.koreanify.client.ime;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class PreeditComposer {

    public record MergeResult(String text, int cursor) {}

    private PreeditComposer() {}

    public static MergeResult merge(final String original, final int cursor, final @Nullable String composition) {
        String safeComposition = Objects.requireNonNullElse(composition, "");
        if (safeComposition.isEmpty()) {
            return new MergeResult(original, cursor);
        }
        int safeCursor = Mth.clamp(cursor, 0, original.length());
        String merged = original.substring(0, safeCursor) + safeComposition + original.substring(safeCursor);
        return new MergeResult(merged, safeCursor + safeComposition.length());
    }

    public static String mergedSearchQuery(final @Nullable String value, final int cursor, final String composition) {
        if (value == null) {
            return "";
        }
        return merge(value, cursor, composition).text().toLowerCase(Locale.ROOT);
    }

    public static UnaryOperator<String> fitToLength(
        final String baseValue,
        final int selectionStartPos,
        final int selectionEndPos,
        final int maxLength
    ) {
        int available = availableSpace(baseValue.length(), selectionStartPos, selectionEndPos, maxLength);
        return preedit -> preedit.length() <= available
            ? preedit
            : preedit.substring(0, available);
    }

    public static UnaryOperator<String> fitToValidator(
        final String baseValue,
        final int selectionStartPos,
        final int selectionEndPos,
        final Predicate<String> validator
    ) {
        return preedit -> fitComposition(preedit, baseValue, selectionStartPos, selectionEndPos, validator);
    }

    private static int availableSpace(
        final int currentLength,
        final int selectionStartPos,
        final int selectionEndPos,
        final int maxLength
    ) {
        int selectionLength = Mth.abs(selectionStartPos - selectionEndPos);
        return Math.max(0, maxLength - (currentLength - selectionLength));
    }

    private static String fitComposition(
        final String fullPreedit,
        final String baseValue,
        final int selectionStartPos,
        final int selectionEndPos,
        final Predicate<String> validator
    ) {
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

    public static void commitAndResetIme(final String text, final Consumer<String> inserter) {
        inserter.accept(text);
        resetIme();
    }

    public static void resetIme() {
        GLFW.glfwResetPreeditText(Minecraft.getInstance().getWindow().handle());
    }
}
