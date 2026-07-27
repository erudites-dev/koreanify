package dev.erudites.mods.koreanify.mixin.components;

import dev.erudites.mods.koreanify.client.ime.PreeditOverlayState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IMEPreeditOverlay.class)
abstract class IMEPreeditOverlayMixin implements PreeditOverlayState {

    @Unique
    private boolean inlined;

    @Override
    public boolean koreanify$inlined() {
        return this.inlined;
    }

    @Override
    public void koreanify$markInlined() {
        this.inlined = true;
    }

    @Inject(
        method = "extractRenderState",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/TextInputManager;setTextInputArea(IIII)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void koreanify$skipInlinedOverlay(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY,
        final float delta,
        CallbackInfo ci
    ) {
        if (this.inlined) {
            ci.cancel();
        }
    }
}
