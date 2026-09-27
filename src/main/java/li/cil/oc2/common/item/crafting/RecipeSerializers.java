package li.cil.oc2.common.item.crafting;

import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.NeoForgeRegistryBridge;
import li.cil.oc2.platform.Platform;
import net.neoforged.bus.api.IEventBus;

public final class RecipeSerializers {
    public static final Supplier<WrenchRecipe.Serializer> WRENCH =
            Platform.registries().register("minecraft:recipe_serializer", API.MOD_ID, "wrench", () -> WrenchRecipe.Serializer.INSTANCE);

    public static void initialize(IEventBus modBus) {
        NeoForgeRegistryBridge.instance().bind(modBus);
    }
}