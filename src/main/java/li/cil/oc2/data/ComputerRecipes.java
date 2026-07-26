package li.cil.oc2.data;

import li.cil.oc2.common.block.ComputerBlockFactory;
import li.cil.oc2.common.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.neoforged.neoforge.common.Tags;

final class ComputerRecipes {
    static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, ComputerBlockFactory.getComputerWithFlash())
            .pattern("ICI")
            .pattern("XTX")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('C', Tags.Items.CHESTS_WOODEN)
            .define('X', Items.BUS_INTERFACE.get())
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_transistor", ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
            .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.BUS_CABLE.get(), 16)
            .pattern("III")
            .pattern("GTG")
            .pattern("III")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_transistor", ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.MISC, Items.BUS_INTERFACE.get())
            .requires(Items.TRANSISTOR.get())
            .requires(Items.BUS_CABLE.get())
            .unlockedBy("has_bus_cable", ModRecipesProvider.inventoryChange(Items.BUS_CABLE.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_CONNECTOR.get(), 4)
            .pattern("IGI")
            .pattern("ITI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_transistor", ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_HUB.get())
            .pattern("ICI")
            .pattern("XTX")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('C', Items.NETWORK_CONNECTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_network_connector", ModRecipesProvider.inventoryChange(Items.NETWORK_CONNECTOR.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.REDSTONE_INTERFACE.get())
            .pattern("ICI")
            .pattern("XTX")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('C', net.minecraft.world.item.Items.COMPARATOR)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.NETWORK_CABLE.get(), 8)
            .pattern("SSS")
            .pattern("GTG")
            .pattern("SSS")
            .define('S', Tags.Items.STRINGS)
            .define('G', Tags.Items.GLASS_BLOCKS)
            .define('T', Items.TRANSISTOR.get())
            .unlockedBy("has_network_connector", ModRecipesProvider.inventoryChange(Items.NETWORK_CONNECTOR.get()))
            .save(consumer);
    }
}
