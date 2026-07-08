package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.wagyourtail.jsmacros.client.access.IItemCooldownEntry;
import xyz.wagyourtail.jsmacros.client.access.IItemCooldownManager;

import java.util.Map;

@Mixin(ItemCooldowns.class)
public class MixinItemCooldowns implements IItemCooldownManager {

    @Shadow
    @Final
    private Map<Identifier, IItemCooldownEntry> cooldowns;

    @Shadow
    private int tickCount;

    @Override
    public Map<Identifier, IItemCooldownEntry> jsmacros_getCooldownItems() {
        return cooldowns;
    }

    @Override
    public int jsmacros_getManagerTicks() {
        return tickCount;
    }

}
