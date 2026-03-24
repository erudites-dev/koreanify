package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditState;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {

    @Shadow @Nullable
    private RecipeBookTabButton selectedTab;
    @Shadow
    protected Minecraft minecraft;
    @Shadow @Nullable
    private EditBox searchBox;
    @Shadow
    private String lastSearch;
    @Shadow
    private ClientRecipeBook book;
    @Shadow
    private boolean ignoreTextInput;
    @Shadow
    protected abstract void checkSearchStringUpdate();
    @Shadow
    protected abstract void pirateSpeechForThePeople(String text);
    @Shadow
    protected abstract boolean isFiltering();
    @Shadow
    protected abstract void updateCollections(boolean resetPage, boolean isFiltering);

    @Inject(method = "preeditUpdated", at = @At("RETURN"))
    private void onPreeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!this.ignoreTextInput && this.searchBox != null && this.searchBox.isVisible()) {
            this.checkSearchStringUpdate();
        }
    }

    @WrapMethod(method = "checkSearchStringUpdate")
    private void wrapCheckSearchStringUpdate(Operation<Void> original) {
        if (this.searchBox == null) {
            original.call();
            return;
        }
        String searchText = PreeditComposer.mergedSearchQuery(
            this.searchBox.getValue(),
            this.searchBox.getCursorPosition(),
            ((PreeditState) this.searchBox).koreanify_composition()
        );
        this.pirateSpeechForThePeople(searchText);
        if (!searchText.equals(this.lastSearch)) {
            this.updateCollections(false, this.isFiltering());
            this.lastSearch = searchText;
        }
    }

    @WrapOperation(
        method = "updateCollections",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;isEmpty()Z",
            ordinal = 0
        )
    )
    private boolean wrapIsEmpty(String instance, Operation<Boolean> original) {
        boolean empty = original.call(instance);
        if (!empty) {
            return false;
        }
        return this.searchBox == null || ((PreeditState) this.searchBox).koreanify_composition().isEmpty();
    }

    @WrapOperation(
        method = "updateCollections",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/searchtree/SearchTree;search(Ljava/lang/String;)Ljava/util/List;"
        )
    )
    private List<RecipeCollection> wrapSearch(SearchTree<RecipeCollection> instance, String searchTarget, Operation<List<RecipeCollection>> original) {
        if (this.searchBox == null) {
            return original.call(instance, searchTarget);
        }
        String composedTarget = PreeditComposer.mergedSearchQuery(
            this.searchBox.getValue(),
            this.searchBox.getCursorPosition(),
            ((PreeditState) this.searchBox).koreanify_composition()
        );
        List<RecipeCollection> vanillaResults = original.call(instance, composedTarget);
        if (this.minecraft.level == null || this.selectedTab == null) {
            return vanillaResults;
        }
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        List<RecipeCollection> koreanMatchedResults = this.book.getCollection(this.selectedTab.getCategory()).stream()
            .filter(collection -> !collection.getRecipes().isEmpty())
            .filter(collection -> {
                List<ItemStack> results = collection.getRecipes().getFirst().resultItems(context);
                return !results.isEmpty() && KoreanSearchMatcher.matches(results.getFirst().getHoverName().getString(), composedTarget);
            })
            .toList();
        return Stream.concat(vanillaResults.stream(), koreanMatchedResults.stream())
            .distinct()
            .toList();
    }
}
