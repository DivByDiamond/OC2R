package li.cil.oc2.data.recipe.peripheral;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.tool.RobotItem;
import li.cil.oc2.data.recipe.ModRecipesProvider;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.neoforged.neoforge.common.Tags;

public final class RobotRecipes {
    public static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, RobotItem.getRobotWithFlash())
                .pattern("ICI")
                .pattern("PTP")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('P', net.minecraft.world.item.Items.PISTON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .unlockedBy(
                        "has_circuit_board",
                        ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.INVENTORY_OPERATIONS_MODULE.get())
                .pattern("TCG")
                .pattern(" B ")
                .define('T', Items.TRANSISTOR.get())
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.BLOCK_OPERATIONS_MODULE.get())
                .pattern("TPG")
                .pattern(" B ")
                .define('T', Items.TRANSISTOR.get())
                .define('P', net.minecraft.world.item.Items.DIAMOND_PICKAXE)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.NETWORK_TUNNEL_MODULE.get())
                .pattern("TEG")
                .pattern(" B ")
                .define('T', Items.TRANSISTOR.get())
                .define('E', Tags.Items.ENDER_PEARLS)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
                .save(consumer);
    }
}