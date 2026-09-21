package xyz.wagyourtail.jsmacros.client.mixin.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventKey;

@Mixin(KeyboardHandler.class)
class MixinKeyboardHandler {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "keyPress", cancellable = true)
    private void onKey(long window, int action, KeyEvent event, final CallbackInfo info) {
        if (window != minecraft.getWindow().handle()) {
            return;
        }
        if (action == InputConstants.REPEAT) {
            return;
        }
        if (EventKey.parse(InputConstants.getKey(event), action, event.modifiers())) {
            info.cancel();
        }
    }

}
