package li.cil.oc2.common.blockentity.computer.bus;

import java.util.*;
import javax.annotation.Nullable;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.computer.ComputerBlockEntity;
import li.cil.oc2.common.bus.element.AbstractBlockDeviceBusElement;
import li.cil.oc2.common.bus.element.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class ComputerBusElement extends AbstractBlockDeviceBusElement {
    private static final String DEVICE_ID_TAG_NAME = "device_id";

    private final ComputerBlockEntity owner;
    private final HashSet<Device> devices = new HashSet<>();
    public UUID deviceId = UUID.randomUUID();

    public ComputerBusElement(final ComputerBlockEntity owner) {
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

    public void addOwnDevices() {
        final Level level = owner.getLevel();
        assert level != null;

        collectDevices(level, getPosition(), null)
                .ifPresent(
                        result -> {
                            for (final BlockEntry info : result.getEntries()) {
                                devices.add(info.getDevice());
                                super.addDevice(info.getDevice());
                            }
                        });
    }

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        return super.getNeighbors()
                .map(
                        neighbors -> {
                            final ArrayList<DeviceBusElement> list = new ArrayList<>(neighbors);
                            list.add(owner.deviceItems.busElement);
                            return list;
                        });
    }

    @Override
    public boolean canScanContinueTowards(@Nullable final Direction direction) {
        return owner.getBlockState().getValue(ComputerBlock.FACING) != direction;
    }

    @Override
    protected boolean canDetectDevicesTowards(@Nullable final Direction direction) {
        return direction == null;
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        if (devices.contains(device)) {
            return Optional.of(deviceId);
        }
        return super.getDeviceIdentifier(device);
    }

    @Override
    public CompoundTag save(final HolderLookup.Provider registries) {
        final CompoundTag tag = super.save(registries);
        tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
        return tag;
    }

    public void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
            deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
        }
    }
}