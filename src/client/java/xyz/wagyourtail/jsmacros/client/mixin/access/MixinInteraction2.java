package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Interaction.class)
public interface MixinInteraction2 {

    @Invoker
    float callGetWidth();

    @Invoker
    float callGetHeight();

    @Invoker
    boolean callGetResponse();

}
