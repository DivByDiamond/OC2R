package li.cil.oc2.data.recipe;

import li.cil.oc2.common.item.crafting.WrenchRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

/** Data generation only: wraps shapeless recipes into {@link WrenchRecipe}s. */
public final class WrenchRecipeOutputAdapter implements RecipeOutput {
    private final RecipeOutput inner;

    public WrenchRecipeOutputAdapter(RecipeOutput inner) {
        this.inner = inner;
    }

    @Override
    public Advancement.Builder advancement() {
        return inner.advancement();
    }

    @Override
    public void accept(
            final ResourceLocation resourceLocation,
            final Recipe<?> recipe,
            @Nullable final AdvancementHolder advancementHolder,
            final ICondition... iConditions) {
        if (!(recipe instanceof ShapelessRecipe shapeless)) {
            throw new IllegalStateException(
                    "WrenchRecipeOutputAdapter can only be used on shapeless recipes");
        }

        inner.accept(
                resourceLocation, new WrenchRecipe(shapeless), advancementHolder, iConditions);
    }
}
