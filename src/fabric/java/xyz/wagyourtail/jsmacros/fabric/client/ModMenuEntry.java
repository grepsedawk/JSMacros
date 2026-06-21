package xyz.wagyourtail.jsmacros.fabric.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screens.Screen;
import xyz.wagyourtail.jsmacros.client.gui.screens.KeyMacrosScreen;

public class ModMenuEntry implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return KeyMacrosScreen::new;
    }
}
