package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.PreeditEvent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(EditBox.class)
abstract class EditBoxMixin implements PreeditState {

    @Shadow
    private String value;
    @Shadow
    private int maxLength;
    @Shadow
    private int displayPos;
    @Shadow
    private int cursorPos;
    @Shadow
    private int highlightPos;
    @Shadow @Nullable
    private Consumer<String> responder;
    @Shadow
    protected abstract void scrollTo(int pos);
    @Shadow
    public abstract void insertText(String text);

    @Unique
    private String composition = "";

    @Override
    public String koreanify$composition() {
        return this.composition;
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void onPreeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        String fullPreedit = (event != null) ? event.fullText() : "";
        if (fullPreedit.isEmpty()) {
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        int availableSpace = PreeditComposer.availableSpace(this.value.length(), this.cursorPos, this.highlightPos, this.maxLength);
        if (availableSpace == 0) {
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        if (fullPreedit.length() > availableSpace) {
            PreeditComposer.commitAndResetIme(fullPreedit.substring(0, availableSpace), this::insertText);
            this.composition = "";
            cir.setReturnValue(true);
            return;
        }
        this.composition = fullPreedit;
        if (this.responder != null) {
            this.responder.accept(PreeditComposer.merge(this.value, this.cursorPos, this.composition).text());
        }
        cir.setReturnValue(true);
    }

    @WrapMethod(method = "extractWidgetRenderState")
    private void wrapExtractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, Operation<Void> original) {
        if (this.composition.isEmpty()) {
            original.call(graphics, mouseX, mouseY, delta);
            return;
        }
        String previousValue = this.value;
        int previousCursor = this.cursorPos;
        int previousHighlight = this.highlightPos;
        int previousDisplay = this.displayPos;
        PreeditComposer.PreeditResult result = PreeditComposer.merge(this.value, this.cursorPos, this.composition);
        this.value = result.text();
        this.cursorPos = result.cursor();
        this.highlightPos = result.cursor();
        this.scrollTo(this.cursorPos);
        try {
            original.call(graphics, mouseX, mouseY, delta);
        } finally {
            this.value = previousValue;
            this.cursorPos = previousCursor;
            this.highlightPos = previousHighlight;
            this.displayPos = previousDisplay;
        }
    }
}
