package li.cil.oc2.data.recipe;

import li.cil.oc2.common.item.Items;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.neoforged.neoforge.common.Tags;

final class ComputerRecipes {
    static void build(final RecipeOutput consumer) {
        RecipeBuilders.computerWithFlash()
                .pattern("ICI")
                .pattern("XTX")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Tags.Items.CHESTS_WOODEN)
                .define('X', Items.BUS_INTERFACE.get())
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .unlockedBy(
                        "has_circuit_board",
                        ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.BUS_CABLE.get(), 16)
                .pattern("III")
                .pattern("GTG")
                .pattern("III")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.INGOTS_GOLD)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        RecipeBuilders.shapeless(RecipeCategory.MISC, Items.BUS_INTERFACE.get())
                .requires(Items.TRANSISTOR.get())
                .requires(Items.BUS_CABLE.get())
                .unlockedBy(
                        "has_bus_cable", ModRecipesProvider.inventoryChange(Items.BUS_CABLE.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.NETWORK_CONNECTOR.get(), 4)
                .pattern("IGI")
                .pattern("ITI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy(
                        "has_transistor",
                        ModRecipesProvider.inventoryChange(Items.TRANSISTOR.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.NETWORK_HUB.get())
                .pattern("ICI")
                .pattern("XTX")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.NETWORK_CONNECTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_network_connector",
                        ModRecipesProvider.inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.NETWORK_SWITCH.get())
                .pattern("ICI")
                .pattern("TBT")
                .pattern("ICI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.NETWORK_CONNECTOR.get())
                .define('T', Items.TRANSISTOR.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_network_connector",
                        ModRecipesProvider.inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.MEDIA_STATION.get())
                .pattern("ICI").pattern("DFD").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', Items.CIRCUIT_BOARD.get())
                .define('D', net.minecraft.world.item.Items.REDSTONE)
                .define('F', net.minecraft.world.item.Items.COMPARATOR)
                .define('B', Items.BUS_INTERFACE.get())
                .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.MACHINE_INTERFACE.get())
                .pattern("IRI").pattern("HCH").pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('R', net.minecraft.world.item.Items.COMPARATOR)
                .define('H', net.minecraft.world.item.Items.HOPPER)
                .define('C', Items.CIRCUIT_BOARD.get())
                .define('B', Items.BUS_INTERFACE.get())
                .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.WIRELESS_ACCESS_POINT.get())
                .pattern(" C ").pattern("IBI").pattern(" N ")
                .define('C', net.minecraft.world.item.Items.COPPER_INGOT)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', Items.CIRCUIT_BOARD.get())
                .define('N', Items.NETWORK_CONNECTOR.get())
                .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.ENVIRONMENT_SENSOR.get())
                .pattern("IGI").pattern("DBD").pattern(" X ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('D', net.minecraft.world.item.Items.DAYLIGHT_DETECTOR)
                .define('B', Items.CIRCUIT_BOARD.get())
                .define('X', Items.BUS_INTERFACE.get())
                .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.GPS_TOWER.get())
                .pattern("IRI")
                .pattern("CPC")
                .pattern("BBB")
                .define('I', net.minecraft.world.item.Items.IRON_BARS)
                .define('R', net.minecraft.world.item.Items.COPPER_INGOT)
                .define('C', Items.CIRCUIT_BOARD.get())
                .define('P', Items.CPU_TIER_3.get())
                .define('B', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_circuit_board", ModRecipesProvider.inventoryChange(Items.CIRCUIT_BOARD.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.REDSTONE_INTERFACE.get())
                .pattern("ICI")
                .pattern("XTX")
                .pattern("IBI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('C', net.minecraft.world.item.Items.COMPARATOR)
                .define('T', Items.TRANSISTOR.get())
                .define('X', Items.BUS_INTERFACE.get())
                .define('B', Items.CIRCUIT_BOARD.get())
                .unlockedBy(
                        "has_computer", ModRecipesProvider.inventoryChange(Items.COMPUTER.get()))
                .save(consumer);

        RecipeBuilders.shaped(RecipeCategory.MISC, Items.NETWORK_CABLE.get(), 8)
                .pattern("SSS")
                .pattern("GTG")
                .pattern("SSS")
                .define('S', Tags.Items.STRINGS)
                .define('G', Tags.Items.GLASS_BLOCKS)
                .define('T', Items.TRANSISTOR.get())
                .unlockedBy(
                        "has_network_connector",
                        ModRecipesProvider.inventoryChange(Items.NETWORK_CONNECTOR.get()))
                .save(consumer);
    }
}
