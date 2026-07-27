package li.cil.oc2.common.bus.element;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;

import net.minecraft.core.Direction;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

public final class BlockEntry implements GroupEntry {
    private final BlockDeviceInfo deviceInfo;
    @Nullable private final String dataKey;
    private final Device device;
    @Nullable private final Direction side;

    public BlockEntry(final BlockDeviceInfo deviceInfo, @Nullable final Direction side) {
        this.deviceInfo = deviceInfo;
        this.side = side;

        this.dataKey = optionalKey(deviceInfo.provider).orElse(null);
        this.device = deviceInfo.device;
    }

    @Override
    public Optional<String> getDeviceDataKey() {
        return Optional.ofNullable(dataKey);
    }

    @Override
    public Optional<String> getLegacyDeviceDataKey() {
        if (dataKey != null) {
            return Optional.of("oc2r:block_device_provider");
        }
        return Optional.empty();
    }

    @Override
    public int getDeviceEnergyConsumption() {
        return deviceInfo.getEnergyConsumption();
    }

    @Override
    public Device getDevice() {
        return device;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BlockEntry that = (BlockEntry) o;
        return Objects.equals(dataKey, that.dataKey)
                && device.equals(that.device)
                && side == that.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataKey, device, side);
    }

    @Override
    public String toString() {
        return device.toString();
    }
}
