package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(LanguageSelectScreen.LanguageSelectionList.class)
abstract class LanguageSelectScreenLanguageSelectionListMixin {

    @WrapOperation(
        method = "filterEntries",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
        )
    )
    private Stream<Map.Entry<String, LanguageInfo>> koreanify$wrapFilter(
        final Stream<Map.Entry<String, LanguageInfo>> instance,
        final Predicate<Map.Entry<String, LanguageInfo>> accepts,
        Operation<Stream<Map.Entry<String, LanguageInfo>>> original,
        final String filter
    ) {
        Predicate<Map.Entry<String, LanguageInfo>> koreanAccepts = entry ->
            KoreanSearchMatcher.matches(entry.getValue().name(), filter)
                || KoreanSearchMatcher.matches(entry.getValue().region(), filter);
        return original.call(instance, accepts.or(koreanAccepts));
    }
}
