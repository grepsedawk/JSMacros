package xyz.wagyourtail.jsmacros.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import xyz.wagyourtail.jsmacros.client.api.event.impl.EventQuitGame;
import xyz.wagyourtail.jsmacros.client.api.helper.PacketByteBufferHelper;
import xyz.wagyourtail.jsmacros.client.config.ClientConfigV2;
import xyz.wagyourtail.jsmacros.client.config.ClientProfile;
import xyz.wagyourtail.jsmacros.client.event.EventRegistry;
import xyz.wagyourtail.jsmacros.client.gui.screens.KeyMacrosScreen;
import xyz.wagyourtail.jsmacros.client.movement.MovementQueue;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.wagyourgui.BaseScreen;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class JsMacrosClient extends JsMacros {
    public static KeyMapping.Category keyBindingCategory = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("jsmacros", "title"));

    public static KeyMapping keyBinding = new KeyMapping("jsmacros.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, keyBindingCategory);
    public static final Core<ClientProfile, EventRegistry> clientCore = new Core<>(EventRegistry::new, ClientProfile::new, configFolder.getAbsoluteFile(), new File(configFolder, "Macros"), LOGGER);

    public static BaseScreen prevScreen;

    public static void onInitializeClient() {
        try {
            clientCore.config.addOptions("client", ClientConfigV2.class);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException | IOException e) {
            e.printStackTrace();
        }

        prevScreen = new KeyMacrosScreen(null);

        // Init MovementQueue
        MovementQueue.clear();

        if (clientCore.config.getOptions(ClientConfigV2.class).serviceAutoReload) {
            clientCore.services.startReloadListener();
        }
        PacketByteBufferHelper.init();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> new EventQuitGame().trigger()));
    }

    static public Component getKeyText(String translationKey) {
        try {
            return InputConstants.getKey(translationKey).getDisplayName();
        } catch (Exception e) {
            return Component.literal(translationKey);
        }
    }

    // if any screen name is added or modified, check FHud#getOpenScreenName() and Inventory#is() annotation as well
    // i didn't put it here is because doclet won't check this class  -- aMelonRind
    static public String getScreenName(Screen s) {
        return switch (s) {
            case null -> null;
            case AbstractContainerScreen<?> handledScreen -> //add more ?
                switch (handledScreen) {
                    case ContainerScreen genericContainerScreen ->
                        String.format("%d Row Chest", genericContainerScreen.getMenu().getRowCount());
                    case DispenserScreen ignored -> "3x3 Container";
                    case AnvilScreen ignored -> "Anvil";
                    case BeaconScreen ignored -> "Beacon";
                    case BlastFurnaceScreen ignored -> "Blast Furnace";
                    case BrewingStandScreen ignored -> "Brewing Stand";
                    case CraftingScreen ignored -> "Crafting Table";
                    case EnchantmentScreen ignored -> "Enchanting Table";
                    case FurnaceScreen ignored -> "Furnace";
                    case GrindstoneScreen ignored -> "Grindstone";
                    case HopperScreen ignored -> "Hopper";
                    case LoomScreen ignored -> "Loom";
                    case MerchantScreen ignored -> "Villager";
                    case ShulkerBoxScreen ignored -> "Shulker Box";
                    case SmithingScreen ignored -> "Smithing Table";
                    case SmokerScreen ignored -> "Smoker";
                    case CartographyTableScreen ignored -> "Cartography Table";
                    case StonecutterScreen ignored -> "Stonecutter";
                    case InventoryScreen ignored -> "Survival Inventory";
                    case HorseInventoryScreen ignored -> "Horse";
                    case CreativeModeInventoryScreen ignored -> "Creative Inventory";
                    default -> s.getClass().getName();
                };
            case ChatScreen ignored -> "Chat";
            default -> {
                Component t = s.getTitle();
                String ret = "";
                if (t != null) {
                    ret = t.getString();
                }
                if (ret.isEmpty()) {
                    ret = "unknown";
                }
                yield ret;
            }
        };
    }

    @Deprecated
    static public String getLocalizedName(InputConstants.Key keyCode) {
        return I18n.get(keyCode.getName());
    }

    @Deprecated
    static public Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

}
