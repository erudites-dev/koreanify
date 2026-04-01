package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.input.PreeditEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(MultiLineEditBox.class)
abstract class MultiLineEditBoxMixin {

    @Shadow @Final
    private MultilineTextField textField;

    @Unique
    private final PreeditHandler preeditHandler = new PreeditHandler();

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void onPreeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        MultilineTextFieldAccessor field = (MultilineTextFieldAccessor) this.textField;
        this.preeditHandler.handlePreedit(
            event,
            this.textField.value(),
            this.textField.cursor(),
            field.koreanify$selectCursor(),
            mergedText -> mergedText.length() <= this.textField.characterLimit() && !field.koreanify$overflowsLineLimit(mergedText),
            this.textField::insertText,
            field.koreanify$valueListener()
        );
        cir.setReturnValue(true);
    }

    @WrapMethod(method = "extractContents")
    private void wrapExtractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, Operation<Void> original) {
        if (this.preeditHandler.composition().isEmpty()) {
            original.call(graphics, mouseX, mouseY, delta);
            return;
        }
        String previousValue = this.textField.value();
        int previousCursor = this.textField.cursor();
        PreeditComposer.PreeditResult result = PreeditComposer.merge(
            previousValue,
            previousCursor,
            this.preeditHandler.composition()
        );
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
