package com.jsmacrosce.jsmacros.client.gui.screens;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.Screen;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.event.impl.EventKey;
import com.jsmacrosce.jsmacros.client.config.ClientConfigV2;
import com.jsmacrosce.jsmacros.client.gui.containers.MacroContainer;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseListener;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.event.IEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

public class KeyMacrosScreen extends MacroScreen {

    public KeyMacrosScreen(Screen parent) {
        super(parent);
    }

    @Override
    public void init() {
        super.init();
        keyScreen.setColor(0x4FFFFFFF);

        Set<IEventListener> listeners = JsMacrosClient.clientCore.eventRegistry.getListeners().get(EventKey.class.getAnnotation(Event.class).value());
        List<ScriptTrigger> macros = new ArrayList<>();

        if (listeners != null) {
            for (IEventListener event : ImmutableList.copyOf(listeners)) {
                if (event instanceof BaseListener && ((BaseListener) event).getRawTrigger().triggerType != ScriptTrigger.TriggerType.EVENT) {
                    macros.add(((BaseListener) event).getRawTrigger());
                }
            }
        }

        macros.sort(JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).getSortComparator());

        for (ScriptTrigger macro : macros) {
            addMacro(macro);
        }
    }

    @Override
    public boolean keyReleased(KeyEvent keyEvent) {
    int modifiers = keyEvent.modifiers();
        String translationKey = EventKey.getKeyModifiers(modifiers);
        if (!translationKey.isEmpty()) {
            translationKey += "+";
        }
        translationKey += InputConstants.getKey(keyEvent).getName();
        for (MacroContainer macro : (List<MacroContainer>) (List) macros) {
            if (!macro.onKey(translationKey)) {
                return false;
            }
        }

        return super.keyReleased(keyEvent);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent buttonEvent) {
        boolean hasShift = buttonEvent.hasShiftDown();
        boolean hasCtrl = buttonEvent.hasControlDown();
        boolean hasAlt = buttonEvent.hasAltDown();
        int button = buttonEvent.button();
        int mods = 0;
        if (hasShift) {
            mods += 1;
        }
        if (hasCtrl) {
            mods += 2;
        }
        if (hasAlt) {
            mods += 4;
        }
        String translationKey = EventKey.getKeyModifiers(mods);
        if (!translationKey.isEmpty()) {
            translationKey += "+";
        }
        translationKey += InputConstants.Type.MOUSE.getOrCreate(button).getName();
        for (MacroContainer macro : (List<MacroContainer>) (List) macros) {
            if (!macro.onKey(translationKey)) {
                return false;
            }
        }
        return super.mouseReleased(buttonEvent);
    }

}
