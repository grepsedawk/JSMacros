package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import com.google.common.collect.ImmutableSet;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.api.classes.render.IDraw2D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

import java.util.Comparator;

@MixinEnvironment("fabric")
@Mixin(DebugScreenOverlay.class)
class MixinDebugHud {
    // TODO: I didn't want to find the mixin reference, changed in 1.21.9 or 1.21.10
    
}
