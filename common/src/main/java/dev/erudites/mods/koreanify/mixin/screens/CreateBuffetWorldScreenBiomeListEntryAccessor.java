package dev.erudites.mods.koreanify.mixin.screens;

import net.minecraft.client.gui.screens.CreateBuffetWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreateBuffetWorldScreen.BiomeList.Entry.class)
public interface CreateBuffetWorldScreenBiomeListEntryAccessor {

    @Accessor("name")
    Component koreanify$name();
}
