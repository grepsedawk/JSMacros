package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.projectile;

import com.jsmacrosce.jsmacros.client.api.helper.world.entity.EntityHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinTridentEntity;

import net.minecraft.world.entity.projectile.arrow.ThrownTrident;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class TridentEntityHelper extends EntityHelper<ThrownTrident> {

    public TridentEntityHelper(ThrownTrident base) {
        super(base);
    }

    /**
     * @return {@code true} if the trident is enchanted with loyalty, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean hasLoyalty() {
        return base.getEntityData().get(((MixinTridentEntity) base).getLoyalty()) > 0;
    }

    /**
     * @return {@code true} if the trident is enchanted, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isEnchanted() {
        return base.isFoil();
    }

}
