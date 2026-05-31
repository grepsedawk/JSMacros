package com.jsmacrosce.jsmacros.client.mixin.events;

import com.google.common.collect.ImmutableSet;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.client.JsMacros;
import com.jsmacrosce.jsmacros.client.api.classes.render.Draw3D;
import com.jsmacrosce.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinWorldRenderer {

    @Shadow
    private LevelTargetBundle targets;

    // 26.1: engine drains submitNodeStorage inside addMainPass (renderSolidFeatures →
    // renderTranslucentFeatures → clearSubmitNodes at L659) before addWeatherPass runs,
    // so any collector.submitItem(...) from this hook silently no-ops. MBS draws
    // (lines/rects/boxes via bufferSource) still render. Retarget to INVOKE
    // submitBlockDestroyAnimation in addMainPass (shift=AFTER) if Item3D support lands.
    @Shadow
    private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "addWeatherPass", at = @At("TAIL"))
    private void onAddWeatherPass(
            FrameGraphBuilder frameGraphBuilder,
            GpuBufferSlice shaderFog,
            CallbackInfo ci
    ) {
        jsmacrosce_addRenderPass(frameGraphBuilder, Minecraft.getInstance().getDeltaTracker());
    }

    @Unique
    private void jsmacrosce_addRenderPass(FrameGraphBuilder frameGraphBuilder, DeltaTracker deltaTracker) {
        if (this.targets == null) {
            return;
        }

        SubmitNodeStorage capturedStorage = this.submitNodeStorage;

        FramePass framePass = frameGraphBuilder.addPass("jsmacrosce_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            ProfilerFiller profiler = net.minecraft.util.profiling.Profiler.get();
            profiler.push("jsmacrosce_d3d");

            try {
                MultiBufferSource.BufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();
                float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(true);
                PoseStack matrixStack = new PoseStack();

                for (Draw3D d : ImmutableSet.copyOf(FHud.renders)) {
                    d.render(matrixStack, consumers, capturedStorage, tickDelta);
                }

                consumers.endBatch();
            } catch (Throwable e) {
                JsMacros.LOGGER.error("Draw3D render error", e);
            }

            profiler.pop();
        });
    }
}
