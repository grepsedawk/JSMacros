package xyz.wagyourtail.jsmacros.client.api.helper;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import xyz.wagyourtail.jsmacros.core.helpers.BaseHelper;

import java.util.Locale;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class FormattingHelper extends BaseHelper<ChatFormatting> {

    public FormattingHelper(ChatFormatting base) {
        super(base);
    }

    /**
     * @return the color value of this formatting.
     * @since 1.8.4
     */
    public int getColorValue() {
        TextColor color = TextColor.fromLegacyFormat(base);
        return color == null ? -1 : color.getValue();
    }

    /**
     * @return the index of this formatting or {@code -1} if this formatting is a modifier.
     * @since 1.8.4
     */
    public int getColorIndex() {
        return base.ordinal();
    }

    /**
     * @return the name of this formatting.
     * @since 1.8.4
     */
    public String getName() {
        return base.name().toLowerCase(Locale.ROOT);
    }

    /**
     * The color code can be used with the paragraph to color text.
     *
     * @return the color code of this formatting.
     * @since 1.8.4
     */
    public char getCode() {
        return base.toString().charAt(1);
    }

    /**
     * @return {@code true} if this formatting is a color, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isColor() {
        return TextColor.fromLegacyFormat(base) != null;
    }

    /**
     * @return {@code true} if this formatting is a modifier, {@code false} otherwise.
     * @since 1.8.4
     */
    public boolean isModifier() {
        return TextColor.fromLegacyFormat(base) == null && base != ChatFormatting.RESET;
    }

    @Override
    public String toString() {
        return String.format("FormattingHelper:{\"index\": %d, \"color\": %d, \"name\": \"%s\", \"code\": \"%s\", \"isColor\": %b, \"isModifier\": %b}", getColorIndex(), getColorValue(), getName(), getCode(), isColor(), isModifier());
    }

}
