
package li.cil.oc2.common.item.crafting;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.integration.Wrenches;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.Nullable;

public final class WrenchRecipe extends ShapelessRecipe {
    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(recipe.getGroup(), CraftingBookCategory.MISC, recipe.getResultItem(RegistryAccess.EMPTY), recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final NonNullList<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            if (stack.hasCraftingRemainingItem()) {
                result.set(slot, stack.getCraftingRemainingItem());
            } else if (Wrenches.isWrench(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            }
        }

        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements RecipeSerializer<WrenchRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final MapCodec<WrenchRecipe> CODEC =
            RecipeSerializer.SHAPELESS_RECIPE.codec()
            .xmap(
                WrenchRecipe::new,
                x -> x
            );
        public static final StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> STREAM_CODEC =
            RecipeSerializer.SHAPELESS_RECIPE.streamCodec()
            .map(
                WrenchRecipe::new,
                x -> x
            );

        @Override
        public MapCodec<WrenchRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }

    public static final class WrenchRecipeOutputAdapter implements RecipeOutput {
        private final RecipeOutput inner;

        public WrenchRecipeOutputAdapter(RecipeOutput inner) {
            this.inner = inner;
        }

        @Override
        public Advancement.Builder advancement() {
            return inner.advancement();
        }

        @Override
        public void accept(final ResourceLocation resourceLocation, final Recipe<?> recipe, @Nullable final AdvancementHolder advancementHolder, final ICondition... iConditions) {
            if (!(recipe instanceof ShapelessRecipe shapeless)) {
                throw new IllegalStateException("WrenchRecipeOutputAdapter can only be used on shapeless recipes");
            }

            inner.accept(resourceLocation, new WrenchRecipe(shapeless), advancementHolder, iConditions);
        }
    }
}
