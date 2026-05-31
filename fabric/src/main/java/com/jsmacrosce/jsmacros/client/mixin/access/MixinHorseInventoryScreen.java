package com.jsmacrosce.jsmacros.client.mixin.access;

// Dummy class to avoid mixin errors
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HorseInventoryScreen.class)
public class MixinHorseInventoryScreen {}