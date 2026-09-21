package xyz.wagyourtail.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import xyz.wagyourtail.jsmacros.client.api.helper.inventory.ItemStackHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class BrewingStandInventory extends Inventory<BrewingStandScreen> {

    private static final long RECIPE_QUERY_TIMEOUT_SECONDS = 1;

    public BrewingStandInventory(BrewingStandScreen inventory) {
        super(inventory);
    }

    /**
     * @param potion the potion to check
     * @return {@code true} if the given potion is can be brewed, {@code false} otherwise. On a
     * remote server, this checks the server-synchronized brewing-input set.
     * @since 1.8.4
     */
    public boolean isBrewablePotion(ItemStackHelper potion) {
        if (mc.hasSingleplayerServer()) {
            ItemStack potionStack = potion.getRaw().copy();
            return withLocalRecipes((recipeManager, level) -> recipeManager.getRecipes().stream()
                    .map(RecipeHolder::value)
                    .filter(BrewingRecipe.class::isInstance)
                    .map(BrewingRecipe.class::cast)
                    .anyMatch(recipe -> recipe.getInput().test(potionStack))
            );
        }
        return getSyncedRecipes().propertySet(RecipePropertySet.BREWING_INPUTS).test(potion.getRaw());
    }

    /**
     * @param ingredient the item to check
     * @return {@code true} if the given item is a valid ingredient, {@code false} otherwise. On a
     * remote server, this checks the server-synchronized brewing-reagent set.
     * @since 1.8.4
     */
    public boolean isValidIngredient(ItemStackHelper ingredient) {
        if (ingredient.isEmpty()) {
            return false;
        }
        if (mc.hasSingleplayerServer()) {
            ItemStack ingredientStack = ingredient.getRaw().copy();
            return withLocalRecipes((recipeManager, level) -> recipeManager.getRecipes().stream()
                    .map(RecipeHolder::value)
                    .filter(BrewingRecipe.class::isInstance)
                    .map(BrewingRecipe.class::cast)
                    .anyMatch(recipe -> recipe.getReagent().test(ingredientStack))
            );
        }
        return getSyncedRecipes().propertySet(RecipePropertySet.BREWING_REAGENTS).test(ingredient.getRaw());
    }

    /**
     * @param potion     the potion to check
     * @param ingredient the ingredient to check
     * @return {@code true} if the given potion and ingredient can be brewed together, {@code false}
     * otherwise.
     * @throws UnsupportedOperationException if the active server does not expose its brewing
     *                                       recipe pairs to this client.
     * @since 1.8.4
     */
    public boolean isValidRecipe(ItemStackHelper potion, ItemStackHelper ingredient) {
        if (potion.isEmpty() || ingredient.isEmpty()) {
            return false;
        }
        if (!mc.hasSingleplayerServer() && (!isBrewablePotion(potion) || !isValidIngredient(ingredient))) {
            return false;
        }
        return getLocalBrewingRecipe(potion.getRaw().copy(), ingredient.getRaw().copy()).isPresent();
    }

    /**
     * @return the left fuel.
     * @since 1.8.4
     */
    public int getFuelCount() {
        return inventory.getMenu().getFuel();
    }

    /**
     * @return the maximum fuel.
     * @since 1.8.4
     */
    public int getMaxFuelUses() {
        return inventory.getMenu().getTotalFuel();
    }

    /**
     * @return {@code true} if the brewing stand can brew any of the held potions with the current
     * ingredient, {@code false} otherwise.
     * @throws UnsupportedOperationException if the active server does not expose its brewing
     *                                       recipe pairs to this client.
     * @since 1.8.4
     */
    public boolean canBrewCurrentInput() {
        ItemStackHelper ingredient = getIngredient();
        if (ingredient.isEmpty()) {
            return false;
        } else if (!isValidIngredient(ingredient)) {
            return false;
        } else {
            for (ItemStackHelper stack : getPotions()) {
                if (!stack.isEmpty() && isValidRecipe(stack, ingredient)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @return the time the potions have been brewing.
     * @since 1.8.4
     */
    public int getBrewTime() {
        return inventory.getMenu().getTotalBrewingTicks() - inventory.getMenu().getBrewingTicks();
    }

    /**
     * @return the remaining time the potions have to brew.
     * @since 1.8.4
     */
    public int getRemainingTicks() {
        return inventory.getMenu().getBrewingTicks();
    }

    /**
     * @param potion     the potion
     * @param ingredient the ingredient
     * @return the resulting potion of the given potion and ingredient if it exists and the potion
     * itself otherwise.
     * @throws UnsupportedOperationException if the active server does not expose its brewing
     *                                       recipe pairs to this client.
     * @since 1.8.4
     */
    public ItemStackHelper previewPotion(ItemStackHelper potion, ItemStackHelper ingredient) {
        if (potion.isEmpty() || ingredient.isEmpty()) {
            return potion;
        }
        if (mc.hasSingleplayerServer()) {
            return new ItemStackHelper(getLocalBrewingResult(potion.getRaw().copy(), ingredient.getRaw().copy()));
        }
        if (!isBrewablePotion(potion) || !isValidIngredient(ingredient)) {
            return potion;
        }
        return new ItemStackHelper(getLocalBrewingResult(potion.getRaw().copy(), ingredient.getRaw().copy()));
    }

    /**
     * @return a list of all resulting potions of the current input.
     * @throws UnsupportedOperationException if the active server does not expose its brewing
     *                                       recipe pairs to this client.
     * @since 1.8.4
     */
    public List<ItemStackHelper> previewPotions() {
        ItemStack ingredient = getIngredient().getRaw();
        return getPotions().stream().map(stack -> previewPotion(stack, new ItemStackHelper(ingredient))).collect(Collectors.toList());
    }

    private RecipeAccess getSyncedRecipes() {
        return Objects.requireNonNull(mc.getConnection()).recipes();
    }

    private <T> T withLocalRecipes(BiFunction<RecipeManager, ServerLevel, T> action) {
        if (mc.level == null || !mc.hasSingleplayerServer()) {
            throw new UnsupportedOperationException("Brewing recipe pairs are not synchronized to the client.");
        }
        IntegratedServer server = mc.getSingleplayerServer();
        ResourceKey<Level> dimension = mc.level.dimension();
        if (server.isSameThread()) {
            return applyLocalRecipeAction(server, dimension, action);
        }
        try {
            return server.submit(() -> applyLocalRecipeAction(server, dimension, action)).get(RECIPE_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while querying integrated-server brewing recipes.", exception);
        } catch (TimeoutException exception) {
            throw new IllegalStateException("Timed out while querying integrated-server brewing recipes.", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Unable to query integrated-server brewing recipes.", exception.getCause());
        }
    }

    private Optional<BrewingRecipe> getLocalBrewingRecipe(ItemStack potion, ItemStack ingredient) {
        return withLocalRecipes((recipeManager, level) -> recipeManager
            .getRecipeFor(RecipeType.BREWING, new BrewingInput(potion, ingredient), level)
            .map(RecipeHolder::value));
    }

    private ItemStack getLocalBrewingResult(ItemStack potion, ItemStack ingredient) {
        return withLocalRecipes((recipeManager, level) -> recipeManager
            .getRecipeFor(RecipeType.BREWING, new BrewingInput(potion, ingredient), level)
            .map(recipe -> recipe.value().assemble(new BrewingInput(potion, ingredient)))
            .orElse(potion));
    }

    private <T> T applyLocalRecipeAction(IntegratedServer server, ResourceKey<Level> dimension, BiFunction<RecipeManager, ServerLevel, T> action) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            throw new IllegalStateException("Integrated server does not have the active client dimension.");
        }
        return action.apply(level.recipeAccess(), level);
    }

    /**
     * @return the ingredient.
     * @since 1.8.4
     */
    public ItemStackHelper getIngredient() {
        return getSlot(3);
    }

    /**
     * @return the fuel item.
     * @since 1.8.4
     */
    public ItemStackHelper getFuel() {
        return getSlot(4);
    }

    /**
     * @return the first potion.
     * @since 1.8.4
     */
    public ItemStackHelper getFirstPotion() {
        return getSlot(0);
    }

    /**
     * @return the second potion.
     * @since 1.8.4
     */
    public ItemStackHelper getSecondPotion() {
        return getSlot(1);
    }

    /**
     * @return the third potion.
     * @since 1.8.4
     */
    public ItemStackHelper getThirdPotion() {
        return getSlot(2);
    }

    /**
     * @return a list of the potions inside the brewing stand.
     * @since 1.8.4
     */
    public List<ItemStackHelper> getPotions() {
        return Arrays.asList(getSlot(0), getSlot(1), getSlot(2));
    }

    @Override
    public String toString() {
        return String.format("BrewingStandInventory:{}");
    }

}
