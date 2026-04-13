package dev.erudites.mods.koreanify.mixin.components;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EditBox.class)
public interface EditBoxAccessor {

    @Accessor("value")
    void koreanify$value(String value);

    @Accessor("cursorPos")
    int koreanify$cursorPos();

    @Accessor("cursorPos")
    void koreanify$cursorPos(int pos);

    @Accessor("highlightPos")
    int koreanify$highlightPos();

    @Accessor("highlightPos")
    void koreanify$highlightPos(int pos);
}
