package li.cil.oc2.common.bus.controller;

import static java.util.Collections.emptySet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.controller.event.AfterDeviceScanEvent;
import li.cil.oc2.common.bus.controller.event.DevicesChangedEvent;
import li.cil.oc2.common.util.event.Event;
import li.cil.oc2.common.util.event.ParameterizedEvent;

public class CommonDeviceBusController implements DeviceBusController {
    public final Set<Runnable> afterBusScanListeners = new Event();
    public final Set<Runnable> beforeDeviceScanListeners = new Event();
    public final Set<Consumer<AfterDeviceScanEvent>> afterDeviceScanListeners =
            new ParameterizedEvent<>();
    public final Set<Consumer<DevicesChangedEvent>> devicesAddedListeners =
            new ParameterizedEvent<>();
    public final Set<Consumer<DevicesChangedEvent>> devicesRemovedListeners =
            new ParameterizedEvent<>();

    private final BusElementManager manager;
    private final Set<Device> devices = new HashSet<>();
    private final Map<Device, Set<UUID>> deviceIds = new ConcurrentHashMap<>();

    public CommonDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption) {
        this.manager = new BusElementManager(this, root, baseEnergyConsumption);
    }

    public void setDeviceContainersChanged() {}

    public void dispose() {
        manager.dispose();
    }

    public BusState getState() {
        return manager.getState();
    }

    public int getEnergyConsumption() {
        return manager.getEnergyConsumption();
    }

    @Override
    public void scheduleBusScan(final ScanReason reason) {
        manager.scheduleBusScan(reason);
    }

    @Override
    public void scanDevices() {
        onBeforeDeviceScan();

        final Set<Device> newDevices = new HashSet<>();
        final Map<Device, Set<UUID>> newDeviceIds = new ConcurrentHashMap<>();
        for (final DeviceBusElement element : manager.getElements()) {
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
        onDevicesRemoved(removedDevices);

        final Set<Device> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(devices);
        onDevicesAdded(addedDevices);

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

        onAfterDeviceScan(didDevicesChange || didDeviceIdsChange);
    }

    @Override
    public Set<Device> getDevices() {
        return devices;
    }

    @Override
    public Set<UUID> getDeviceIdentifiers(final Device device) {
        return deviceIds.getOrDefault(device, emptySet());
    }

    public void scan() {
        manager.scan();
    }

    protected Collection<DeviceBusElement> getElements() {
        return manager.getElements();
    }

    protected void onAfterBusScan() {
        afterBusScanListeners.forEach(Runnable::run);
    }

    protected void onBeforeDeviceScan() {
        beforeDeviceScanListeners.forEach(Runnable::run);
    }

    protected void onAfterDeviceScan(final boolean didDevicesChange) {
        final var event = new AfterDeviceScanEvent(didDevicesChange);
        afterDeviceScanListeners.forEach(c -> c.accept(event));
    }

    protected void onDevicesAdded(final Collection<Device> devices) {
        final var event = new DevicesChangedEvent(devices);
        devicesAddedListeners.forEach(c -> c.accept(event));
    }

    protected void onDevicesRemoved(final Collection<Device> devices) {
        final var event = new DevicesChangedEvent(devices);
        devicesRemovedListeners.forEach(c -> c.accept(event));
    }
}