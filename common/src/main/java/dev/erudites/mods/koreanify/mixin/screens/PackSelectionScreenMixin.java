package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(PackSelectionScreen.class)
abstract class PackSelectionScreenMixin {

    @WrapOperation(
        method = "filterEntries",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
        )
    )
    private Stream<PackSelectionModel.Entry> koreanify$wrapFilter(
        final Stream<PackSelectionModel.Entry> instance,
        final Predicate<PackSelectionModel.Entry> accepts,
        Operation<Stream<PackSelectionModel.Entry>> original,
        final String value
    ) {
        Predicate<PackSelectionModel.Entry> koreanAccepts = entry ->
            KoreanSearchMatcher.matches(entry.getId(), value)
                || KoreanSearchMatcher.matches(entry.getTitle().getString(), value)
                || KoreanSearchMatcher.matches(entry.getDescription().getString(), value);
        return original.call(instance, accepts.or(koreanAccepts));
    }
}
