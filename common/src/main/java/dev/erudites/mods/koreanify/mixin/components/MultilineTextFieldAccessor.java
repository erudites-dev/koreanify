package dev.erudites.mods.koreanify.mixin.components;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(MultilineTextField.class)
interface MultilineTextFieldAccessor {

    @Accessor("cursor")
    void koreanify$cursor(int cursor);

    @Accessor("selectCursor")
    int koreanify$selectCursor();

    @Accessor("selectCursor")
    void koreanify$selectCursor(int selectCursor);

    @Accessor("valueListener")
    Consumer<String> koreanify$valueListener();

    @Invoker("overflowsLineLimit")
    boolean koreanify$overflowsLineLimit(String newValue);
}
