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

//? if >=26.1 {
/*// Minecraft 26.x turned RecipeSerializer into a record and ShapelessRecipe into a final-serializer
// class, so the wrench recipe wraps a shapeless recipe instead of extending it.
public final class WrenchRecipe extends net.minecraft.world.item.crafting.NormalCraftingRecipe {
    public static final RecipeSerializer<WrenchRecipe> SERIALIZER =
            new RecipeSerializer<>(
                    ShapelessRecipe.MAP_CODEC.xmap(WrenchRecipe::new, WrenchRecipe::delegate),
                    ShapelessRecipe.STREAM_CODEC.map(WrenchRecipe::new, WrenchRecipe::delegate));

    private final ShapelessRecipe delegate;

    public WrenchRecipe(final ShapelessRecipe recipe) {
        super(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(recipe.showNotification()),
                new net.minecraft.world.item.crafting.CraftingRecipe.CraftingBookInfo(
                        CraftingBookCategory.MISC, recipe.group()));
        this.delegate = recipe;
    }

    private ShapelessRecipe delegate() {
        return delegate;
    }

    @Override
    public RecipeSerializer<WrenchRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    protected net.minecraft.world.item.crafting.PlacementInfo createPlacementInfo() {
        return delegate.placementInfo();
    }

    @Override
    public boolean matches(final CraftingInput input, final net.minecraft.world.level.Level level) {
        return delegate.matches(input, level);
    }

    @Override
    public ItemStack assemble(final CraftingInput input) {
        return delegate.assemble(input);
    }

    @Override
    public List<net.minecraft.world.item.crafting.display.RecipeDisplay> display() {
        return delegate.display();
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
}
*///?} else {
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
//?}
