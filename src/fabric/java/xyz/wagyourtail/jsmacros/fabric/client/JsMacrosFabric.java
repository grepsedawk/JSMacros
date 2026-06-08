package xyz.wagyourtail.jsmacros.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import xyz.wagyourtail.jsmacros.client.JsMacros;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.api.classes.inventory.CommandManager;
import xyz.wagyourtail.jsmacros.client.tick.TickBasedEvents;
import xyz.wagyourtail.jsmacros.fabric.client.api.classes.CommandBuilderFabric;
import xyz.wagyourtail.jsmacros.fabric.client.api.classes.CommandManagerFabric;

public class JsMacrosFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitializeClient() {
        JsMacrosClient.onInitializeClient();
        ClientTickEvents.END_CLIENT_TICK.register(TickBasedEvents::onTick);
        KeyMappingHelper.registerKeyMapping(JsMacrosClient.keyBinding);
        CommandBuilderFabric.registerEvent();
    }

    @Override
    public void onInitialize() {
        JsMacros.onInitialize();

        // initialize loader-specific stuff
        CommandManager.instance = new CommandManagerFabric();
    }

}
