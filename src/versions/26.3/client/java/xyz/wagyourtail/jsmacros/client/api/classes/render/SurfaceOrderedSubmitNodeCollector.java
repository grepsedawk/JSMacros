package xyz.wagyourtail.jsmacros.client.api.classes.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Keeps submissions from one surface element in its native ordered-collector bucket.
 */
public final class SurfaceOrderedSubmitNodeCollector implements SubmitNodeCollector {
    private final SubmitNodeCollector root;
    private final int order;

    public SurfaceOrderedSubmitNodeCollector(SubmitNodeCollector root, int order) {
        this.root = root;
        this.order = order;
    }

    private OrderedSubmitNodeCollector delegate() {
        return root.order(order);
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new SurfaceOrderedSubmitNodeCollector(root, Math.addExact(this.order, order));
    }

    @Override
    public void submitShadow(PoseStack poseStack, float shadowRadius, List<EntityRenderState.ShadowPiece> pieces) {
        delegate().submitShadow(poseStack, shadowRadius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, Vec3 offset, int yOffset, Component text, boolean discrete, int light, CameraRenderState camera) {
        delegate().submitNameTag(poseStack, offset, yOffset, text, discrete, light, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode displayMode, int color, int backgroundColor, int light, int overlay) {
        delegate().submitText(poseStack, x, y, text, shadow, displayMode, color, backgroundColor, light, overlay);
    }

    @Override
    public void submitTextBackground(PoseStack poseStack, float x, float y, float width, float height, int color, Font.DisplayMode displayMode, int light) {
        delegate().submitTextBackground(poseStack, x, y, width, height, color, displayMode, light);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {
        delegate().submitFlame(poseStack, state, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState state) {
        delegate().submitLeash(poseStack, state);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, UvMapping uvMapping, int outlineColor) {
        delegate().submitModel(model, state, poseStack, renderType, light, overlay, color, uvMapping, outlineColor);
    }

    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int color, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        delegate().submitCrumblingOverlay(model, state, poseStack, renderType, light, overlay, color, crumblingOverlay);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState state, int light) {
        delegate().submitMovingBlock(poseStack, state, light);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] colors, int light, int overlay, int seed) {
        delegate().submitBlockModel(poseStack, renderType, parts, colors, light, overlay, seed);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int stage, boolean translucent) {
        delegate().submitBreakingBlockModel(poseStack, parts, stage, translucent);
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float lineWidth, boolean seeThrough) {
        delegate().submitShapeOutline(poseStack, shape, renderType, color, lineWidth, seeThrough);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int light, int overlay, int seed, int[] colors, net.minecraft.client.resources.model.geometry.ItemQuads quads, ItemStackRenderState.FoilType foilType) {
        delegate().submitItem(poseStack, displayContext, light, overlay, seed, colors, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
        delegate().submitCustomGeometry(poseStack, renderType, renderer);
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState group) {
        delegate().submitQuadParticleGroup(group);
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean translucent) {
        delegate().submitGizmoPrimitives(group, camera, translucent);
    }
}
