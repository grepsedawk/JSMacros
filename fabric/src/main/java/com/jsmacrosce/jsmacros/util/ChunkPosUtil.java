package com.jsmacrosce.jsmacros.util;

import net.minecraft.world.level.ChunkPos;

public final class ChunkPosUtil {
    private ChunkPosUtil() {}

    public static int x(ChunkPos pos) {
        return pos.x();
    }

    public static int z(ChunkPos pos) {
        return pos.z();
    }
}
