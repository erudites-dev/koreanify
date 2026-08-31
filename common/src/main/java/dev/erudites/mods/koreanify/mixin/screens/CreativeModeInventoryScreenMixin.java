package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.ComposedSearch;
import dev.erudites.mods.koreanify.client.search.NameIndex;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {

    @Shadow
    private EditBox searchBox;
    @Shadow
    private boolean ignoreTextInput;
    @Shadow
    protected abstract void refreshSearchResults();

    @Unique
    private final NameIndex<ItemStack> itemNameIndex = new NameIndex<>();

    @Inject(method = "init", at = @At("TAIL"))
    private void koreanify$invalidateItemNameIndex(CallbackInfo ci) {
        this.itemNameIndex.invalidate();
    }

    @Inject(method = "preeditUpdated", at = @At("RETURN"))
    private void koreanify$preeditUpdated(final @Nullable PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ComposedSearch.shouldRefresh(this.ignoreTextInput, this.searchBox)) {
            this.refreshSearchResults();
        }
    }

    @WrapOperation(
        method = "refreshSearchResults",
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
        method = "refreshSearchResults",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/searchtree/SearchTree;search(Ljava/lang/String;)Ljava/util/List;"
        )
    )
    private List<ItemStack> koreanify$wrapSearch(
        final SearchTree<ItemStack> instance,
        final String searchTarget,
        Operation<List<ItemStack>> original
    ) {
        String composedTarget = ComposedSearch.query(this.searchBox);
        if (composedTarget.isEmpty()) {
            return original.call(instance, searchTarget);
        }
        if (composedTarget.startsWith("#")) {
            return original.call(instance, composedTarget.substring(1));
        }
        List<ItemStack> vanillaResults = original.call(instance, composedTarget);
        List<ItemStack> koreanMatchedResults = this.itemNameIndex.matching(
            CreativeModeTabs.searchTab().getDisplayItems(),
            composedTarget,
            stack -> stack.getHoverName().getString()
        );
        return ComposedSearch.combine(vanillaResults, koreanMatchedResults);
    }
}
