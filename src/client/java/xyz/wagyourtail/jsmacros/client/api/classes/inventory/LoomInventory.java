package xyz.wagyourtail.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BannerPattern;
import xyz.wagyourtail.jsmacros.client.access.ILoomScreen;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 1.5.1
 */
@SuppressWarnings("unused")
public class LoomInventory extends Inventory<LoomScreen> {

    protected LoomInventory(LoomScreen inventory) {
        super(inventory);
    }

    /**
     * @param name
     * @return success
     * @since 1.5.1
     */
    @Deprecated
    public boolean selectPatternName(String name) {
        throw new NullPointerException("This method is deprecated. Use selectPatternId instead.");
    }

    /**
     * @return available pattern ids
     * @since 1.7.0
     */
    public List<String> listAvailablePatterns() {
        var registry = Objects.requireNonNull(mc.getConnection()).registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        return inventory.getMenu().getSelectablePatterns().stream().map(e -> registry.getKey(e.value()).toString()).collect(Collectors.toList());
    }

    /**
     * @param id
     * @return success
     * @since 1.5.1
     */
    public boolean selectPatternId(String id) {
        Holder<BannerPattern> pattern = Objects.requireNonNull(mc.getConnection())
                .registryAccess()
                .lookupOrThrow(Registries.BANNER_PATTERN)
                .get(Identifier.parse(id)).orElse(null);

        int iid = inventory.getMenu().getSelectablePatterns().indexOf(pattern);
        if (pattern != null && ((ILoomScreen) inventory).jsmacros_canApplyDyePattern() &&
                inventory.getMenu().clickMenuButton(player, iid)) {
            assert mc.gameMode != null;
            mc.gameMode.handleInventoryButtonClick(syncId, iid);
            return true;
        }
        return false;
    }

    /**
     * @param index
     * @return success
     * @since 1.5.1
     */
    public boolean selectPattern(int index) {
        List<Holder<BannerPattern>> patterns = inventory.getMenu().getSelectablePatterns();

        if (index >= 0 && index <= patterns.size() && ((ILoomScreen) inventory).jsmacros_canApplyDyePattern() &&
                inventory.getMenu().clickMenuButton(player, index)) {
            assert mc.gameMode != null;
            mc.gameMode.handleInventoryButtonClick(syncId, index);
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("LoomInventory:{}");
    }

}
