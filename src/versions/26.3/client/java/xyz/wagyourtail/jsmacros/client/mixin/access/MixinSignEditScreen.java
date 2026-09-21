package xyz.wagyourtail.jsmacros.client.mixin.access;

import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xyz.wagyourtail.jsmacros.client.access.ISignEditScreen;

@Mixin(AbstractSignEditScreen.class)
public abstract class MixinSignEditScreen implements ISignEditScreen {

    @Shadow
    @Final
    private String[] messages;

    @Shadow
    @Final
    private SignText.Mutable text;

    @Shadow
    @Nullable
    private TextFieldHelper signField;

    @Shadow
    private int line;

    @Override
    public void jsmacros_setLine(int line, String text) {
        this.messages[line] = text;
        this.text.setLine(line, Component.nullToEmpty(text));
    }

    @Override
    public void jsmacros_fixSelection() {
        int pos = this.messages[this.line].length() + 1;
        if (this.signField != null) this.signField.setCursorPos(pos, false);
    }

}
