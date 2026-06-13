package xyz.wagyourtail.jsmacros.client.api.classes.render.components;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.joml.Matrix3x2fStack;
import xyz.wagyourtail.doclet.DocletIgnore;

/**
 * @author Wagyourtail
 */
public interface RenderElement extends Renderable {

    Minecraft mc = Minecraft.getInstance();

    int getZIndex();

    /**
     * Render this element in 3D world space using PoseStack + MultiBufferSource.
     * Called by Surface for >1.21.5. Default is a no-op.
     */
    @DocletIgnore
    default void render3D(PoseStack matrixStack, MultiBufferSource consumers, int light, boolean seeThrough, SubmitNodeCollector collector, float delta) {}

    /**
     * Converts a packed lightmap value (as returned by {@code LightTexture.pack()}) to a
     * brightness multiplier in [0, 1]. Uses the higher of block and sky light.
     * Full-bright ({@code 0xF000F0}) → {@code 1.0f}; fully dark → {@code 0.0f}.
     */
    @DocletIgnore
    static float lightBrightness(int packedLight) {
        int block = (packedLight >>  4) & 0xF;
        int sky   = (packedLight >> 20) & 0xF;
        return Math.max(block, sky) / 15.0f;
    }

    @DocletIgnore
    default void setupMatrix(Matrix3x2fStack matrices, double x, double y, float scale, float rotation, double width, double height, boolean rotateAroundCenter) {
        matrices.translate((float) x, (float) y);
        matrices.scale(scale, scale);
        if (rotateAroundCenter) {
            matrices.translate((float) (width / 2), (float) (height / 2));
        }
        matrices.rotate((float) Math.toRadians(rotation));
        if (rotateAroundCenter) {
            matrices.translate((float) (-width / 2), (float) (-height / 2));
        }
        matrices.translate((float) -x, (float) -y);
    }

}
