package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditState;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.client.searchtree.SearchTree;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.stream.Stream;

@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {

    @Shadow
    private EditBox searchBox;
    @Shadow
    private boolean ignoreTextInput;
    @Shadow
    protected abstract void refreshSearchResults();

    @Inject(method = "preeditUpdated", at = @At("RETURN"))
    private void koreanify$preeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!this.ignoreTextInput && this.searchBox != null && this.searchBox.isVisible()) {
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
        boolean empty = original.call(instance);
        if (!empty) {
            return false;
        }
        return this.searchBox == null || ((PreeditState) this.searchBox).koreanify$composition().isEmpty();
    }

    @WrapOperation(
        method = "refreshSearchResults",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/searchtree/SearchTree;search(Ljava/lang/String;)Ljava/util/List;"
        )
    )
    private List<ItemStack> koreanify$wrapSearch(SearchTree<ItemStack> instance, String searchTarget, Operation<List<ItemStack>> original) {
        String composedTarget = PreeditComposer.mergedSearchQuery(
            this.searchBox.getValue(),
            this.searchBox.getCursorPosition(),
            ((PreeditState) this.searchBox).koreanify$composition()
        );
        if (composedTarget.startsWith("#")) {
            return original.call(instance, composedTarget);
        }
        List<ItemStack> vanillaResults = original.call(instance, composedTarget);
        List<ItemStack> koreanMatchedResults = CreativeModeTabs.searchTab().getDisplayItems().stream()
            .filter(item -> KoreanSearchMatcher.matches(item.getHoverName().getString(), composedTarget))
            .toList();
        return Stream.concat(vanillaResults.stream(), koreanMatchedResults.stream())
            .distinct()
            .toList();
    }
}
