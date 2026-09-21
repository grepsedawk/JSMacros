package xyz.wagyourtail.jsmacros.client.api.event.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import xyz.wagyourtail.doclet.DocletDeclareType;
import xyz.wagyourtail.doclet.DocletReplaceReturn;
import xyz.wagyourtail.jsmacros.client.JsMacrosClient;
import xyz.wagyourtail.jsmacros.client.access.IRecipeBookWidget;
import xyz.wagyourtail.jsmacros.client.api.library.impl.FKeyBind;
import xyz.wagyourtail.jsmacros.client.config.ClientConfigV2;
import xyz.wagyourtail.jsmacros.core.event.BaseEvent;
import xyz.wagyourtail.jsmacros.core.event.Event;
import xyz.wagyourtail.wagyourgui.BaseScreen;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Wagyourtail
 * @since 1.2.7
 */
@Event(value = "Key", oldName = "KEY", cancellable = true)
public class EventKey extends BaseEvent {
    static final Minecraft mc = Minecraft.getInstance();
    public final int action;

    @DocletReplaceReturn("globalThis.Key")
    public final String key;
    @DocletReplaceReturn("KeyMods")
    @DocletDeclareType(name = "KeyMods", type =
            """
            KeyMod.shift | KeyMod.ctrl | KeyMod.alt
            | `${KeyMod.shift}+${KeyMod.ctrl | KeyMod.alt}`
            | `${KeyMod.ctrl}+${KeyMod.alt}`
            | `${KeyMod.shift}+${KeyMod.ctrl}+${KeyMod.alt}`
            declare namespace KeyMod {
                type shift = 'key.keyboard.left.shift';
                type ctrl = 'key.keyboard.left.control';
                type alt = 'key.keyboard.left.alt';
            }
            """
    )
    public final String mods;

    private static final Set<InputConstants.Key> wasNullOnDown = new HashSet<>();
    private static final InputConstants.Key LEFT_SHIFT = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LSHIFT);
    private static final InputConstants.Key RIGHT_SHIFT = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RSHIFT);
    private static final InputConstants.Key LEFT_CONTROL = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LCONTROL);
    private static final InputConstants.Key RIGHT_CONTROL = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RCONTROL);
    private static final InputConstants.Key LEFT_ALT = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_LALT);
    private static final InputConstants.Key RIGHT_ALT = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_RALT);

    public EventKey(int action, String key, String mods) {
        super(JsMacrosClient.clientCore);
        this.action = action;
        this.key = key;
        this.mods = mods;
    }

    public static boolean parse(InputConstants.Key keycode, int action, int mods) {
        String keyStr = keycode.getName();

        if (keycode == InputConstants.UNKNOWN) {
            return false;
        }

        if (action == InputConstants.PRESS) {
            FKeyBind.KeyTracker.press(keycode);
        } else {
            FKeyBind.KeyTracker.unpress(keycode);
        }

        if (mc.gui.screen() != null) {
            if (action != InputConstants.RELEASE || !wasNullOnDown.contains(keycode)) {
                if (JsMacrosClient.clientCore.config.getOptions(ClientConfigV2.class).disableKeyWhenScreenOpen) {
                    return false;
                }
                if (mc.gui.screen() instanceof BaseScreen) {
                    return false;
                }
                GuiEventListener focused = mc.gui.screen().getFocused();
                if (focused instanceof EditBox) {
                    return false;
                }
                if (focused instanceof RecipeBookComponent && ((IRecipeBookWidget) focused).jsmacros_isSearching()) {
                    return false;
                }
            }
        } else if (action == InputConstants.PRESS) {
            wasNullOnDown.add(keycode);
        }

        if (action == InputConstants.RELEASE) {
            wasNullOnDown.remove(keycode);
        }

        // fix mods if it was a mod key
        if (action == InputConstants.PRESS) {
            if (keycode.equals(LEFT_SHIFT) || keycode.equals(RIGHT_SHIFT)) {
                mods &= ~InputConstants.MOD_SHIFT;
            } else if (keycode.equals(LEFT_CONTROL) || keycode.equals(RIGHT_CONTROL)) {
                mods &= ~InputConstants.MOD_CONTROL;
            } else if (keycode.equals(LEFT_ALT) || keycode.equals(RIGHT_ALT)) {
                mods &= ~InputConstants.MOD_ALT;
            }
        }

        String modsStr = getKeyModifiers(mods);
        EventKey ev = new EventKey(action, keyStr, modsStr);
        ev.trigger();
        return ev.isCanceled();
    }

    @Override
    public String toString() {
        return String.format("%s:{\"key\": \"%s\"}", this.getEventName(), key);
    }

    /**
     * turn an {@link Integer Integer} for key modifiers into a Translation Key.
     *
     * @param mods
     * @return
     */
    public static String getKeyModifiers(int mods) {
        String s = "";
        if ((mods & InputConstants.MOD_SHIFT) != 0) {
            s += "key.keyboard.left.shift";
        }
        if ((mods & InputConstants.MOD_CONTROL) != 0) {
            if (s.length() > 0) {
                s += "+";
            }
            s += "key.keyboard.left.control";
        }
        if ((mods & InputConstants.MOD_ALT) != 0) {
            if (s.length() > 0) {
                s += "+";
            }
            s += "key.keyboard.left.alt";
        }
        return s;
    }

    /**
     * turn a Translation Key for modifiers into an {@link Integer Integer}.
     *
     * @param mods
     * @return
     */
    public static int getModInt(String mods) {
        int i = 0;
        String[] modArr = mods.split("\\+");
        for (String mod : modArr) {
            switch (mod) {
                case "key.keyboard.left.shift":
                case "key.keyboard.right.shift":
                    i |= InputConstants.MOD_SHIFT;
                    break;
                case "key.keyboard.left.control":
                case "key.keyboard.right.control":
                    i |= InputConstants.MOD_CONTROL;
                    break;
                case "key.keyboard.left.alt":
                case "key.keyboard.right.alt":
                    i |= InputConstants.MOD_ALT;
                    break;
                default:
            }
        }
        return i;

    }
}
