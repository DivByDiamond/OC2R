package li.cil.oc2.common.bus.device.provider.util;

import java.util.Optional;
import java.util.function.Supplier;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.platform.BlockCapability;
import li.cil.oc2.platform.Platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractBlockEntityCapabilityDeviceProvider<T, U extends BlockEntity>
        extends AbstractBlockEntityDeviceProvider<U> {
    private final Supplier<BlockCapability<T>> capabilitySupplier;

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final BlockEntityType<U> blockEntityType,
            final Supplier<BlockCapability<T>> capabilitySupplier) {
        super(blockEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractBlockEntityCapabilityDeviceProvider(
            final Supplier<BlockCapability<T>> capabilitySupplier) {
        super();
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected final Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final U blockEntity) {
        final BlockCapability<T> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final var blockEntityLevel = blockEntity.getLevel();
        if (!(blockEntityLevel instanceof ServerLevel level))
            throw new IllegalStateException();

        final var blockPos = blockEntity.getBlockPos();
        final T optional =
                Platform.capabilities()
                        .getBlockCapability(
                                capability,
                                level,
                                blockPos,
                                null,
                                blockEntity,
                                query.getQuerySide());
        if (optional == null) {
            return Optional.empty();
        }

        return getBlockDevice(query, optional);
    }

    protected abstract Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final T value);
}