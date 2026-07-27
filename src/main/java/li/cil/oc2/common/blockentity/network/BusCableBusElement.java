package li.cil.oc2.common.blockentity.network;

import java.util.Set;
import javax.annotation.Nullable;
import li.cil.oc2.common.block.cable.BusCableStateProperties;
import li.cil.oc2.common.block.types.ConnectionType;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;
import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.element.BlockEntry;
import li.cil.oc2.common.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

final class BusCableBusElement extends AbstractBlockDeviceBusElement {
    private final BusCableBlockEntity owner;

    BusCableBusElement(final BusCableBlockEntity owner) {
        this.owner = owner;
    }

    @Nullable
    @Override
    public Level getLevel() {
        return owner.getLevel();
    }

    @Override
    public BlockPos getPosition() {
        return owner.getBlockPos();
    }

    @Override
    public boolean canScanContinueTowards(@Nullable final Direction direction) {
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(owner.getBlockState(), direction);
        return connectionType == ConnectionType.CABLE || connectionType == ConnectionType.INTERFACE;
    }

    @Override
    public boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        final ConnectionType connectionType =
                BusCableStateProperties.getConnectionType(owner.getBlockState(), direction);
        return connectionType == ConnectionType.INTERFACE;
    }

    @Override
    protected void collectSyntheticDevices(
            final LevelAccessor level,
            final BlockPos pos,
            @Nullable final Direction side,
            final Set<BlockEntry> entries) {
        super.collectSyntheticDevices(level, pos, side, entries);

        if (side == null || entries.isEmpty()) {
            return;
        }

        final String interfaceName = owner.getInterfaceName(side);
        if (!StringUtil.isNullOrEmpty(interfaceName)) {
            entries.add(
                    new BlockEntry(
                            new BlockDeviceInfo(null, new TypeNameRPCDevice(interfaceName)), side));
        }
    }

    @Override
    public double getEnergyConsumption() {
        return super.getEnergyConsumption()
                + Config.busCableEnergyPerTick
                + BusCableStateProperties.getInterfaceCount(owner.getBlockState())
                        * Config.busInterfaceEnergyPerTick;
    }
}