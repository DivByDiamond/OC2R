package li.cil.oc2.common.bus.controller;

import static java.util.Collections.emptySet;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;

/**
 * The device view of one controller: the devices its own elements provide, their identifiers, and
 * the devices owned by neighbouring controllers. Rebuilt from scratch on every device scan, which
 * fires the controller's before/added/removed/after callbacks in the usual order.
 */
final class DeviceTable {
    private final CommonDeviceBusController controller;
    private final Set<Device> devices = new HashSet<>();
    private final Map<Device, Set<UUID>> deviceIds = new ConcurrentHashMap<>();
    private final Map<Device, DeviceBusController> occupiedDevices = new ConcurrentHashMap<>();

    DeviceTable(final CommonDeviceBusController controller) {
        this.controller = controller;
    }

    void scan() {
        controller.onBeforeDeviceScan();

        final Set<Device> newDevices = new HashSet<>();
        occupiedDevices.clear();
        final Map<Device, Set<UUID>> newDeviceIds = new ConcurrentHashMap<>();
        for (final DeviceBusElement element : controller.getElements()) {
            if (!controller.isOwnerOf(element)) {
                final DeviceBusController owner = controller.ownerOf(element);
                for (final Device device : element.getLocalDevices()) {
                    occupiedDevices.put(device, owner);
                }
                continue;
            }
            for (final Device device : element.getLocalDevices()) {
                newDevices.add(device);
                element.getDeviceIdentifier(device)
                        .ifPresent(
                                identifier ->
                        newDeviceIds
                                .computeIfAbsent(// NOPMD: per-device set
                                        device, unused -> new HashSet<>()) // NOPMD allocation depends on loop iteration / per-item state
                                .add(identifier));
            }
        }

        final Set<Device> removedDevices = new HashSet<>(devices);
        removedDevices.removeAll(newDevices);
        controller.onDevicesRemoved(removedDevices);

        final Set<Device> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(devices);
        controller.onDevicesAdded(addedDevices);

        final boolean didDevicesChange = !removedDevices.isEmpty() || !addedDevices.isEmpty();
        final boolean didDeviceIdsChange;
        if (didDevicesChange) {
            devices.clear();
            devices.addAll(newDevices);

            didDeviceIdsChange = true;
        } else {
            didDeviceIdsChange =
                    deviceIds.entrySet().stream()
                            .anyMatch(
                                    entry ->
                                            !Objects.equals(
                                                    entry.getValue(),
                                                    newDeviceIds.get(entry.getKey())));
        }

        if (didDeviceIdsChange) {
            deviceIds.clear();
            deviceIds.putAll(newDeviceIds);
        }

        controller.onAfterDeviceScan(didDevicesChange || didDeviceIdsChange);
    }

    Set<Device> getDevices() {
        return devices;
    }

    Set<UUID> getDeviceIdentifiers(final Device device) {
        return deviceIds.getOrDefault(device, emptySet());
    }

    Map<Device, DeviceBusController> getOccupiedDevices() {
        return occupiedDevices;
    }
}
