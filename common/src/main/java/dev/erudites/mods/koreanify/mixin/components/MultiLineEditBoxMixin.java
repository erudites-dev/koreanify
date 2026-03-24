package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
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
    private String composition = "";

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void onPreeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        String fullPreedit = (event != null) ? event.fullText() : "";
        MultilineTextFieldAccessor accessor = (MultilineTextFieldAccessor) this.textField;
        if (fullPreedit.isEmpty()) {
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        String validComposition = PreeditComposer.fitComposition(
            fullPreedit,
            this.textField.value(),
            this.textField.cursor(),
            accessor.getSelectCursor(),
            mergedText -> mergedText.length() <= this.textField.characterLimit() && !accessor.callOverflowsLineLimit(mergedText)
        );
        if (validComposition.isEmpty()) {
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        if (validComposition.length() < fullPreedit.length()) {
            PreeditComposer.commitAndResetIme(validComposition, this.textField::insertText);
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        this.composition = validComposition;
        Consumer<String> valueListener = accessor.getValueListener();
        if (valueListener != null) {
            valueListener.accept(PreeditComposer.merge(this.textField.value(), this.textField.cursor(), this.composition).text());
        }
        cir.setReturnValue(true);
    }

    @WrapMethod(method = "extractContents")
    private void wrapExtractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, Operation<Void> original) {
        if (this.composition.isEmpty()) {
            original.call(graphics, mouseX, mouseY, delta);
            return;
        }
        String previousValue = this.textField.value();
        int previousCursor = this.textField.cursor();
        PreeditComposer.PreeditResult result = PreeditComposer.merge(previousValue, previousCursor, this.composition);
        this.textField.setValue(result.text());
        MultilineTextFieldAccessor accessor = (MultilineTextFieldAccessor) this.textField;
        accessor.setCursor(result.cursor());
        accessor.setSelectCursor(result.cursor());
        try {
            original.call(graphics, mouseX, mouseY, delta);
        } finally {
            this.textField.setValue(previousValue);
            accessor.setCursor(previousCursor);
            accessor.setSelectCursor(previousCursor);
        }
    }
}
