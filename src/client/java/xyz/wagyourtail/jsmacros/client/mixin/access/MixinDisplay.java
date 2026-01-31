package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface MixinDisplay {

    @Invoker
    Brightness callGetBrightnessOverride();

    @Invoker
    float callGetViewRange();

    @Invoker
    float callGetShadowRadius();

    @Invoker
    float callGetShadowStrength();

    @Invoker
    float callGetWidth();

    @Invoker
    int callGetGlowColorOverride();

    @Invoker
    float callGetHeight();

    @Invoker
    Display.BillboardConstraints callGetBillboardConstraints();

}
