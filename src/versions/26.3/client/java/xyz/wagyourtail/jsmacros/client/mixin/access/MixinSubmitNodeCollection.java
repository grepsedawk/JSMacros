package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceElementCompositionScope;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceLatePhaseAccess;
import xyz.wagyourtail.jsmacros.client.api.classes.render.SurfaceItemSeeThroughScope;

import java.util.ArrayList;
import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection implements SurfaceLatePhaseAccess {
    @Unique private final SimpleFeatureRenderPhase jsmacros$surfaceLatePhase = new SimpleFeatureRenderPhase();
    @Shadow @Final public TranslucentFeatureRenderPhase seeThrough;
    @Shadow @Final public SimpleFeatureRenderPhase alwaysOnTopGizmos;
    @Shadow @Final public SimpleFeatureRenderPhase translucentCustomGeometry;
    @Mutable @Shadow @Final private List<FeatureRenderPhase<?>> allPhases;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void registerSurfaceLatePhase(boolean useImprovedTransparency, TranslucentFeatureRenderPhase seeThrough, CallbackInfo ci) {
        allPhases = new ArrayList<>(allPhases);
        allPhases.add(jsmacros$surfaceLatePhase);
    }

    @Override
    public SimpleFeatureRenderPhase jsmacros$getSurfaceLatePhase() {
        return jsmacros$surfaceLatePhase;
    }

    @Redirect(
            method = "submitItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    )
    private void routeTranslucentItem(FeatureRenderPhase<?> phase, SubmitNode submit) {
        route(phase, submit);
    }

    @Redirect(
            method = "submitItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    )
    private void routeSolidItem(SimpleFeatureRenderPhase phase, SubmitNode submit) {
        route(phase, submit);
    }

    @Redirect(
            method = "submitModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    )
    private void routeTranslucentModel(FeatureRenderPhase<?> phase, SubmitNode submit) {
        route(phase, submit);
    }

    @Redirect(
            method = "submitModel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V", ordinal = 1)
    )
    private void routeSolidModel(SimpleFeatureRenderPhase phase, SubmitNode submit) {
        route(phase, submit);
    }

    @Redirect(
            method = "submitCustomGeometry",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    )
    private void routeScopedCustomGeometry(SimpleFeatureRenderPhase phase, SubmitNode submit) {
        if (SurfaceElementCompositionScope.isLateActive()) {
            jsmacros$surfaceLatePhase.submit(submit);
        } else if (SurfaceItemSeeThroughScope.isActive() && submit instanceof CustomFeatureRenderer.Submit) {
            alwaysOnTopGizmos.submit(submit);
        } else {
            phase.submit(submit);
        }
    }

    @Redirect(
            method = "submitTextPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/TranslucentFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/TranslucentSubmit;)V")
    )
    private void routeScopedText(TranslucentFeatureRenderPhase phase, TranslucentSubmit submit) {
        if (SurfaceElementCompositionScope.isLateActive()) {
            jsmacros$surfaceLatePhase.submit(submit);
        } else if (SurfaceItemSeeThroughScope.isActive()) {
            alwaysOnTopGizmos.submit(submit);
        } else if (SurfaceElementCompositionScope.isActive()) {
            translucentCustomGeometry.submit(submit);
        } else {
            phase.submit(submit);
        }
    }

    @Redirect(
            method = "submitTextPart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    )
    private void routeScopedNormalText(SimpleFeatureRenderPhase phase, SubmitNode submit) {
        if (SurfaceElementCompositionScope.isLateActive()) {
            jsmacros$surfaceLatePhase.submit(submit);
        } else if (SurfaceElementCompositionScope.isActive() && !SurfaceItemSeeThroughScope.isActive()) {
            translucentCustomGeometry.submit(submit);
        } else {
            phase.submit(submit);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void route(FeatureRenderPhase phase, SubmitNode submit) {
        if (SurfaceElementCompositionScope.isLateActive()) {
            jsmacros$surfaceLatePhase.submit(submit);
        } else if (SurfaceItemSeeThroughScope.isActive() && submit instanceof TranslucentSubmit translucentSubmit) {
            seeThrough.submit(translucentSubmit);
        } else {
            phase.submit(submit);
        }
    }
}
