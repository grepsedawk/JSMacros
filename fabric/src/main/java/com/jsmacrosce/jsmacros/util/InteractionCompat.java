package com.jsmacrosce.jsmacros.util;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

public final class InteractionCompat {
    private InteractionCompat() {}

    public static InteractionResult interact(MultiPlayerGameMode gameMode, LocalPlayer player, Entity entity, InteractionHand hand) {
        return gameMode.interact(player, entity, new EntityHitResult(entity), hand);
    }
}
