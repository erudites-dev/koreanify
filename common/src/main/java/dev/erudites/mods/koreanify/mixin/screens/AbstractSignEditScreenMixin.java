package dev.erudites.mods.koreanify.mixin.screens;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditComposer;
import dev.erudites.mods.koreanify.client.ime.PreeditHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractSignEditScreen.class)
abstract class AbstractSignEditScreenMixin {

    @Shadow @Final
    protected SignBlockEntity sign;
    @Shadow @Final
    private String[] messages;
    @Shadow
    private int line;
    @Shadow @Nullable
    private TextFieldHelper signField;

    @Unique
    private final PreeditHandler preeditHandler = new PreeditHandler();

    @Inject(method = "preeditUpdated", at = @At("HEAD"), cancellable = true)
    private void koreanify$preeditUpdated(PreeditEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.signField == null) {
            this.preeditHandler.clear();
            cir.setReturnValue(true);
            return;
        }
        this.preeditHandler.handlePreedit(
            event,
            this.messages[this.line],
            this.signField.getCursorPos(),
            this.signField.getSelectionPos(),
            mergedText -> Minecraft.getInstance().font.width(mergedText) <= this.sign.getMaxTextLineWidth(),
            this.signField::insertText,
            null
        );
        cir.setReturnValue(true);
    }

    @WrapMethod(method = "extractSignText")
    private void koreanify$wrapRenderSignText(GuiGraphicsExtractor graphics, Vector2f cursorPosOutput, Operation<Void> original) {
        if (this.preeditHandler.composition().isEmpty() || this.signField == null) {
            original.call(graphics, cursorPosOutput);
            return;
        }
        String previousMessage = this.messages[this.line];
        int previousCursorPos = this.signField.getCursorPos();
        int previousSelectionPos = this.signField.getSelectionPos();
        PreeditComposer.PreeditResult result = PreeditComposer.merge(
            previousMessage,
            previousCursorPos,
            this.preeditHandler.composition()
        );
        this.messages[this.line] = result.text();
        this.signField.setCursorPos(result.cursor());
        this.signField.setSelectionPos(result.cursor());
        try {
            original.call(graphics, cursorPosOutput);
        } finally {
            this.messages[this.line] = previousMessage;
            this.signField.setCursorPos(previousCursorPos);
            this.signField.setSelectionPos(previousSelectionPos);
        }
    }
}
