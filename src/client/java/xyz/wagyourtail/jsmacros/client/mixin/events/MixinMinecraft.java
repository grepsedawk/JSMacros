package xyz.wagyourtail.jsmacros.client.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventLaunchGame;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDimensionChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDisconnect;
import xyz.wagyourtail.jsmacros.client.mixin.access.MixinDisconnectedScreen;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    @Final
    private User user;

    @Inject(at = @At("HEAD"), method = "setLevel")
    public void onJoinWorld(ClientLevel world, CallbackInfo ci) {
        if (world != null) {
            new EventDimensionChange(world.dimension().identifier().toString()).trigger();
        }
    }

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;isLocalServer:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER, remap = false), method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V")
    public void onDisconnect(Screen s, boolean keepResourcePacks, boolean stopSound, CallbackInfo ci) {
        if (s instanceof DisconnectedScreen) {
            new EventDisconnect(((MixinDisconnectedScreen) s).getDetails().reason()).trigger();
        } else {
            new EventDisconnect(null).trigger();
        }
    }

    @Inject(at = @At("HEAD"), method = "run")
    private void onStart(CallbackInfo ci) {
        new EventLaunchGame(this.user.getName()).trigger();
    }

}
