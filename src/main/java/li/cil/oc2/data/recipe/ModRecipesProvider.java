package li.cil.oc2.data.recipe;

import java.util.concurrent.CompletableFuture;
import li.cil.oc2.data.recipe.peripheral.PeripheralRecipes;
import li.cil.oc2.data.recipe.peripheral.RobotRecipes;
import li.cil.oc2.data.recipe.peripheral.StorageRecipes;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.ItemLike;

public final class ModRecipesProvider extends RecipeProvider {
    public ModRecipesProvider(
            final PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput consumer) {
        ComputerRecipes.build(consumer);
        StorageRecipes.build(consumer);
        PeripheralRecipes.build(consumer);
        RobotRecipes.build(consumer);
        CardRecipes.build(consumer);
        ComponentRecipes.build(consumer);
    }

    public static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(final ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}