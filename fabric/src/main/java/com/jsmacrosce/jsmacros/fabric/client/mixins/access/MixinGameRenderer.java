package com.jsmacrosce.jsmacros.fabric.client.mixins.access;

import dev.kikugie.fletching_table.annotation.MixinEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.access.IScreenInternal;
import com.jsmacrosce.jsmacros.client.api.classes.InteractionProxy;
import com.jsmacrosce.jsmacros.client.api.classes.render.ScriptScreen;

@MixinEnvironment("fabric")
@Mixin(value = GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Redirect(method = "extractGui", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void onRender(Screen instance, GuiGraphicsExtractor drawContext, int mouseX, int mouseY, float delta) {
        instance.extractRenderStateWithTooltipAndSubtitles(drawContext, mouseX, mouseY, delta);
        if (!(minecraft.screen instanceof ScriptScreen)) {
            ((IScreenInternal) instance).jsmacros_render(drawContext, mouseX, mouseY, delta);
        }
    }

}
