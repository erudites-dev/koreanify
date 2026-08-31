package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.gui.screens.social.SocialInteractionsPlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;

@Mixin(SocialInteractionsPlayerList.class)
abstract class SocialInteractionsPlayerListMixin {

    @Shadow
    private String filter;

    @WrapOperation(
        method = "updateFilteredPlayers",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;removeIf(Ljava/util/function/Predicate;)Z"
        )
    )
    private boolean koreanify$wrapRemoveIf(
        final List<PlayerEntry> instance,
        final Predicate<PlayerEntry> removes,
        Operation<Boolean> original
    ) {
        Predicate<PlayerEntry> koreanKeeps = player -> !KoreanSearchMatcher.matches(player.getPlayerName(), this.filter);
        return original.call(instance, removes.and(koreanKeeps));
    }

    @WrapOperation(
        method = "addPlayer",
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
