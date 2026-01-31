package xyz.wagyourtail.jsmacros.client.api.helper.world.entity.specialized.display;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import xyz.wagyourtail.jsmacros.api.math.Vec3D;
import xyz.wagyourtail.jsmacros.client.api.helper.world.entity.EntityHelper;
import xyz.wagyourtail.jsmacros.client.mixin.access.MixinDisplay;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class DisplayEntityHelper<T extends Display> extends EntityHelper<T> {

    public DisplayEntityHelper(T base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetX() {
        return base.getPosition(getLerpProgress(0)).x;
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetY() {
        return base.getPosition(getLerpProgress(0)).y;
    }

    /**
     * @since 1.9.1
     */
    public double getLerpTargetZ() {
        return base.getPosition(getLerpProgress(0)).z;
    }

    /**
     * @since 1.9.1
     */
    public float getLerpTargetPitch() {
        return base.getXRot(getLerpProgress(0));
    }

    /**
     * @since 1.9.1
     */
    public float getLerpTargetYaw() {
        return base.getYRot(getLerpProgress(0));
    }

    /**
     * @since 1.9.1
     */
    public Vec3D getVisibilityBoundingBox() {
        AABB box = base.getBoundingBoxForCulling();
        return new Vec3D(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * @return "fixed", "vertical", "horizontal" or "center"
     * @since 1.9.1
     */
    public String getBillboardMode() {
        return ((MixinDisplay) base).callGetBillboardConstraints().getSerializedName();
    }

    /**
     * @since 1.9.1
     */
    public int getBrightness() {
        Brightness bri = ((MixinDisplay) base).callGetBrightnessOverride();
        return bri == null ? 0 : Math.max(bri.sky(), bri.block());
    }

    /**
     * @since 1.9.1
     */
    public int getSkyBrightness() {
        Brightness bri = ((MixinDisplay) base).callGetBrightnessOverride();
        return bri == null ? 0 : bri.sky();
    }

    /**
     * @since 1.9.1
     */
    public int getBlockBrightness() {
        Brightness bri = ((MixinDisplay) base).callGetBrightnessOverride();
        return bri == null ? 0 : bri.block();
    }

    /**
     * @since 1.9.1
     */
    public float getViewRange() {
        return ((MixinDisplay) base).callGetViewRange();
    }

    /**
     * @since 1.9.1
     */
    public float getShadowRadius() {
        return ((MixinDisplay) base).callGetShadowRadius();
    }

    /**
     * @since 1.9.1
     */
    public float getShadowStrength() {
        return ((MixinDisplay) base).callGetShadowStrength();
    }

    /**
     * @since 1.9.1
     */
    public float getDisplayWidth() {
        return ((MixinDisplay) base).callGetWidth();
    }

    /**
     * @since 1.9.1
     */
    public int getGlowColorOverride() {
        return ((MixinDisplay) base).callGetGlowColorOverride();
    }

    /**
     * @since 1.9.1
     */
    public float getLerpProgress(double delta) {
        return base.calculateInterpolationProgress((float) delta);
    }

    /**
     * @since 1.9.1
     */
    public float getDisplayHeight() {
        return ((MixinDisplay) base).callGetHeight();
    }

}
