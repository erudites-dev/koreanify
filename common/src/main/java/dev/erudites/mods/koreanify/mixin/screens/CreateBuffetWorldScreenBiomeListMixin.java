package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(CreateBuffetWorldScreen.BiomeList.class)
abstract class CreateBuffetWorldScreenBiomeListMixin {

    @WrapOperation(
        method = "filterEntries",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"
        )
    )
    private Stream<CreateBuffetWorldScreen.BiomeList.Entry> koreanify$wrapFilter(
        final Stream<CreateBuffetWorldScreen.BiomeList.Entry> instance,
        final Predicate<CreateBuffetWorldScreen.BiomeList.Entry> accepts,
        Operation<Stream<CreateBuffetWorldScreen.BiomeList.Entry>> original,
        final String filter
    ) {
        Predicate<CreateBuffetWorldScreen.BiomeList.Entry> koreanAccepts =
            entry -> KoreanSearchMatcher.matches(
                ((CreateBuffetWorldScreenBiomeListEntryAccessor) entry).koreanify$name().getString(),
                filter
            );
        return original.call(instance, accepts.or(koreanAccepts));
    }
}
