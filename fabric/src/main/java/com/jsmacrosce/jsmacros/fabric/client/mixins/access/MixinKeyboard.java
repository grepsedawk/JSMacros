package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

@MixinEnvironment("fabric")
@Mixin(KeyboardHandler.class)
public class MixinKeyboard {
    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean onKeyPressed(Screen instance, KeyEvent keyEvent, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_keyPressed(keyEvent.key(), keyEvent.scancode(), keyEvent.modifiers());
        return original.call(instance, keyEvent);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean onCharTyped1(Screen instance, CharacterEvent characterEvent, Operation<Boolean> original) {
        // 26.1's CharacterEvent is char-only by design — modifier bits only travel with KeyEvent now.
        ((IScreenInternal) instance).jsmacros_charTyped((char) characterEvent.codepoint(), 0);
        return original.call(instance, characterEvent);
    }
}
