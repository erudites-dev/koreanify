package dev.erudites.mods.koreanify.client.search;

import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditState;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

public final class ComposedSearch {

    private ComposedSearch() {}

    public static boolean shouldRefresh(final boolean ignoreTextInput, final @Nullable EditBox searchBox) {
        return !ignoreTextInput && searchBox != null && searchBox.isVisible();
    }

    public static boolean composing(final @Nullable EditBox searchBox) {
        return !composition(searchBox).isEmpty();
    }

    public static String query(final @Nullable EditBox searchBox) {
        if (searchBox == null) {
            return "";
        }
        return PreeditComposer.mergedSearchQuery(
            searchBox.getValue(),
            searchBox.getCursorPosition(),
            composition(searchBox)
        );
    }

    public static <T> List<T> combine(final List<T> vanillaResults, final List<T> koreanMatchedResults) {
        return Stream.concat(vanillaResults.stream(), koreanMatchedResults.stream())
            .distinct()
            .toList();
    }

    private static String composition(final @Nullable EditBox searchBox) {
        return searchBox instanceof PreeditState state ? state.koreanify$composition() : "";
    }
}
