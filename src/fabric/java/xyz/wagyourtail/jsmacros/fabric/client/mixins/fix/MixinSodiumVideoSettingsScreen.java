// SPDX-License-Identifier: MIT
package xyz.wagyourtail.jsmacros.fabric.client.mixins.fix;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;
import net.caffeinemc.mods.sodium.client.util.Dim2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(value = VideoSettingsScreen.class, remap = false)
public abstract class MixinSodiumVideoSettingsScreen {

    @Shadow
    public abstract Dim2i getDimensions();

    @WrapOperation(method = {"rebuild", "updateSearchWidgetWidth"}, at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/VideoSettingsScreen;getWidth()I"), require = 0)
    private int jsmacros$layoutWidth(VideoSettingsScreen screen, Operation<Integer> original) {
        return screen.getDimensions().width();
    }

    @WrapOperation(method = "rebuild", at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/gui/VideoSettingsScreen;getHeight()I"), require = 0)
    private int jsmacros$layoutHeight(VideoSettingsScreen screen, Operation<Integer> original) {
        return screen.getDimensions().height();
    }

    @Unique
    public int getLimitX() {
        return getDimensions().getLimitX();
    }

    @Unique
    public int getLimitY() {
        return getDimensions().getLimitY();
    }

    @Unique
    public int getCenterX() {
        return getDimensions().getCenterX();
    }

    @Unique
    public int getCenterY() {
        return getDimensions().getCenterY();
    }

}
