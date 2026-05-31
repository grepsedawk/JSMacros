package com.jsmacrosce.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.jsmacrosce.jsmacros.util.SlotClickEventHelper;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreen {

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"), cancellable = true)
    public void beforeMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        SlotClickEventHelper.fire((AbstractContainerScreen<?>) (Object) this, actionType.id(), actionType == ContainerInput.THROW, button, slotId, ci);
    }

}
