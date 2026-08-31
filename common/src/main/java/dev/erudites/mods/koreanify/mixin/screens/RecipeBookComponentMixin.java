package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.ComposedSearch;
import dev.erudites.mods.koreanify.client.search.NameIndex;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {

    @Shadow
    private @Nullable RecipeBookTabButton selectedTab;
    @Shadow
    protected Minecraft minecraft;
    @Shadow
    private @Nullable EditBox searchBox;
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

    @Unique
    private final NameIndex<RecipeCollection> recipeNameIndex = new NameIndex<>();

    @Inject(method = "preeditUpdated", at = @At("RETURN"))
    private void koreanify$preeditUpdated(final @Nullable PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ComposedSearch.shouldRefresh(this.ignoreTextInput, this.searchBox)) {
            this.checkSearchStringUpdate();
        }
    }

    @WrapMethod(method = "checkSearchStringUpdate")
    private void koreanify$wrapCheckSearchStringUpdate(Operation<Void> original) {
        if (this.searchBox == null) {
            original.call();
            return;
        }
        String searchText = ComposedSearch.query(this.searchBox);
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
    private boolean koreanify$wrapIsEmpty(String instance, Operation<Boolean> original) {
        return original.call(instance) && !ComposedSearch.composing(this.searchBox);
    }

    @WrapOperation(
        method = "updateCollections",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/searchtree/SearchTree;search(Ljava/lang/String;)Ljava/util/List;"
        )
    )
    private List<RecipeCollection> koreanify$wrapSearch(
        final SearchTree<RecipeCollection> instance,
        final String searchTarget,
        Operation<List<RecipeCollection>> original
    ) {
        String composedTarget = ComposedSearch.query(this.searchBox);
        if (composedTarget.isEmpty()) {
            return original.call(instance, searchTarget);
        }
        List<RecipeCollection> vanillaResults = original.call(instance, composedTarget);
        if (this.minecraft.level == null || this.selectedTab == null) {
            return vanillaResults;
        }
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        List<RecipeCollection> koreanMatchedResults = this.recipeNameIndex.matching(
            this.book.getCollection(this.selectedTab.getCategory()),
            composedTarget,
            collection -> collection.getRecipes().stream()
                .findFirst()
                .flatMap(recipe -> recipe.resultItems(context).stream().findFirst())
                .map(result -> result.getHoverName().getString())
                .orElse("")
        );
        return ComposedSearch.combine(vanillaResults, koreanMatchedResults);
    }
}
