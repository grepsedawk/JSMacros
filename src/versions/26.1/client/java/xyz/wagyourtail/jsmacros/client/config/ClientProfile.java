package xyz.wagyourtail.jsmacros.client.config;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.criterion.BlockPredicate;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.DyeColor;
import org.slf4j.Logger;
import xyz.wagyourtail.jsmacros.access.CustomClickEvent;
import xyz.wagyourtail.jsmacros.api.library.FJavaUtils;
import xyz.wagyourtail.jsmacros.api.library.FUtils;
import xyz.wagyourtail.jsmacros.client.access.IChatHud;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventKey;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventLaunchGame;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventMouseScroll;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventQuitGame;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventRecvMessage;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventRecvPacket;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventResourcePackLoaded;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventSendMessage;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventSendPacket;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventTitle;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventClickSlot;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventContainerUpdate;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventDropSlot;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventItemDamage;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventItemPickup;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventOpenContainer;
import xyz.wagyourtail.jsmacros.client.api.event.impl.inventory.EventSlotUpdate;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventAirChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventArmorChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventAttackBlock;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventAttackEntity;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventDamage;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventDeath;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventEXPChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventFallFlying;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventHeal;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventHealthChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventHeldItemChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventHungerChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventInteractBlock;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventInteractEntity;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventOpenScreen;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventRiding;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventSignEdit;
import xyz.wagyourtail.jsmacros.client.api.event.impl.player.EventStatusEffectUpdate;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventBlockUpdate;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventBossbar;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventChunkLoad;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventChunkUnload;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDimensionChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventDisconnect;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventEntityDamaged;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventEntityHealed;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventEntityLoad;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventEntityUnload;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventJoinServer;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventNameChange;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventPlayerJoin;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventPlayerLeave;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventSound;
import xyz.wagyourtail.jsmacros.client.api.event.impl.world.EventTick;
import xyz.wagyourtail.jsmacros.client.api.helper.AdvancementHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.AdvancementManagerHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.AdvancementProgressHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.BlockPredicateHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.CommandNodeHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.DyeColorHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.FormattingHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.InteractionManagerHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.NBTElementHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.NbtPredicateHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.OptionsHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.PacketByteBufferHelper;
import xyz.wagyourtail.jsmacros.client.api.helper.StatePredicateHelper;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FChat;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FClient;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FHud;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FKeyBind;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FPlayer;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FPositionCommon;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FWorld;
import xyz.wagyourtail.jsmacros.client.gui.screens.EditorScreen;
import xyz.wagyourtail.jsmacros.client.gui.screens.MacroScreen;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.BaseProfile;
import xyz.wagyourtail.jsmacros.core.language.BaseScriptContext;
import xyz.wagyourtail.jsmacros.core.language.BaseWrappedException;

import java.util.Arrays;

public class ClientProfile extends BaseProfile {
    private static final Minecraft mc = Minecraft.getInstance();

    public ClientProfile(Core<ClientProfile, ?> runner, Logger logger) {
        super(runner, logger);
    }

