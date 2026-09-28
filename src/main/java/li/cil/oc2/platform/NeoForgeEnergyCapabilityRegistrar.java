package li.cil.oc2.platform;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/** {@link EnergyCapabilityRegistrar} wrapping NeoForge's {@link RegisterCapabilitiesEvent}. */
public final class NeoForgeEnergyCapabilityRegistrar implements EnergyCapabilityRegistrar {
    // Capability providers are re-queried on every device scan, but NeoForge's capability system
    // (and our own device-bus code, via ObjectDevice/IdentityProxy) requires the exposed object to
    // keep a stable identity across queries for the same underlying storage. Without this cache,
    // unwrap() below would hand out a fresh IEnergyStorage instance per query, which the device bus
    // then treats as a different device every scan, causing it to be "removed" and "re-added" on
    // every single neighbor update — as a cascading storm of rescans across a whole test world, this
    // was observed hanging a GameTestServer indefinitely.
    private static final Map<EnergyStorage, IEnergyStorage> WRAPPER_CACHE = new ConcurrentHashMap<>();

    private final RegisterCapabilitiesEvent event;

    // The event is valid only for the duration of the one-shot setup callback that constructs
    // this registrar and is never retained past it, so storing the reference cannot leak mutable
    // state to anything outside that call.
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "event outlives only the setup callback")
    public NeoForgeEnergyCapabilityRegistrar(final RegisterCapabilitiesEvent event) {
        this.event = event;
    }

    @Override
    public void registerBlock(final BlockEnergyProvider provider, final Block... blocks) {
        event.registerBlock(
                Capabilities.EnergyStorage.BLOCK,
                (level, pos, state, be, side) -> unwrap(provider.get(level, pos, state, be, side)),
                blocks);
    }

    @Override
    public void registerEntity(final EntityType<?> type, final EntityEnergyProvider provider) {
        event.registerEntity(
                Capabilities.EnergyStorage.ENTITY,
                type,
                (entity, side) -> unwrap(provider.get(entity, side)));
    }

    @Override
    public void registerItem(final ItemEnergyProvider provider, final ItemLike... items) {
        event.registerItem(
                Capabilities.EnergyStorage.ITEM,
                (stack, unused) -> unwrap(provider.get(stack)),
                items);
    }

    @Nullable
    private static IEnergyStorage unwrap(@Nullable final EnergyStorage storage) {
        if (storage == null) {
            return null;
        }
        return WRAPPER_CACHE.computeIfAbsent(storage, NeoForgeEnergyCapabilityRegistrar::wrap);
    }

    private static IEnergyStorage wrap(final EnergyStorage storage) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(final int maxReceive, final boolean simulate) {
                return storage.receiveEnergy(maxReceive, simulate);
            }

            @Override
            public int extractEnergy(final int maxExtract, final boolean simulate) {
                return storage.extractEnergy(maxExtract, simulate);
            }

            @Override
            public int getEnergyStored() {
                return storage.getEnergyStored();
            }

            @Override
            public int getMaxEnergyStored() {
                return storage.getMaxEnergyStored();
            }

            @Override
            public boolean canExtract() {
                return storage.canExtract();
            }

            @Override
            public boolean canReceive() {
                return storage.canReceive();
            }
        };
    }
}
