package dev.erudites.mods.koreanify.mixin.components;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Consumer;

@Mixin(MultilineTextField.class)
interface MultilineTextFieldAccessor {

    @Accessor("cursor")
    void setCursor(int cursor);

    @Accessor("selectCursor")
    int getSelectCursor();

    @Accessor("selectCursor")
    void setSelectCursor(int selectCursor);

    @Accessor("valueListener")
    Consumer<String> getValueListener();

    @Invoker("overflowsLineLimit")
    boolean callOverflowsLineLimit(String newValue);
}
