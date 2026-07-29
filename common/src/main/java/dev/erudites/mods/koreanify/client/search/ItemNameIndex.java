package dev.erudites.mods.koreanify.client.search;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ItemNameIndex {

    private static final ItemStack[] NO_ITEMS = new ItemStack[0];
    private static final String[] NO_NAMES = new String[0];

    private @Nullable Collection<ItemStack> source;
    private @Nullable String language;
    private ItemStack[] items = NO_ITEMS;
    private String[] names = NO_NAMES;

    public void invalidate() {
        this.source = null;
        this.language = null;
        this.items = NO_ITEMS;
        this.names = NO_NAMES;
    }

    public List<ItemStack> matching(final Collection<ItemStack> displayItems, final String query) {
        this.refreshIfStale(displayItems);
        List<ItemStack> matched = new ArrayList<>();
        for (int i = 0; i < this.items.length; i++) {
            if (KoreanSearchMatcher.matches(this.names[i], query)) {
                matched.add(this.items[i]);
            }
        }
        return matched;
    }

    private void refreshIfStale(final Collection<ItemStack> displayItems) {
        String currentLanguage = Minecraft.getInstance().getLanguageManager().getSelected();
        if (this.source == displayItems
            && this.items.length == displayItems.size()
            && currentLanguage.equals(this.language)
        ) {
            return;
        }
        this.items = displayItems.toArray(ItemStack[]::new);
        this.names = new String[this.items.length];
        for (int i = 0; i < this.items.length; i++) {
            this.names[i] = this.items[i].getHoverName().getString();
        }
        this.source = displayItems;
        this.language = currentLanguage;
    }
}
