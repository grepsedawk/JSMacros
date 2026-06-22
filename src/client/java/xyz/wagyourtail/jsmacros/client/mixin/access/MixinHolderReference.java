// SPDX-License-Identifier: MIT
package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.wagyourtail.jsmacros.client.api.classes.RegistryHelper;

@Mixin(Holder.Reference.class)
public class MixinHolderReference {

    @Inject(method = "canSerializeIn", at = @At("RETURN"), cancellable = true)
    public void allowSerializeForAllEqualityOwner(HolderOwner<?> owner, CallbackInfoReturnable<Boolean> cir) {
        if (owner == RegistryHelper.ALL_EQUALITY_OWNER) {
            cir.setReturnValue(true);
        }
    }

}
