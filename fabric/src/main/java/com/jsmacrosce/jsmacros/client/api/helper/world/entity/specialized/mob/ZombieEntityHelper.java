package com.jsmacrosce.jsmacros.client.api.helper.world.entity.specialized.mob;

import com.jsmacrosce.jsmacros.client.api.helper.world.entity.MobEntityHelper;

import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ZombieEntityHelper<T extends Zombie> extends MobEntityHelper<T> {

    public ZombieEntityHelper(T base) {
        super(base);
    }

    /**
     * @return {@code true} if this zombie is converting to a drowned, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isConvertingToDrowned() {
        return base.isUnderWaterConverting();
    }

}
