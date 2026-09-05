package dev.erudites.mods.koreanify.mixin.components;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.erudites.mods.koreanify.client.ime.PreeditOverlayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.IMEPreeditOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(IMEPreeditOverlay.class)
abstract class IMEPreeditOverlayMixin implements PreeditOverlayState {

    @Shadow
    private int inputLeft;
    @Shadow
    private int inputTop;
    @Shadow @Final
    private int inputHeight;
    @Shadow @Final
    private int preEditTextWidth;

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

    @WrapMethod(method = "extractRenderState")
    private void koreanify$anchorOverlayToComposition(
        final GuiGraphicsExtractor graphics,
        final int mouseX,
        final int mouseY,
        final float delta,
        Operation<Void> original
    ) {
        if (!this.inlined) {
            original.call(graphics, mouseX, mouseY, delta);
            return;
        }
        Minecraft.getInstance().textInputManager().setTextInputArea(
            this.inputLeft - this.preEditTextWidth,
            this.inputTop,
            this.inputLeft,
            this.inputTop + this.inputHeight
        );
    }
}
