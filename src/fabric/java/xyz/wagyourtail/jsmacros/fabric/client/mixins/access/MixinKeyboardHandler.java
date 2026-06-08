package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;

@Mixin(KeyboardHandler.class)
public class MixinKeyboardHandler {

    @WrapOperation(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z"))
    private boolean onKeyPressed(Screen instance, KeyEvent event, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_keyPressed(event.key(), event.scancode(), event.modifiers());
        return original.call(instance, event);
    }

    @WrapOperation(method = "charTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z"))
    private boolean onCharTyped1(Screen instance, CharacterEvent event, Operation<Boolean> original) {
        ((IScreenInternal) instance).jsmacros_charTyped((char) event.codepoint(), 0);
        return original.call(instance, event);
    }

}
