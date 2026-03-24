package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WorldSelectionList.class)
abstract class WorldSelectionListMixin {

    @WrapMethod(method = "filterAccepts")
    private boolean wrapFilterAccepts(String filter, LevelSummary level, Operation<Boolean> original) {
        boolean originalResult = original.call(filter, level);
        if (originalResult) {
            return true;
        }
        return KoreanSearchMatcher.matches(level.getLevelName(), filter) || KoreanSearchMatcher.matches(level.getLevelId(), filter);
    }
}
