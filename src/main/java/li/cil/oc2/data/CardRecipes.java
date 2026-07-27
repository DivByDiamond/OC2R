package li.cil.oc2.data;

import li.cil.oc2.common.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.neoforged.neoforge.common.Tags;

final class CardRecipes {
    static void build(final RecipeOutput consumer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.REDSTONE_INTERFACE_CARD.get())
                .pattern("IRT")
                .pattern(" B ")
                .define('R', net.minecraft.world.item.Items.REDSTONE_TORCH)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.NETWORK_INTERFACE_CARD.get())
                .pattern("IGT")
                .pattern(" B ")
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.NETWORK_TUNNEL_CARD.get())
                .pattern("IET")
                .pattern(" B ")
                .define('E', Tags.Items.ENDER_PEARLS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.FILE_IMPORT_EXPORT_CARD.get())
                .pattern("IET")
                .pattern(" B ")
                .define('E', net.minecraft.world.item.Items.PAPER)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.SOUND_CARD.get())
                .pattern("IST")
                .pattern(" B ")
                .define('S', net.minecraft.world.item.Items.NOTE_BLOCK)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.FLOPPY.get())
                .pattern("ITI")
                .pattern("QBQ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_disk_drive",
                        ModRecipesProvider.inventoryChange(Items.DISK_DRIVE.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, Items.FLOPPY_MODERN.get())
                .pattern("ITI")
                .pattern("QBQ")
                .pattern("QBQ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('T', Items.TRANSISTOR.get())
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_disk_drive",
                        ModRecipesProvider.inventoryChange(Items.DISK_DRIVE.get()))
                .save(consumer);
    }
}