    @Override
    protected boolean loadProfile(String profileName) {
        boolean val = super.loadProfile(profileName);
        final Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MacroScreen) {
            mc.execute(() -> ((MacroScreen) mc.screen).reload());
        }
        return val;
    }

    public static Class<? extends Throwable>[] ignoredErrors = new Class[]{
            InterruptedException.class,
            BaseScriptContext.ScriptAssertionError.class,
    };

    @Override
    public void logError(Throwable ex) {
        ex.printStackTrace();
        Throwable finalEx = ex;
        if (Arrays.stream(ignoredErrors).anyMatch(e -> e.isAssignableFrom(finalEx.getClass()))) {
            return;
        }
        if (ex instanceof RuntimeException) {
            if (ex.getCause() != null) {
                Throwable cause = ex.getCause();
                if (Arrays.stream(ignoredErrors).anyMatch(e -> e.isAssignableFrom(cause.getClass()))) {
                    return;
                }
                // un-wrap exceptions
                if (ex.getMessage().equals(ex.getCause().toString())) {
                    ex = ex.getCause();
                }
            }
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui != null) {
            BaseWrappedException<?> e;
            try {
                e = runner.wrapException(ex);
            } catch (Throwable t) {
                t.printStackTrace();
                mc.execute(() -> ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(Component.translatable("jsmacros.errorerror").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED))));
                return;
            }
            Component text = compileError(e);
            mc.execute(() -> {
                try {
                    ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(text);
                } catch (Throwable t) {
                    ((IChatHud) mc.gui.getChat()).jsmacros_addMessageBypass(Component.translatable("jsmacros.errorerror").setStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED)));
                    t.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean checkJoinedThreadStack() {
        return mc.isSameThread() || joinedThreadStack.contains(Thread.currentThread());
    }

    private Component compileError(BaseWrappedException<?> ex) {
        if (ex == null) {
            return null;
        }
        BaseWrappedException<?> head = ex;
        MutableComponent text = Component.literal("");
        do {
            String message = head.message;
            MutableComponent line = Component.literal(message).setStyle(Style.EMPTY.withColor(ChatFormatting.RED));
            if (head.location != null) {
                Style locationStyle = Style.EMPTY.withColor(ChatFormatting.GOLD);
                if (head.location instanceof BaseWrappedException.GuestLocation) {
                    BaseWrappedException.GuestLocation loc = (BaseWrappedException.GuestLocation) head.location;
                    if (loc.file != null) {
                        locationStyle = locationStyle.withHoverEvent(
                                new HoverEvent.ShowText(Component.translatable("jsmacros.clicktoview"))
                        ).withClickEvent(new CustomClickEvent(() -> {
                            if (loc.startIndex > -1) {
                                EditorScreen.openAndScrollToIndex(loc.file, loc.startIndex, loc.endIndex);
                            } else if (loc.line > -1) {
                                EditorScreen.openAndScrollToLine(loc.file, loc.line, loc.column, -1);
                            } else {
                                EditorScreen.openAndScrollToIndex(loc.file, 0, 0);
                            }
                        }));
                    }
                }
                line.append(Component.literal(" (" + head.location + ")").setStyle(locationStyle));
            }
            if ((head = head.next) != null) {
                line.append("\n");
            }
            text.append(line);
        } while (head != null);
        return text;
    }

    @Override
    public void initRegistries() {
        super.initRegistries();

        runner.eventRegistry.addEvent(EventAirChange.class);
        runner.eventRegistry.addEvent(EventArmorChange.class);
        runner.eventRegistry.addEvent(EventAttackBlock.class);
        runner.eventRegistry.addEvent(EventAttackEntity.class);
        runner.eventRegistry.addEvent(EventBlockUpdate.class);
        runner.eventRegistry.addEvent(EventBossbar.class);
        runner.eventRegistry.addEvent(EventChunkLoad.class);
        runner.eventRegistry.addEvent(EventChunkUnload.class);
        runner.eventRegistry.addEvent(EventContainerUpdate.class);
        runner.eventRegistry.addEvent(EventClickSlot.class);
        runner.eventRegistry.addEvent(EventDamage.class);
        runner.eventRegistry.addEvent(EventHeal.class);
        runner.eventRegistry.addEvent(EventDeath.class);
        runner.eventRegistry.addEvent(EventDimensionChange.class);
        runner.eventRegistry.addEvent(EventDisconnect.class);
        runner.eventRegistry.addEvent(EventDropSlot.class);
        runner.eventRegistry.addEvent(EventEntityDamaged.class);
        runner.eventRegistry.addEvent(EventEntityHealed.class);
        runner.eventRegistry.addEvent(EventEntityLoad.class);
        runner.eventRegistry.addEvent(EventEntityUnload.class);
        runner.eventRegistry.addEvent(EventEXPChange.class);
        runner.eventRegistry.addEvent(EventFallFlying.class);
        runner.eventRegistry.addEvent(EventHealthChange.class);
        runner.eventRegistry.addEvent(EventHeldItemChange.class);
        runner.eventRegistry.addEvent(EventHungerChange.class);
        runner.eventRegistry.addEvent(EventInteractBlock.class);
        runner.eventRegistry.addEvent(EventInteractEntity.class);
        runner.eventRegistry.addEvent(EventItemDamage.class);
        runner.eventRegistry.addEvent(EventItemPickup.class);
        runner.eventRegistry.addEvent(EventRecvPacket.class);
        runner.eventRegistry.addEvent(EventSendPacket.class);
        runner.eventRegistry.addEvent(EventJoinServer.class);
        runner.eventRegistry.addEvent(EventKey.class);
        runner.eventRegistry.addEvent(EventLaunchGame.class);
        runner.eventRegistry.addEvent(EventMouseScroll.class);
        runner.eventRegistry.addEvent(EventNameChange.class);
        runner.eventRegistry.addEvent(EventOpenContainer.class);
        runner.eventRegistry.addEvent(EventOpenScreen.class);
        runner.eventRegistry.addEvent(EventRecvPacket.class);
        runner.eventRegistry.addEvent(EventSendPacket.class);
        runner.eventRegistry.addEvent(EventPlayerJoin.class);
        runner.eventRegistry.addEvent(EventPlayerLeave.class);
        runner.eventRegistry.addEvent(EventQuitGame.class);
        runner.eventRegistry.addEvent(EventRecvMessage.class);
        runner.eventRegistry.addEvent(EventRiding.class);
        runner.eventRegistry.addEvent(EventResourcePackLoaded.class);
        runner.eventRegistry.addEvent(EventSendMessage.class);
        runner.eventRegistry.addEvent(EventSignEdit.class);
        runner.eventRegistry.addEvent(EventSlotUpdate.class);
        runner.eventRegistry.addEvent(EventSound.class);
        runner.eventRegistry.addEvent(EventStatusEffectUpdate.class);
        runner.eventRegistry.addEvent(EventTick.class);
        runner.eventRegistry.addEvent(EventTitle.class);

        runner.libraryRegistry.addLibrary(FChat.class);
        runner.libraryRegistry.addLibrary(FHud.class);
        runner.libraryRegistry.addLibrary(FClient.class);
        runner.libraryRegistry.addLibrary(FKeyBind.class);
        runner.libraryRegistry.addLibrary(FPlayer.class);
        runner.libraryRegistry.addLibrary(FPositionCommon.class);
        runner.libraryRegistry.addLibrary(FJavaUtils.class);
        runner.libraryRegistry.addLibrary(FUtils.class);
        runner.libraryRegistry.addLibrary(FWorld.class);

        runner.registerHelper(AdvancementNode.class, AdvancementHelper.class);
        runner.registerHelper(AdvancementTree.class, AdvancementManagerHelper.class);
        runner.registerHelper(AdvancementProgress.class, AdvancementProgressHelper.class);
        runner.registerHelper(BlockPredicate.class, BlockPredicateHelper.class);
//        runner.registerHelper(CommandContext.class, CommandContextHelper.class);
        runner.registerHelper(CommandNode.class, CommandNodeHelper.class);
        runner.registerHelper(DyeColor.class, DyeColorHelper.class);
        runner.registerHelper(ChatFormatting.class, FormattingHelper.class);
        runner.registerHelper(MultiPlayerGameMode.class, InteractionManagerHelper.class);
        runner.helperRegistry.registerType(Tag.class, NBTElementHelper::wrap);
        runner.registerHelper(NumericTag.class, NBTElementHelper.NBTNumberHelper.class);
        runner.registerHelper(CompoundTag.class, NBTElementHelper.NBTCompoundHelper.class);
        runner.registerHelper(CollectionTag.class, (Class) NBTElementHelper.NBTListHelper.class);
        runner.registerHelper(NbtPredicate.class, NbtPredicateHelper.class);
        runner.registerHelper(Options.class, OptionsHelper.class);
        runner.registerHelper(FriendlyByteBuf.class, PacketByteBufferHelper.class);
        runner.registerHelper(StatePropertiesPredicate.class, StatePredicateHelper.class);
        // TODO: complete list

    }

}
