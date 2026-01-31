package xyz.wagyourtail.jsmacros.client.api.helper.world.entity.specialized.other;

import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import xyz.wagyourtail.jsmacros.client.access.IMixinInteractionEntity;
import xyz.wagyourtail.jsmacros.client.api.helper.world.entity.EntityHelper;
import xyz.wagyourtail.jsmacros.client.mixin.access.MixinInteraction2;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@SuppressWarnings("unused")
public class InteractionEntityHelper extends EntityHelper<Interaction> {

    public InteractionEntityHelper(Interaction base) {
        super(base);
    }

    /**
     * @since 1.9.1
     */
    public void setCanHit(boolean value) {
        ((IMixinInteractionEntity) base).jsmacros_setCanHitOverride(value);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastAttacker() {
        LivingEntity e = base.getLastAttacker();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * @since 1.9.1
     */
    @Nullable
    public EntityHelper<?> getLastInteracted() {
        LivingEntity e = base.getTarget();
        return e == null ? null : EntityHelper.create(e);
    }

    /**
     * @since 1.9.1
     */
    public float getWidth() {
        return ((MixinInteraction2) base).callGetWidth();
    }

    /**
     * @since 1.9.1
     */
    public float getHeight() {
        return ((MixinInteraction2) base).callGetHeight();
    }

    /**
     * @since 1.9.1
     */
    public boolean shouldRespond() {
        return ((MixinInteraction2) base).callGetResponse();
    }

}
