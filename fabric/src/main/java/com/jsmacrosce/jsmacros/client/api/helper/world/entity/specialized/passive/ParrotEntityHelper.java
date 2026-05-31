package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import com.jsmacrosce.doclet.DocletReplaceReturn;

import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.world.entity.animal.parrot.Parrot;

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
     * @apiNote In 1.21.9 and beyond, parrot entities are removed when on player shoulders so this will always return
     * false.
     * @return {@code true} if this parrot is sitting on any player's shoulder, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isSittingOnShoulder() {
        if (!isSitting()) return false;
        return false;
    }
}
