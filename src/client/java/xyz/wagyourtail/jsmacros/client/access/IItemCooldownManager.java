package xyz.wagyourtail.jsmacros.client.access;

import net.minecraft.resources.Identifier;

import java.util.Map;

public interface IItemCooldownManager {

    Map<Identifier, IItemCooldownEntry> jsmacros_getCooldownItems();

    int jsmacros_getManagerTicks();

}
