/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class BlockEntityCapabilityDeviceProvider extends AbstractBlockEntityCapabilityDeviceProvider<Device, BlockEntity> {
    public BlockEntityCapabilityDeviceProvider() {
        super(() -> Capabilities.Device.BLOCK);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final Device device) {
        return Optional.of(device);
    }
}
