package xyz.wagyourtail.jsmacros.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * @author Etheradon
 * @since 1.6.4
 */
public final class TranslationUtil {

    private TranslationUtil() {
    }

    public static Component getTranslatedEventName(String eventName) {
        String lowerCaseName = eventName.toLowerCase(Locale.ROOT);
        return Language.getInstance().has("jsmacros.event." + lowerCaseName) ? Component.translatable("jsmacros.event." + lowerCaseName) : Component.literal(eventName);
    }

}
