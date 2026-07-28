package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.worldselection.AbstractGameRulesScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractGameRulesScreen.RuleList.class)
abstract class AbstractGameRulesScreenRuleListMixin {

    @WrapMethod(method = "toLowerCaseMatchesFilter")
    private static boolean koreanify$wrapToLowerCaseMatchesFilter(
        final String gameRuleId,
        final String lowerCaseFilter,
        Operation<Boolean> original
    ) {
        return original.call(gameRuleId, lowerCaseFilter) || KoreanSearchMatcher.matches(gameRuleId, lowerCaseFilter);
    }
}
