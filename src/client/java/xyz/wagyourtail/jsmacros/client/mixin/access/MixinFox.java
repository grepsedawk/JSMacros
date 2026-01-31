package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.stream.Stream;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(Fox.class)
public interface MixinFox {

    @Invoker
    boolean invokeIsDefending();

    @Invoker
    Stream<EntityReference<LivingEntity>> invokeGetTrustedEntities();

    @Invoker
    boolean invokeTrusts(LivingEntity entity);

}
