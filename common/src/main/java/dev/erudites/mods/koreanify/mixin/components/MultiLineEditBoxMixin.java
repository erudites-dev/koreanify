package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditDispatcher;
import dev.erudites.mods.koreanify.client.ime.PreeditOverlayState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiLineEditBox.class)
abstract class MultiLineEditBoxMixin {

    @Shadow @Final
    private MultilineTextField textField;
    @Shadow
    private @Nullable IMEPreeditOverlay preeditOverlay;

    @Unique
    private final PreeditDispatcher preeditDispatcher = new PreeditDispatcher();

    @Inject(method = "preeditUpdated", at = @At("RETURN"))
    private void koreanify$preeditUpdated(final @Nullable PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.preeditOverlay != null) {
            ((PreeditOverlayState) this.preeditOverlay).koreanify$markInlined();
        }
        MultilineTextFieldAccessor field = (MultilineTextFieldAccessor) this.textField;
        this.preeditDispatcher.apply(
            event,
            this.textField.value(),
            this.textField.cursor(),
            field.koreanify$selectCursor(),
            mergedText -> mergedText.length() <= this.textField.characterLimit() && !field.koreanify$overflowsLineLimit(mergedText),
            this.textField::insertText,
            field.koreanify$valueListener()
        );
    }

    @WrapMethod(method = "extractContents")
    private void koreanify$wrapExtractContents(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY,
        final float delta,
        Operation<Void> original
    ) {
        if (this.preeditDispatcher.composition().isEmpty()) {
            original.call(graphics, mouseX, mouseY, delta);
            return;
        }
        String previousValue = this.textField.value();
        int previousCursor = this.textField.cursor();
        PreeditComposer.MergeResult result = this.preeditDispatcher.merge(previousValue, previousCursor);
        this.textField.setValue(result.text());
        MultilineTextFieldAccessor field = (MultilineTextFieldAccessor) this.textField;
        field.koreanify$cursor(result.cursor());
        field.koreanify$selectCursor(result.cursor());
        try {
            original.call(graphics, mouseX, mouseY, delta);
        } finally {
            this.textField.setValue(previousValue);
            field.koreanify$cursor(previousCursor);
            field.koreanify$selectCursor(previousCursor);
        }
    }
}
