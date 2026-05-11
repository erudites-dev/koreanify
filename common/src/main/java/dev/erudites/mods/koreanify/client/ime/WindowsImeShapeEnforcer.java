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

    public static void forceHalfwidthIfEnabled(Window window) {
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

    private static void forceHalfwidth(Window window) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(window.handle());
        if (hwnd == 0L) {
            return;
        }
        MemorySegment windowHandle = MemorySegment.ofAddress(hwnd);
        MemorySegment inputContext = invokeAddress(Imm32.GET_CONTEXT, windowHandle);
        if (inputContext.address() == 0L) {
            return;
        }
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment conversion = arena.allocate(JAVA_INT);
            MemorySegment sentence = arena.allocate(JAVA_INT);
            if (invokeInt(Imm32.GET_CONVERSION_STATUS, inputContext, conversion, sentence) != 0) {
                int conversionValue = conversion.get(JAVA_INT, 0);
                if ((conversionValue & IME_CMODE_FULLSHAPE) != 0) {
                    invokeInt(
                        Imm32.SET_CONVERSION_STATUS,
                        inputContext,
                        conversionValue & ~IME_CMODE_FULLSHAPE,
                        sentence.get(JAVA_INT, 0)
                    );
                }
            }
        } finally {
            invokeInt(Imm32.RELEASE_CONTEXT, windowHandle, inputContext);
        }
    }

    private static MemorySegment invokeAddress(MethodHandle handle, Object... args) {
        try {
            return (MemorySegment) handle.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    private static int invokeInt(MethodHandle handle, Object... args) {
        try {
            return (int) handle.invokeWithArguments(args);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
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

        private static MethodHandle downcall(String name, FunctionDescriptor descriptor) {
            return LINKER.downcallHandle(SYMBOLS.find(name).orElseThrow(), descriptor);
        }
    }
}
