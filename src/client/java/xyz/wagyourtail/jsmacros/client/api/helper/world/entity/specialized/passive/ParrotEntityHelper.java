package xyz.wagyourtail.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.world.entity.animal.Parrot;
import xyz.wagyourtail.doclet.DocletReplaceReturn;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ParrotEntityHelper extends TameableEntityHelper<Parrot> {

    public ParrotEntityHelper(Parrot base) {
        super(base);
    }

    /**
     * @return the variant of this parrot.
     * @since 1.8.4
     */
    @DocletReplaceReturn("ParrotVariant")
    public String getVariant() {
        return base.getVariant().getSerializedName();
    }

    /**
     * @return {@code true} if this parrot is sitting, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isSitting() {
        return base.isInSittingPose();
    }

    /**
     * @return {@code true} if this parrot is flying, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isFlying() {
        return base.isFlying();
    }

    /**
     * @return {@code true} if this parrot is dancing to music, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isPartying() {
        return base.isPartyParrot();
    }

    /**
     * @return {@code true} if this parrot is just standing around, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isStanding() {
        return !isPartying() && !isFlying() && !isSitting();
    }

    /**
     * @return always {@code false}, as parrot entities stop existing once on any player's shoulder
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSittingOnShoulder() {
        /* Sad fact:
         *  In newer versions parrots get murdered (discarded) when they sit on a shoulder.
         *  Their NBT lives on the shoulder entity data for the player, and is used to create a new entity.
         *
         * The only way this would ever be true is if you memory leak a removed parrot's entity, which is bad.
         */
        return false;
    }

}
