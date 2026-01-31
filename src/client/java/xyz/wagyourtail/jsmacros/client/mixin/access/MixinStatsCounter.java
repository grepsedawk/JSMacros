package xyz.wagyourtail.jsmacros.client.mixin.access;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StatsCounter.class)
public interface MixinStatsCounter {
    @Accessor
    Object2IntMap<Stat<?>> getStats();

}
