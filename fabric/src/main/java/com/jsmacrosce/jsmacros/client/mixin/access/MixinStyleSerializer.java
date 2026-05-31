package com.jsmacrosce.jsmacros.client.mixin.access;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Style.Serializer.class, priority = 1001)
public class MixinStyleSerializer {


}
