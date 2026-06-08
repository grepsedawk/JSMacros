package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Rendering overlays as part of f3 seems useless, they already get rendered elsewhere.
 * @see {@link xyz.wagyourtail.jsmacros.client.mixin.access.MixinGui}
 */
@Mixin(DebugScreenOverlay.class)
class MixinDebugScreenOverlay {
    /*@Inject(
            method = "drawGameInformation(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V",
            at = @At("TAIL")
    )
    private void afterDrawLeftText(GuiGraphicsExtractor context, CallbackInfo ci) {
        DebugScreenOverlay self = (DebugScreenOverlay) (Object) this;
        if (!self.showDebugScreen()) return;

        ImmutableSet.copyOf(FHud.overlays).stream()
                .sorted(Comparator.comparingInt(IDraw2D::getZIndex))
                .forEachOrdered(hud -> hud.render(context));
    }*/
}
