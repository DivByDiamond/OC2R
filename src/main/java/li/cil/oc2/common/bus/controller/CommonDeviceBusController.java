package li.cil.oc2.common.bus.controller;

import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.Event;
import li.cil.oc2.common.util.ParameterizedEvent;

import java.util.*;
import static java.util.Collections.emptySet;

public class CommonDeviceBusController implements DeviceBusController {
    public final Event onAfterBusScan = new Event();
    public final Event onBeforeDeviceScan = new Event();
    public final ParameterizedEvent<AfterDeviceScanEvent> onAfterDeviceScan = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesAdded = new ParameterizedEvent<>();
    public final ParameterizedEvent<DevicesChangedEvent> onDevicesRemoved = new ParameterizedEvent<>();

    private final BusElementManager manager;
    private final HashSet<Device> devices = new HashSet<>();
    private final HashMap<Device, Set<UUID>> deviceIds = new HashMap<>();

    public CommonDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption) {
        this.manager = new BusElementManager(this, root, baseEnergyConsumption);
    }

    public void setDeviceContainersChanged() {
    }

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

        final HashSet<Device> newDevices = new HashSet<>();
        final HashMap<Device, Set<UUID>> newDeviceIds = new HashMap<>();
        for (final DeviceBusElement element : manager.getElements()) {
            for (final Device device : element.getLocalDevices()) {
                newDevices.add(device);
                element.getDeviceIdentifier(device).ifPresent(identifier -> newDeviceIds
                    .computeIfAbsent(device, unused -> new HashSet<>()).add(identifier));
            }
        }

        final HashSet<Device> removedDevices = new HashSet<>(devices);
        removedDevices.removeAll(newDevices);
        onDevicesRemoved(removedDevices);

        final HashSet<Device> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(devices);
        onDevicesAdded(addedDevices);

        final boolean didDevicesChange = !removedDevices.isEmpty() || !addedDevices.isEmpty();
        final boolean didDeviceIdsChange;
        if (didDevicesChange) {
            devices.clear();
            devices.addAll(newDevices);

            didDeviceIdsChange = true;
        } else {
            didDeviceIdsChange = deviceIds.entrySet().stream().anyMatch(entry ->
                !Objects.equals(entry.getValue(), newDeviceIds.get(entry.getKey())));
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
        onAfterBusScan.run();
    }

    protected void onBeforeDeviceScan() {
        onBeforeDeviceScan.run();
    }

    protected void onAfterDeviceScan(final boolean didDevicesChange) {
        onAfterDeviceScan.accept(new AfterDeviceScanEvent(didDevicesChange));
    }

    protected void onDevicesAdded(final Collection<Device> devices) {
        onDevicesAdded.accept(new DevicesChangedEvent(devices));
    }

    protected void onDevicesRemoved(final Collection<Device> devices) {
        onDevicesRemoved.accept(new DevicesChangedEvent(devices));
    }
}
