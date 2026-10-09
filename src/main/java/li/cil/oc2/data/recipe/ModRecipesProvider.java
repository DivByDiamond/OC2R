package li.cil.oc2.data.recipe;

import java.util.concurrent.CompletableFuture;
import li.cil.oc2.data.recipe.peripheral.PeripheralRecipes;
import li.cil.oc2.data.recipe.peripheral.RobotRecipes;
import li.cil.oc2.data.recipe.peripheral.StorageRecipes;
//? if >=26.2 {
/*import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
*///?} else if >=26.1 {
/*import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
*///?} else {
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
//?}
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.level.ItemLike;

public final class ModRecipesProvider extends RecipeProvider {
    //? if >=26.1 {
    /*private final RecipeOutput consumer;

    private ModRecipesProvider(final HolderLookup.Provider registries, final RecipeOutput output) {
        super(registries, output);
        this.consumer = output;
        RecipeBuilders.setItems(items);
    }

    @Override
    protected void buildRecipes() {
    *///?} else {
    public ModRecipesProvider(
            final PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput consumer) {
    //?}
        ComputerRecipes.build(consumer);
        StorageRecipes.build(consumer);
        PeripheralRecipes.build(consumer);
        RobotRecipes.build(consumer);
        CardRecipes.build(consumer);
        ComponentRecipes.build(consumer);
    }

    //? if >=26.1 {
    /*public static final class Runner extends RecipeProvider.Runner {
        public Runner(
                final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(
                final HolderLookup.Provider registries, final RecipeOutput output) {
            return new ModRecipesProvider(registries, output);
        }

        @Override
        public String getName() {
            return "OC2R recipes";
        }
    }
    *///?}

    public static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryChange(final ItemLike item) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(item);
    }
}