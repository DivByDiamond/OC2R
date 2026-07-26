package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.crafting.WrenchRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.Tags;

final class StorageRecipes {
    static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.DISK_DRIVE.get())
            .pattern("IUI")
            .pattern("XTD")
            .pattern("IBI")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('U', ItemTags.BUTTONS)
            .define('T', Items.TRANSISTOR.get())
            .define('X', Items.BUS_INTERFACE.get())
            .define('D', net.minecraft.world.item.Items.DISPENSER)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_SMALL.get(), 2)
            .pattern("ITI")
            .pattern(" B ")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_MEDIUM.get(), 2)
            .pattern("GTG")
            .pattern(" B ")
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_LARGE.get(), 2)
            .pattern("DTD")
            .pattern(" B ")
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.MEMORY_EXTRA_LARGE.get(), 2)
            .pattern("DTD")
            .pattern("EBE")
            .pattern("DTD")
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .define('E', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_SMALL.get())
            .pattern("ITI")
            .pattern("EBE")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .define('E', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_MEDIUM.get())
            .pattern("GTG")
            .pattern("EBE")
            .define('G', Tags.Items.INGOTS_GOLD)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .define('E', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_LARGE.get())
            .pattern("DTD")
            .pattern("EBE")
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .define('E', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.HARD_DRIVE_EXTRA_LARGE.get())
            .pattern("ETE")
            .pattern("DBD")
            .pattern("EBE")
            .define('D', Tags.Items.GEMS_DIAMOND)
            .define('T', Items.TRANSISTOR.get())
            .define('B', Items.CIRCUIT_BOARD.get())
            .define('E', Tags.Items.GEMS_EMERALD)
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapedRecipeBuilder
            .shaped(RecipeCategory.MISC, Items.FLASH_MEMORY.get())
            .pattern("ITI")
            .pattern("RBR")
            .define('I', Tags.Items.INGOTS_IRON)
            .define('T', Items.TRANSISTOR.get())
            .define('R', Tags.Items.DUSTS_REDSTONE)
            .define('B', Items.CIRCUIT_BOARD.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(consumer);

        ShapelessRecipeBuilder
            .shapeless(RecipeCategory.MISC, Items.FLASH_MEMORY_CUSTOM.get())
            .requires(Items.FLASH_MEMORY.get())
            .unlockedBy("has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
            .unlockedBy("has_robot", ModRecipesProvider.inventoryChange(Items.ROBOT.get()))
            .save(new WrenchRecipe.WrenchRecipeOutputAdapter(consumer));
    }
}
