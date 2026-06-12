package xyz.wagyourtail.jsmacros.client.mixin.events;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventTitle;

@Mixin(ChatListener.class)
public class MixinChatListener {

    @ModifyVariable(method = "handleOverlay", at = @At("HEAD"), argsOnly = true)
    private Component modifyOverlayMessage(Component text) {
        EventTitle et = new EventTitle("ACTIONBAR", text);
        et.trigger();
        if (et.message == null || et.isCanceled()) {
            return null;
        } else {
            return et.message.getRaw();
        }
    }

}
