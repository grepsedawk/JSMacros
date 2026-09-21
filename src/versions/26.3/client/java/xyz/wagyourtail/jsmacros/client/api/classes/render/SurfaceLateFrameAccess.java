package xyz.wagyourtail.jsmacros.client.api.classes.render;

import com.mojang.renderpearl.api.commands.RenderPass;

public interface SurfaceLateFrameAccess {
    boolean jsmacros$hasSurfaceLatePhase();
    void jsmacros$executeSurfaceLatePhase(RenderPass renderPass);
}
