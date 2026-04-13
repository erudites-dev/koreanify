package dev.erudites.mods.koreanify.mixin.commands;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@Mixin(LiteralCommandNode.class)
abstract class LiteralCommandNodeMixin {

    @Shadow @Final
    private String literal;

    @WrapMethod(method = "listSuggestions")
    private CompletableFuture<Suggestions> koreanify$wrapListSuggestions(
        CommandContext<?> context,
        SuggestionsBuilder builder,
        Operation<CompletableFuture<Suggestions>> original
    ) {
        String remaining = builder.getRemaining();
        if (!remaining.isEmpty()
            && remaining.indexOf(' ') < 0
            && (!KoreanifyConfig.INSTANCE.command.commandSearchKoreanOnly || remaining.chars().anyMatch(c -> c >= 'ㄱ'))
            && KoreanSearchMatcher.matches(this.literal, remaining)) {
            return builder.suggest(this.literal).buildFuture();
        }
        return original.call(context, builder);
    }
}
