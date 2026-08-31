package dev.erudites.mods.koreanify.client.search;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class NameIndex<T> {

    private static final String[] NO_NAMES = new String[0];

    private @Nullable Collection<T> source;
    private @Nullable String language;
    private List<T> entries = List.of();
    private String[] names = NO_NAMES;

    public void invalidate() {
        this.source = null;
        this.language = null;
        this.entries = List.of();
        this.names = NO_NAMES;
    }

    public List<T> matching(final Collection<T> source, final String query, final Function<T, String> nameOf) {
        this.refreshIfStale(source, nameOf);
        ImmutableList.Builder<T> matched = ImmutableList.builder();
        for (int i = 0; i < this.names.length; i++) {
            if (KoreanSearchMatcher.matches(this.names[i], query)) {
                matched.add(this.entries.get(i));
            }
        }
        return matched.build();
    }

    private void refreshIfStale(final Collection<T> source, final Function<T, String> nameOf) {
        String currentLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
        if (this.source == source
            && this.names.length == source.size()
            && currentLanguage.equals(this.language)
        ) {
            return;
        }
        this.entries = List.copyOf(source);
        this.names = new String[this.entries.size()];
        for (int i = 0; i < this.names.length; i++) {
            this.names[i] = nameOf.apply(this.entries.get(i));
        }
        this.source = source;
        this.language = currentLanguage;
    }
}
