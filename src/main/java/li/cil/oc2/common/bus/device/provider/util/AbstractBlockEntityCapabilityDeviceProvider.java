package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractBlockEntityCapabilityDeviceProvider<
                TCapability, TBlockEntity extends BlockEntity>
        extends AbstractBlockEntityDeviceProvider<TBlockEntity> {
    private final Supplier<BlockCapability<TCapability, @Nullable Direction>> capabilitySupplier;

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final BlockEntityType<TBlockEntity> blockEntityType,
            final Supplier<BlockCapability<TCapability, @Nullable Direction>> capabilitySupplier) {
        super(blockEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final Supplier<BlockCapability<TCapability, @Nullable Direction>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected final Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final TBlockEntity blockEntity) {
        final BlockCapability<TCapability, @Nullable Direction> capability =
                capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        if (!(blockEntity.getLevel() instanceof ServerLevel level))
            throw new IllegalStateException();

        final var blockPos = blockEntity.getBlockPos();
        final TCapability optional =
                level.getCapability(capability, blockPos, null, blockEntity, query.getQuerySide());
        if (optional == null) {
            return Optional.empty();
        }

        return getBlockDevice(query, optional);
    }

    protected abstract Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final TCapability value);
}
