package li.cil.oc2.data.recipe;

//? if >=26.1 {
/*import net.minecraft.core.HolderGetter;
*///?}
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
//? if >=26.1 {
/*import net.minecraft.world.item.Item;
*///?}
import net.minecraft.world.item.ItemStack;
//? if >=26.1 {
/*import net.minecraft.world.item.ItemStackTemplate;
*///?}
import net.minecraft.world.level.ItemLike;

/**
 * Recipe builder factories that hide the 26.x change where builders need an item lookup. On 26.x
 * the lookup is provided by {@link ModRecipesProvider} for the duration of a data generation run.
 */
public final class RecipeBuilders {
    //? if >=26.1 {
    /*private static HolderGetter<Item> items;

    static void setItems(final HolderGetter<Item> items) {
        RecipeBuilders.items = items;
    }
    *///?}

    private RecipeBuilders() {}

    public static ShapedRecipeBuilder computerWithFlash() {
        return machineWithFlash(false);
    }

    public static ShapedRecipeBuilder robotWithFlash() {
        return machineWithFlash(true);
    }

    private static ShapedRecipeBuilder machineWithFlash(final boolean robot) {
        //? if >=26.1 {
        /*final var empty = java.util.Optional.<ItemStackTemplate>empty();
        final var container = li.cil.oc2.common.components.RestrictedContainer.fromTemplates(java.util.Map.of(
                li.cil.oc2.common.tags.ItemTags.DEVICES_FLASH_MEMORY, java.util.List.of(java.util.Optional.of(
                        new ItemStackTemplate(li.cil.oc2.common.item.Items.FLASH_MEMORY_CUSTOM.get()))),
                li.cil.oc2.common.tags.ItemTags.DEVICES_CPU, java.util.List.of(empty),
                li.cil.oc2.common.tags.ItemTags.DEVICES_MEMORY, java.util.Collections.nCopies(4, empty),
                robot ? li.cil.oc2.common.tags.ItemTags.DEVICES_ROBOT_MODULE
                : li.cil.oc2.common.tags.ItemTags.DEVICES_CARD,
                java.util.Collections.nCopies(4, empty),
                li.cil.oc2.common.tags.ItemTags.DEVICES_HARD_DRIVE,
                java.util.Collections.nCopies(robot ? 2 : 4, empty)));
        final var components = net.minecraft.core.component.DataComponentPatch.builder()
                .set(li.cil.oc2.common.components.DataComponents.RESTRICTED_CONTAINER.get(), container).build();
        return ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC,
                new ItemStackTemplate(robot ? li.cil.oc2.common.item.Items.ROBOT.get()
                        : li.cil.oc2.common.item.Items.COMPUTER.get(), components));
        *///?} else {
        return shaped(RecipeCategory.MISC, robot ? li.cil.oc2.common.item.tool.RobotItem.getRobotWithFlash()
                : li.cil.oc2.common.block.computer.factory.ComputerBlockFactory.getComputerWithFlash());
        //?}
    }

    public static ShapedRecipeBuilder shaped(final RecipeCategory category, final ItemLike item) {
        //? if >=26.1 {
        /*return ShapedRecipeBuilder.shaped(items, category, item);
        *///?} else {
        return ShapedRecipeBuilder.shaped(category, item);
        //?}
    }

    public static ShapedRecipeBuilder shaped(
            final RecipeCategory category, final ItemLike item, final int count) {
        //? if >=26.1 {
        /*return ShapedRecipeBuilder.shaped(items, category, item, count);
        *///?} else {
        return ShapedRecipeBuilder.shaped(category, item, count);
        //?}
    }

    public static ShapedRecipeBuilder shaped(final RecipeCategory category, final ItemStack stack) {
        //? if >=26.1 {
        /*return ShapedRecipeBuilder.shaped(
                items, category, ItemStackTemplate.fromNonEmptyStack(stack));
        *///?} else {
        return ShapedRecipeBuilder.shaped(category, stack);
        //?}
    }

    public static ShapelessRecipeBuilder shapeless(
            final RecipeCategory category, final ItemLike item) {
        //? if >=26.1 {
        /*return ShapelessRecipeBuilder.shapeless(items, category, item);
        *///?} else {
        return ShapelessRecipeBuilder.shapeless(category, item);
        //?}
    }

    public static ShapelessRecipeBuilder shapeless(
            final RecipeCategory category, final ItemLike item, final int count) {
        //? if >=26.1 {
        /*return ShapelessRecipeBuilder.shapeless(items, category, item, count);
        *///?} else {
        return ShapelessRecipeBuilder.shapeless(category, item, count);
        //?}
    }
}
