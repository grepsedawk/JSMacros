package com.jsmacrosce.jsmacros.client.mixin.access;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.jsmacrosce.jsmacros.client.access.IChatHud;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud implements IChatHud {

    @Shadow
    private void addMessage(Component message, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag indicator) {
    }

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Override
    public void jsmacros_addMessageBypass(Component message) {
        addMessage(message, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system());
    }

    @Unique
    ThreadLocal<Integer> jsmacros$positionOverride = ThreadLocal.withInitial(() -> 0);

    @Override
    public void jsmacros_addMessageAtIndexBypass(Component message, int index, int time) {
        jsmacros$positionOverride.set(index);
        addMessage(message, null, GuiMessageSource.SYSTEM_CLIENT, GuiMessageTag.system());
        jsmacros$positionOverride.set(0);
    }

    @Redirect(
            method = "addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;addFirst(Ljava/lang/Object;)V")
    )
    public <E> void overrideMessagePos(List<GuiMessage> instance, E guiMessage) {
        this.allMessages.add(jsmacros$positionOverride.get(), (GuiMessage) guiMessage);
    }


}
