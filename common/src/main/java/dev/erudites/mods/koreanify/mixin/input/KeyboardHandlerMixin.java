package dev.erudites.mods.koreanify.mixin.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import dev.erudites.mods.koreanify.client.ime.WindowsImeShapeEnforcer;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFWCharCallbackI;
import org.lwjgl.glfw.GLFWIMEStatusCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWPreeditCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {

    @WrapOperation(
        method = "setup",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/platform/InputConstants;setupKeyboardCallbacks(Lcom/mojang/blaze3d/platform/Window;Lorg/lwjgl/glfw/GLFWKeyCallbackI;Lorg/lwjgl/glfw/GLFWCharCallbackI;Lorg/lwjgl/glfw/GLFWPreeditCallbackI;Lorg/lwjgl/glfw/GLFWIMEStatusCallbackI;)V"
        )
    )
    private void koreanify$wrapKeyboardCallbacks(
        Window window,
        GLFWKeyCallbackI keyPressCallback,
        GLFWCharCallbackI charTypedCallback,
        GLFWPreeditCallbackI preeditCallback,
        GLFWIMEStatusCallbackI imeStatusCallback,
        Operation<Void> original
    ) {
        original.call(
            window,
            keyPressCallback,
            (GLFWCharCallbackI) (handle, codepoint) -> {
                charTypedCallback.invoke(handle, codepoint);
                if (codepoint == 0x3000 || (codepoint >= 0xFF01 && codepoint <= 0xFF5E)) {
                    WindowsImeShapeEnforcer.forceHalfwidthIfEnabled(window);
                }
            },
            preeditCallback,
            (GLFWIMEStatusCallbackI) handle -> {
                imeStatusCallback.invoke(handle);
                WindowsImeShapeEnforcer.forceHalfwidthIfEnabled(window);
            }
        );
    }
}
