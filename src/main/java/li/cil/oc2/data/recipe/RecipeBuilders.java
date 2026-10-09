package li.cil.oc2.data.recipe;

import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
//? if >=26.1 {
/*import net.minecraft.core.HolderGetter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
*///?}

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
