package xyz.wagyourtail.jsmacros.client.mixin.access;

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
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.api.classes.render.Draw3D;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {

    @Shadow
    private LevelTargetBundle targets;

    @Shadow
    private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "addMainPass", at = @At("TAIL"))
    private void onRenderMain(
            FrameGraphBuilder frame,
            Frustum frustum,
            Matrix4fc modelViewMatrix,
            GpuBufferSlice terrainFog,
            boolean renderOutline,
            LevelRenderState levelRenderState,
            DeltaTracker deltaTracker,
            ProfilerFiller profiler,
            ChunkSectionsToRender chunkSectionsToRender,
            CallbackInfo ci
    ) {
        if (this.targets == null) {
            return;
        }
        // Capture the live submit-node storage: addMainPass still has it before the engine
        // drains it (clearSubmitNodes), so collector-based draws (e.g. Item on a Surface)
        // render this frame. Injecting later (addWeatherPass) would no-op those.
        SubmitNodeStorage capturedStorage = this.submitNodeStorage;
        FramePass framePass = frame.addPass("jsmacros_draw3d");
        LevelTargetBundle frameBufferSet = this.targets;
        frameBufferSet.main = framePass.readsAndWrites(frameBufferSet.main);

        framePass.executes(() -> {
            profiler.push("jsmacros_d3d");

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