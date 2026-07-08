package xyz.wagyourtail.jsmacros.client.mixin.access;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw2D;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IDraw2D;
import xyz.wagyourtail.jsmacros.client.api.classes.render.IScreen;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

import java.util.function.Consumer;

@Mixin(Gui.class)
public class MixinGui {

    @Shadow
    private Screen screen;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRenderHud(DeltaTracker tickCounter, boolean renderCrosshair, boolean renderHotbar, CallbackInfo ci, @Local GuiGraphicsExtractor context) {
        if (!FHud.overlays.isEmpty()) {
            for (IDraw2D<Draw2D> overlay : FHud.overlays) {
                overlay.render(context);
            }
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;removed()V"), method = "setScreen")
    public void onCloseScreen(Screen screen, CallbackInfo ci) {
        Consumer<IScreen> onClose = ((IScreen) this.screen).getOnClose();
        try {
            if (onClose != null) onClose.accept((IScreen) screen);
        } catch (Throwable e) {
            JsMacrosClient.clientCore.profile.logError(e);
        }
    }
}
