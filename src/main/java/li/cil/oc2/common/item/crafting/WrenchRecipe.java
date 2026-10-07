package li.cil.oc2.common.item.crafting;

import com.mojang.serialization.MapCodec;
import java.util.List;
import li.cil.oc2.common.integration.Wrenches;
import li.cil.oc2.platform.Platform;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class WrenchRecipe extends ShapelessRecipe {
    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(
                recipe.getGroup(),
                CraftingBookCategory.MISC,
                recipe.getResultItem(RegistryAccess.EMPTY),
                recipe.getIngredients());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(final CraftingInput input) {
        final List<ItemStack> result = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int slot = 0; slot < input.size(); slot++) {
            final ItemStack stack = input.getItem(slot);
            final ItemStack remainder = Platform.hooks().getCraftingRemainder(stack);
            if (!remainder.isEmpty()) {
                result.set(slot, remainder);
            } else if (Wrenches.isWrench(stack)) {
                final ItemStack copy = stack.copy();
                copy.setCount(1);
                result.set(slot, copy);
            }
        }

        return (NonNullList<ItemStack>) result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    public static final class Serializer implements RecipeSerializer<WrenchRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final MapCodec<WrenchRecipe> CODEC_INSTANCE =
                RecipeSerializer.SHAPELESS_RECIPE.codec().xmap(WrenchRecipe::new, x -> x);
        public static final StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> STREAM_CODEC_INSTANCE =
                RecipeSerializer.SHAPELESS_RECIPE.streamCodec().map(WrenchRecipe::new, x -> x);

        @Override
        public MapCodec<WrenchRecipe> codec() {
            return CODEC_INSTANCE;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WrenchRecipe> streamCodec() {
            return STREAM_CODEC_INSTANCE;
        }
    }
}
