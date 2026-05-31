package com.jsmacrosce.jsmacros.client.api.classes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.server.permissions.PermissionSet;

/**
 * @author Etheradon
 * @since 1.8.4
 */
public class FakeServerCommandSource extends CommandSourceStack {

    private final ClientSuggestionProvider source;

    public FakeServerCommandSource(ClientSuggestionProvider source, LocalPlayer player) {
        // TODO: (1.21.11) These are specified as NotNull, will this fail?
        super(null, player.position(), player.getRotationVector(), null, PermissionSet.ALL_PERMISSIONS, player.getName().getString(), player.getDisplayName(), null, player);
        this.source = source;
    }

    @Override
    public Collection<String> getSelectedEntities() {
        return source.getSelectedEntities();
    }

    @Override
    public Collection<String> getCustomTabSuggestions() {
        return source.getCustomTabSuggestions();
    }

    @Override
    public Collection<String> getOnlinePlayerNames() {
        return source.getOnlinePlayerNames();
    }

    @Override
    public Collection<String> getAllTeams() {
        return source.getAllTeams();
    }

    @Override
    public Stream<Identifier> getAvailableSounds() {
        return source.getAvailableSounds();
    }

    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
        return source.customSuggestion(context);
    }

    @Override
    public Collection<TextCoordinates> getRelevantCoordinates() {
        return source.getRelevantCoordinates();
    }

    @Override
    public Collection<TextCoordinates> getAbsoluteCoordinates() {
        return source.getAbsoluteCoordinates();
    }

    @Override
    public Set<ResourceKey<Level>> levels() {
        return source.levels();
    }

    @Override
    public RegistryAccess registryAccess() {
        return source.registryAccess();
    }

    @Override
    public void sendSuccess(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
        Minecraft.getInstance().player.sendSystemMessage(feedbackSupplier.get());
    }

}
