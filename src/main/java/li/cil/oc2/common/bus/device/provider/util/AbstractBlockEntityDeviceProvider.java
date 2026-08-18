package li.cil.oc2.common.bus.device.provider.util;

import java.util.Optional;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractBlockEntityDeviceProvider<T extends BlockEntity>
        extends AbstractBlockDeviceProvider {
    private final BlockEntityType<T> blockEntityType;

    protected AbstractBlockEntityDeviceProvider(final BlockEntityType<T> blockEntityType) {
        super();
        this.blockEntityType = blockEntityType;
    }

    protected AbstractBlockEntityDeviceProvider() {
        super();
        this.blockEntityType = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Optional<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (blockEntity == null) {
            return Optional.empty();
        }

        if (blockEntityType != null && !blockEntity.getType().equals(blockEntityType)) {
            return Optional.empty();
        }

        try {
            return getBlockDevice(query, (T) blockEntity);
        } catch (ClassCastException ignored) {
            return Optional.empty();
        }
    }

    protected abstract Optional<Device> getBlockDevice(
            final BlockDeviceQuery query, final T blockEntity);
}