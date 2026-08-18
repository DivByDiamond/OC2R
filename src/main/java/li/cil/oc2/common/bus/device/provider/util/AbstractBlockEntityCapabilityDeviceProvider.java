package li.cil.oc2.common.bus.device.provider.util;

import java.util.Optional;
import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractBlockEntityCapabilityDeviceProvider<T, U extends BlockEntity>
        extends AbstractBlockEntityDeviceProvider<U> {
    private final Supplier<BlockCapability<T, @Nullable Direction>> capabilitySupplier;

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final BlockEntityType<U> blockEntityType,
            final Supplier<BlockCapability<T, @Nullable Direction>> capabilitySupplier) {
        super(blockEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final Supplier<BlockCapability<T, @Nullable Direction>> capabilitySupplier) {
        super();
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected final Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final U blockEntity) {
        final BlockCapability<T, @Nullable Direction> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final var blockEntityLevel = blockEntity.getLevel();
        if (!(blockEntityLevel instanceof ServerLevel level))
            throw new IllegalStateException();

        final var blockPos = blockEntity.getBlockPos();
        final T optional =
                level.getCapability(capability, blockPos, null, blockEntity, query.getQuerySide());
        if (optional == null) {
            return Optional.empty();
        }

        return getBlockDevice(query, optional);
    }

    protected abstract Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final T value);
}