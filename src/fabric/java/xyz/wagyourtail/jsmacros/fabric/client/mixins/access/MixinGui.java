package xyz.wagyourtail.jsmacros.fabric.client.mixins.access;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.wagyourtail.jsmacros.client.access.IScreenInternal;
import xyz.wagyourtail.jsmacros.client.api.classes.render.ScriptScreen;

@Mixin(value = Gui.class)
public class MixinGui {
    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void onRender(Screen instance, GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        instance.extractRenderStateWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        if (!(Minecraft.getInstance().gui.screen() instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }

}
