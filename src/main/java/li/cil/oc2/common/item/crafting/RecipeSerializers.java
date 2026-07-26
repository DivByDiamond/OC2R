/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item.crafting;

import li.cil.oc2.api.API;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;

public final class RecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, API.MOD_ID);

    ///////////////////////////////////////////////////////////////////

    public static final DeferredHolder<RecipeSerializer<?>, WrenchRecipe.Serializer> WRENCH = RECIPE_SERIALIZERS.register("wrench", () -> WrenchRecipe.Serializer.INSTANCE);

    ///////////////////////////////////////////////////////////////////

    public static void initialize(IEventBus modBus) {
        RECIPE_SERIALIZERS.register(modBus);
    }
}
