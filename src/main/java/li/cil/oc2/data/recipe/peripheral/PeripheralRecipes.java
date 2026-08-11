package li.cil.oc2.data.recipe.peripheral;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.WrenchRecipe;
import li.cil.oc2.data.recipe.ModRecipesProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.neoforged.neoforge.common.Tags;

public final class PeripheralRecipes {
    public static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.KEYBOARD.get())
                .pattern("UUU")
                .pattern("XTU")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('U', net.minecraft.tags.ItemTags.BUTTONS)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.CHARGER.get())
                .pattern("IPI")
                .pattern("XTX")
                .pattern("IRI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('P', net.minecraft.world.item.Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.PROJECTOR.get())
                .pattern("GLG")
                .pattern("XTD")
                .pattern("GBG")
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('L', net.minecraft.world.item.Items.REDSTONE_LAMP)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.WRENCH.get())
                .pattern("I I")
                .pattern(" T ")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.MANUAL.get())
                .requires(net.minecraft.world.item.Items.BOOK)
                .unlockedBy(
                        "has_book",
                        ModRecipesProvider.inventoryChange(net.minecraft.world.item.Items.BOOK))
                .unlockedBy("has_wrench", ModRecipesProvider.inventoryChange(Items.WRENCH.get()))
                .save(new WrenchRecipe.WrenchRecipeOutputAdapter(consumer));
    }
}