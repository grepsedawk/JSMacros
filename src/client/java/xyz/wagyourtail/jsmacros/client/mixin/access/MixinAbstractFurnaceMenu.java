package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@Mixin(AbstractFurnaceMenu.class)
public interface MixinAbstractFurnaceMenu {

    @Invoker
    boolean invokeCanSmelt(ItemStack itemStack);

    @Invoker
    boolean invokeIsFuel(ItemStack itemStack);

    @Accessor
    ContainerData getData();

}
