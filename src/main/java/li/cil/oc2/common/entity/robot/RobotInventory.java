/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity.robot;

import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.AbstractDeviceBusElement;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.components.RestrictedContainer;
import li.cil.oc2.common.container.FixedSizeItemStackHandler;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.vm.AbstractVMItemStackHandlers;
import li.cil.oc2.common.vm.BaseAddressProvider;
import li.cil.oc2.common.vm.VMItemStackHandlers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.*;

public class RobotInventory {
    public static final int INVENTORY_SIZE = 12;
    private static final int MEMORY_SLOTS = 4;
    private static final int HARD_DRIVE_SLOTS = 2;
    private static final int FLASH_MEMORY_SLOTS = 1;
    private static final int MODULE_SLOTS = 4;
    private static final int CPU_SLOTS = 1;

    private final Robot robot;
    private final RobotItemStackHandlers deviceItems;
    private final RobotBusElement busElement;
    private final ItemStackHandler inventory;
    private Runnable onDeviceChanged = () -> {};

    public RobotInventory(final Robot robot) {
        this.robot = robot;
        this.deviceItems = new RobotItemStackHandlers(robot::registryAccess);
        this.busElement = new RobotBusElement();
        this.inventory = new FixedSizeItemStackHandler(INVENTORY_SIZE);
    }

    public void setOnDeviceChanged(final Runnable onDeviceChanged) {
        this.onDeviceChanged = onDeviceChanged;
    }

    public AbstractVMItemStackHandlers getDeviceItems() { return deviceItems; }
    public VMItemStackHandlers getItemStackHandlers() { return deviceItems; }
    public DeviceBusElement getBusElement() { return busElement; }
    public ItemStackHandler getInventory() { return inventory; }
    public Object getCombinedItemHandlers() { return deviceItems.combinedItemHandlers; }

    public void saveItems(final HolderLookup.Provider provider, final CompoundTag tag) {
        deviceItems.saveItems(provider, tag);
    }

    public CompoundTag saveDevices(final HolderLookup.Provider registries) {
        return deviceItems.saveDevices(registries);
    }

    public void loadItems(final HolderLookup.Provider provider, final CompoundTag tag) {
        deviceItems.loadItems(provider, tag);
    }

    public void loadDevices(final HolderLookup.Provider registries, final CompoundTag tag) {
        deviceItems.loadDevices(registries, tag);
    }

    public CompoundTag serializeBusElement(final HolderLookup.Provider registries) {
        return busElement.serialize();
    }

    public void deserializeBusElement(final CompoundTag tag) {
        busElement.deserialize(tag);
    }

    public void saveItems(final RestrictedContainer container) {
        deviceItems.saveItems(container);
    }

    public void loadItems(final HolderLookup.Provider provider, final RestrictedContainer container) {
        deviceItems.loadItems(provider, container);
    }

    ///////////////////////////////////////////////////////////////////

    private final class RobotItemStackHandlers extends AbstractVMItemStackHandlers {
        public RobotItemStackHandlers(final java.util.function.Supplier<HolderLookup.Provider> providerSupplier) {
            super(providerSupplier,
                new GroupDefinition(DeviceTypes.MEMORY, MEMORY_SLOTS),
                new GroupDefinition(DeviceTypes.HARD_DRIVE, HARD_DRIVE_SLOTS),
                new GroupDefinition(DeviceTypes.FLASH_MEMORY, FLASH_MEMORY_SLOTS),
                new GroupDefinition(DeviceTypes.ROBOT_MODULE, MODULE_SLOTS),
                new GroupDefinition(DeviceTypes.CPU, CPU_SLOTS));
        }

        @Override
        protected ItemDeviceQuery makeQuery(final ItemStack stack) {
            return Devices.makeQuery(robot, stack);
        }

        @Override
        protected void onChanged() {
            super.onChanged();
            if (!robot.level().isClientSide()) {
                onDeviceChanged.run();
            }
        }
    }

    private final class RobotBusElement extends AbstractDeviceBusElement {
        private static final String DEVICE_ID_TAG_NAME = "device_id";

        private final Device device = new ObjectDevice(robot.new RobotDevice(), "robot");
        private UUID deviceId = UUID.randomUUID();

        @Override
        public Optional<Collection<DeviceBusElement>> getNeighbors() {
            return Optional.of(java.util.Collections.singleton(deviceItems.busElement));
        }

        @Override
        public Collection<Device> getLocalDevices() {
            return java.util.Collections.singleton(device);
        }

        @Override
        public Optional<UUID> getDeviceIdentifier(final Device device) {
            if (device == this.device) {
                return Optional.of(deviceId);
            }
            return super.getDeviceIdentifier(device);
        }

        public CompoundTag serialize() {
            final CompoundTag tag = new CompoundTag();
            tag.putUUID(DEVICE_ID_TAG_NAME, deviceId);
            return tag;
        }

        public void deserialize(final CompoundTag tag) {
            if (tag.hasUUID(DEVICE_ID_TAG_NAME)) {
                deviceId = tag.getUUID(DEVICE_ID_TAG_NAME);
            }
        }
    }
}
