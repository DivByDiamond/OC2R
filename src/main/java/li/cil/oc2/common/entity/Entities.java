package li.cil.oc2.common.entity;

import li.cil.oc2.api.API;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class Entities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, API.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Robot>> ROBOT =
            register(
                    "robot",
                    Robot::new,
                    MobCategory.MISC,
                    b -> b.sized(14f / 16f, 14f / 16f).fireImmune().noSummon());

    public static void initialize(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    @SuppressWarnings("SameParameterValue")
    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            final String name,
            final EntityType.EntityFactory<T> factory,
            final MobCategory classification,
            final Function<EntityType.Builder<T>, EntityType.Builder<T>> customizer) {
        return ENTITIES.register(
                name,
                () -> customizer.apply(EntityType.Builder.of(factory, classification)).build(name));
    }
}
