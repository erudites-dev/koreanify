package dev.erudites.mods.koreanify.mixin.commands;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SharedSuggestionProvider.class)
interface SuggestionsBuilderMixin {

    @WrapMethod(method = "matchesSubStr")
    private static boolean koreanify$wrapSubStr(String pattern, String input, Operation<Boolean> original) {
        return original.call(
            KoreanSearchMatcher.toJamo(pattern),
            KoreanSearchMatcher.toJamo(input)
        );
    }
}
