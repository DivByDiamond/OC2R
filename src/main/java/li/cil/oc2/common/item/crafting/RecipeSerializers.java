package li.cil.oc2.common.item.crafting;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.Platform;

public final class RecipeSerializers {
    //? if >=26.1 {
    /*public static final Supplier<net.minecraft.world.item.crafting.RecipeSerializer<WrenchRecipe>> WRENCH =
            Platform.registries().register("minecraft:recipe_serializer", API.MOD_ID, "wrench", () -> WrenchRecipe.SERIALIZER);
    *///?} else {
    public static final Supplier<WrenchRecipe.Serializer> WRENCH =
            Platform.registries().register("minecraft:recipe_serializer", API.MOD_ID, "wrench", () -> WrenchRecipe.Serializer.INSTANCE);
    //?}

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }
}