package xyz.wagyourtail.jsmacros.client.mixin.access;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceLateFrameAccess;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceLatePhaseAccess;

import java.util.ArrayList;
import java.util.List;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public abstract class MixinPreparedFrame implements SurfaceLateFrameAccess {
    @Shadow private FeatureFrameContext context;
    @Unique private final List<FeatureRenderPhase<?>> jsmacros$surfaceLatePhases = new ArrayList<>();

    @Invoker("executePhase")
    abstract void jsmacros$executePhase(FeatureRenderPhase<?> phase, FeatureFrameContext context, RenderPass renderPass);

    @Inject(method = "begin", at = @At("TAIL"))
    private void captureSurfaceLatePhases(FeatureFrameContext context, SubmitNodeStorage storage,
                                         CallbackInfoReturnable<FeatureRenderDispatcher.PreparedFrame> cir) {
        jsmacros$surfaceLatePhases.clear();
        // Preparing the frame drains these queues; retain their identities in native bucket order.
        for (var collection : storage.getSubmitsPerOrder().values()) {
            var phase = ((SurfaceLatePhaseAccess) collection).jsmacros$getSurfaceLatePhase();
            if (!phase.isEmpty()) {
                jsmacros$surfaceLatePhases.add(phase);
            }
        }
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void clearSurfaceLatePhases(CallbackInfo ci) {
        jsmacros$surfaceLatePhases.clear();
    }

    @Inject(method = "hasAnySeeThrough", at = @At("RETURN"), cancellable = true)
    private void includeSurfaceLatePhase(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && jsmacros$hasSurfaceLatePhase()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean jsmacros$hasSurfaceLatePhase() {
        return !jsmacros$surfaceLatePhases.isEmpty();
    }

    @Override
    public void jsmacros$executeSurfaceLatePhase(RenderPass renderPass) {
        for (var phase : jsmacros$surfaceLatePhases) {
            jsmacros$executePhase(phase, context, renderPass);
        }
    }
}
