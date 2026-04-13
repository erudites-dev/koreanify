package dev.erudites.mods.koreanify.mixin.commands;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditState;
import dev.erudites.mods.koreanify.client.search.KoreanSearchMatcher;
import dev.erudites.mods.koreanify.mixin.components.EditBoxAccessor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CommandSuggestions.class)
abstract class CommandSuggestionsMixin {

    @Shadow @Final
    private EditBox input;

    @Shadow
    private boolean keepSuggestions;

    @WrapMethod(method = "updateCommandInfo")
    private void koreanify$wrapUpdateCommandInfo(Operation<Void> original) {
        if (this.keepSuggestions || !(this.input instanceof PreeditState state)) {
            original.call();
            return;
        }
        String composition = state.koreanify$composition();
        if (composition.isEmpty()) {
            original.call();
            return;
        }
        EditBoxAccessor box = (EditBoxAccessor) this.input;
        String value = this.input.getValue();
        int cursor = box.koreanify$cursorPos();
        int highlight = box.koreanify$highlightPos();
        PreeditComposer.PreeditResult result = PreeditComposer.merge(value, cursor, composition);
        box.koreanify$value(result.text());
        box.koreanify$cursorPos(result.cursor());
        box.koreanify$highlightPos(result.cursor());
        try {
            original.call();
        } finally {
            box.koreanify$value(value);
            box.koreanify$cursorPos(cursor);
            box.koreanify$highlightPos(highlight);
        }
    }

    @WrapMethod(method = "calculateSuggestionSuffix")
    private static @Nullable String koreanify$wrapCalculateSuggestionSuffix(
        String contents,
        String suggestion,
        Operation<String> original
    ) {
        String result = original.call(contents, suggestion);
        if (result != null || suggestion.length() <= contents.length()) {
            return result;
        }
        String prefix = suggestion.substring(0, contents.length());
        return KoreanSearchMatcher.matches(prefix, contents)
            ? suggestion.substring(contents.length())
            : null;
    }
}
