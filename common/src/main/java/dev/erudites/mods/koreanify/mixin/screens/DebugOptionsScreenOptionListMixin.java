package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.debug.DebugOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DebugOptionsScreen.OptionList.class)
abstract class DebugOptionsScreenOptionListMixin {

    @WrapOperation(
        method = "updateSearch",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"
        )
    )
    private boolean koreanify$wrapContains(
        final String instance,
        final CharSequence query,
        Operation<Boolean> original
    ) {
        return original.call(instance, query) || KoreanSearchMatcher.matches(instance, query.toString());
    }
}
