package xyz.wagyourtail.jsmacros.client.mixin.events;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventDropSlot;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen {

    @Inject(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryMouseClick(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"), cancellable = true)
    public void beforeMouseClick(Slot slot, int slotId, int button, ContainerInput actionType, CallbackInfo ci) {
        EventClickSlot event = new EventClickSlot((AbstractContainerScreen<?>) (Object) this, actionType.ordinal(), button, slotId);
        event.trigger();
        if (event.isCanceled()) {
            ci.cancel();
            return;
        }
        if (actionType == ContainerInput.THROW || slotId == -999) {
            EventDropSlot eventDrop = new EventDropSlot((AbstractContainerScreen<?>) (Object) this, slotId, button == 1);
            eventDrop.trigger();
            if (eventDrop.isCanceled()) {
                ci.cancel();
            }
        }
    }

}
