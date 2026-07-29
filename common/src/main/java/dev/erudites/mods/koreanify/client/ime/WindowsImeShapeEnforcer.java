package dev.erudites.mods.koreanify.client.ime;

import com.mojang.blaze3d.platform.Window;
import dev.erudites.mods.koreanify.client.KoreanifyClientMod;
import dev.erudites.mods.koreanify.client.config.KoreanifyConfig;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class WindowsImeShapeEnforcer {

    private static final int IME_CMODE_FULLSHAPE = 0x0008;
    private static boolean unavailable;

    private WindowsImeShapeEnforcer() {}

    public static void forceHalfwidthIfEnabled(final Window window) {
        if (unavailable
            || Util.getPlatform() != Util.OS.WINDOWS
            || !KoreanifyConfig.get().input.preventWindowsFullwidthSwitching
        ) {
            return;
        }
        try {
            forceHalfwidth(window);
        } catch (Throwable e) {
            unavailable = true;
            KoreanifyClientMod.LOGGER.warn("Failed to enforce Windows IME half-width mode", e);
        }
    }

    private static void forceHalfwidth(final Window window) throws Throwable {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(window.handle());
        if (hwnd == 0L) {
            return;
        }
        MemorySegment windowHandle = MemorySegment.ofAddress(hwnd);
        MemorySegment inputContext = (MemorySegment) Imm32.GET_CONTEXT.invokeExact(windowHandle);
        if (inputContext.address() == 0L) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment conversion = arena.allocate(JAVA_INT);
            MemorySegment sentence = arena.allocate(JAVA_INT);
            int read = (int) Imm32.GET_CONVERSION_STATUS.invokeExact(inputContext, conversion, sentence);
            int conversionValue = conversion.get(JAVA_INT, 0);
            if (read != 0 && (conversionValue & IME_CMODE_FULLSHAPE) != 0) {
                int halfwidth = conversionValue & ~IME_CMODE_FULLSHAPE;
                int sentenceValue = sentence.get(JAVA_INT, 0);
                int _ = (int) Imm32.SET_CONVERSION_STATUS.invokeExact(inputContext, halfwidth, sentenceValue);
            }
        } finally {
            int _ = (int) Imm32.RELEASE_CONTEXT.invokeExact(windowHandle, inputContext);
        }
    }

    private static final class Imm32 {
        private static final Linker LINKER = Linker.nativeLinker();
        private static final SymbolLookup SYMBOLS = SymbolLookup.libraryLookup("imm32", Arena.global());
        private static final MethodHandle GET_CONTEXT = downcall("ImmGetContext", FunctionDescriptor.of(ADDRESS, ADDRESS));
        private static final MethodHandle RELEASE_CONTEXT = downcall("ImmReleaseContext", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
        private static final MethodHandle GET_CONVERSION_STATUS = downcall("ImmGetConversionStatus", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS));
        private static final MethodHandle SET_CONVERSION_STATUS = downcall("ImmSetConversionStatus", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

        private Imm32() {}

        private static MethodHandle downcall(final String name, final FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(SYMBOLS.find(name).orElseThrow(), descriptor);
        }
    }
}
