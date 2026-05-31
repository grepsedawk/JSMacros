package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.passive;

import com.jsmacrosce.jsmacros.client.mixin.access.MixinHorseEntity;

import net.minecraft.world.entity.animal.equine.Horse;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class HorseEntityHelper extends AbstractHorseEntityHelper<Horse> {

    public HorseEntityHelper(Horse base) {
        super(base);
    }

    /**
     * @return the variant of this horse.
     * @since 1.8.4
     */
    public int getVariant() {
        return ((MixinHorseEntity) base).invokeGetHorseVariant();
    }

}
