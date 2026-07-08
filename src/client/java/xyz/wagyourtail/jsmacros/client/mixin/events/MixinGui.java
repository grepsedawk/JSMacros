package xyz.wagyourtail.jsmacros.client.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventOpenContainer;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventOpenScreen;

@Mixin(Gui.class)
public abstract class MixinGui {

    @Shadow
    private Screen screen;

    @Shadow
    public abstract void setScreen(Screen screen);

    @Unique
    private Screen jsmacros$prevScreen;

    @Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;", opcode = Opcodes.PUTFIELD), method = "setScreen")
    public void onOpenScreen(Screen screen, CallbackInfo info) {
        if (this.screen != screen) {
            jsmacros$prevScreen = screen;
            new EventOpenScreen(screen).trigger();
        }
    }

    @Inject(at = @At("TAIL"), method = "setScreen")
    public void afterOpenScreen(Screen screen, CallbackInfo info) {
        if (screen instanceof AbstractContainerScreen<?>) {
            MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            assert gameMode != null;
            if (gameMode.getPlayerMode().isCreative() && !(screen instanceof CreativeModeInventoryScreen)) {
                return;
            }
            EventOpenContainer event = new EventOpenContainer(((AbstractContainerScreen<?>) screen));
            event.trigger();
            if (event.isCanceled()) {
                setScreen(jsmacros$prevScreen);
            }
        }
        jsmacros$prevScreen = null;
    }
}
