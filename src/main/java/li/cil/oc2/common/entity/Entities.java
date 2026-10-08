package li.cil.oc2.common.entity;

import java.util.function.Function;
import java.util.function.Supplier;
import li.cil.oc2.api.API;
import li.cil.oc2.platform.Platform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class Entities {
    public static final Supplier<EntityType<Robot>> ROBOT =
            register(
                    "robot",
                    Robot::new,
                    MobCategory.MISC,
                    b -> b.sized(14f / 16f, 14f / 16f).fireImmune().noSummon());

    public static void initialize() {
        // Calling this loads the class, which queues its registrations on the bridge.
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Entity> Supplier<EntityType<T>> register(
            final String name,
            final EntityType.EntityFactory<T> factory,
            final MobCategory classification,
            final Function<EntityType.Builder<T>, EntityType.Builder<T>> customizer) {
        return Platform.registries().register("minecraft:entity_type", API.MOD_ID, 
                name,
                //? if >=26.1 {
                /*() ->
                        customizer
                                .apply(EntityType.Builder.of(factory, classification))
                                .build(
                                        net.minecraft.resources.ResourceKey.create(
                                                net.minecraft.core.registries.Registries.ENTITY_TYPE,
                                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(API.MOD_ID, name))));
                *///?} else {
                () -> customizer.apply(EntityType.Builder.of(factory, classification)).build(name));
                //?}
    }
}