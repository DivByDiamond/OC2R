package li.cil.oc2.common.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import li.cil.oc2.platform.FluidStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.material.Fluids;

/** Registry-aware fluid serialization retains every component, including custom mod variants. */
public final class MachineResourcePersistence {
    private static final Codec<FluidStack> FLUID = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(
                    stack -> stack.isEmpty() ? Fluids.EMPTY : stack.getFluid()),
            Codec.intRange(0, MachineResources.FLUID_CAPACITY).fieldOf("amount").forGetter(FluidStack::getAmount),
            DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(FluidStack::getComponentsPatch)).apply(instance, FluidStack::new));
    private static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            FLUID.fieldOf("fluid").forGetter(State::fluid),
            FLUID.fieldOf("recovery").forGetter(State::recovery),
            Codec.intRange(0, MachineResources.ENERGY_CAPACITY).fieldOf("energy").forGetter(State::energy))
            .apply(instance, State::new));

    private record State(FluidStack fluid, FluidStack recovery, int energy) {}

    private MachineResourcePersistence() {}

    public static void save(final MachineResources resources, final CompoundTag tag,
                            final HolderLookup.Provider registries) {
        final State state = new State(resources.fluid(), resources.recovery(), resources.energy());
        tag.put("resources", CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), state)
                .getOrThrow());
    }

    public static void load(final MachineResources resources, final CompoundTag tag,
                            final HolderLookup.Provider registries) {
        final var data = tag.get("resources");
        if (data == null) return;
        CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), data).result().ifPresent(
                state -> resources.restore(state.fluid(), state.recovery(), state.energy()));
    }
}
