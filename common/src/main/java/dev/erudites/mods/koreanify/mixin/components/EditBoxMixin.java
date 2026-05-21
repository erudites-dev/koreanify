package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditDispatcher;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    @Shadow
    private @Nullable Consumer<String> responder;
    @Shadow
    protected abstract void scrollTo(int pos);
    @Shadow
    public abstract void insertText(String input);

    @Unique
    private final PreeditDispatcher preeditDispatcher = new PreeditDispatcher();

    @Override
    public String koreanify$composition() {
        return this.preeditDispatcher.composition();
    }

    @Inject(method = "setValue", at = @At("HEAD"))
    private void koreanify$clearPreeditOnSetValue(final String value, CallbackInfo ci) {
        if (this.preeditDispatcher.composition().isEmpty()) {
            return;
        }
        this.preeditDispatcher.clear();
        PreeditComposer.resetIme();
    }

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void koreanify$preeditUpdated(final @Nullable PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        this.preeditDispatcher.apply(
            event,
            this.value,
            this.cursorPos,
            this.highlightPos,
            this.maxLength,
            this::insertText,
            this.responder
        );
        cir.setReturnValue(true);
    }

    @WrapMethod(method = "extractWidgetRenderState")
    private void koreanify$wrapExtractWidgetRenderState(
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
        String previousValue = this.value;
        int previousCursor = this.cursorPos;
        int previousHighlight = this.highlightPos;
        int previousDisplay = this.displayPos;
        PreeditComposer.MergeResult result = PreeditComposer.merge(
            this.value,
            this.cursorPos,
            this.preeditDispatcher.composition()
        );
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
