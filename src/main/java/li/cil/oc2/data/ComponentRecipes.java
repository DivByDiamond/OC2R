package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.neoforged.neoforge.common.Tags;

final class ComponentRecipes {
    static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.TRANSISTOR.get(), 12)
                .pattern("RCR")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', net.minecraft.world.item.Items.COMPARATOR)
                .unlockedBy(
                        "has_gold",
                        ModRecipesProvider.inventoryChange(
                                net.minecraft.world.item.Items.GOLD_INGOT))
                .save(consumer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CIRCUIT_BOARD.get(), 6)
                .requires(Tags.Items.INGOTS_GOLD)
                .requires(net.minecraft.world.item.Items.CLAY_BALL)
                .requires(Items.TRANSISTOR.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);
    }
}