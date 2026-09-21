package xyz.wagyourtail.jsmacros.client.mixin.access;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceLateFrameAccess;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Inject(
            method = "submitFeatures",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;finalizeGizmoCollection()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void submitJsMacrosDraw3D(
            LevelRenderState levelRenderState,
            SubmitNodeCollector collector,
            boolean renderBlockOutline,
            CallbackInfo ci
    ) {
        try {
            float tickDelta = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            PoseStack matrixStack = new PoseStack();
            for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                d.render(matrixStack, collector, tickDelta);
            }
        } catch (Throwable e) {
            JsMacros.LOGGER.error("Draw3D render error", e);
        }
    }

    @Inject(method = "executeSeeThrough", at = @At("TAIL"))
    private void renderLateSurfaceFeatures(FeatureRenderDispatcher.PreparedFrame preparedFrame, RenderTarget target, CallbackInfo ci) {
        SurfaceLateFrameAccess lateFrame = (SurfaceLateFrameAccess) preparedFrame;
        if (!lateFrame.jsmacros$hasSurfaceLatePhase()) {
            return;
        }
        RenderPassDescriptor descriptor = RenderPassDescriptor.builder(() -> "JsMacros late surface features")
                .withColorAttachment(target.getColorTextureView())
                .build();
        try (var renderPass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(descriptor)) {
            RenderSystem.bindDefaultUniforms(renderPass);
            lateFrame.jsmacros$executeSurfaceLatePhase(renderPass);
        }
    }

}
