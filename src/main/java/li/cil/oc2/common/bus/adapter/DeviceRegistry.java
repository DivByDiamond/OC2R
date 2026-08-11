package li.cil.oc2.common.bus.adapter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.*;
import li.cil.oc2.common.bus.device.rpc.RPCDeviceList;

public class DeviceRegistry {
    public final List<RPCDeviceWithIdentifier> devicesWithId = new ArrayList<>();
    public final Map<UUID, RPCDeviceList> devicesById = new ConcurrentHashMap<>();
    final Set<RPCDeviceList> unmountedDevices = new HashSet<>();
    final Set<RPCDeviceList> mountedDevices = new HashSet<>();
    final List<RPCEventSource> subscriptions = new ArrayList<>();

    void mountDevices() {
        for (final RPCDevice device : unmountedDevices) {
            device.mount();
        }
        mountedDevices.addAll(unmountedDevices);
        unmountedDevices.clear();
    }

    void unmountDevices() {
        for (final RPCDevice device : mountedDevices) {
            device.unmount();
        }
        unmountedDevices.addAll(mountedDevices);
        mountedDevices.clear();
    }

    void disposeDevices(final IEventSink eventSink) {
        for (final RPCEventSource res : subscriptions) {
            res.unsubscribe(eventSink);
        }
        unmountDevices();
        unmountedDevices.forEach(RPCDeviceList::dispose);
    }

    void rebuild(final DeviceBusController controller) {
        final Map<UUID, List<RPCDevice>> devicesByIdentifier = new ConcurrentHashMap<>();
        for (final Device device : controller.getDevices()) {
            if (device instanceof final RPCDevice rpcDevice) {
                final Set<UUID> identifiers = controller.getDeviceIdentifiers(device);
                for (final UUID identifier : identifiers) {
                    devicesByIdentifier
                            .computeIfAbsent(identifier, unused -> new ArrayList<>())
                            .add(rpcDevice);
                }
            }
        }

        final Map<RPCDeviceList, List<UUID>> identifiersByDevice = new ConcurrentHashMap<>();
        devicesByIdentifier.forEach(
                (identifier, devices) -> {
                    final RPCDeviceList device = new RPCDeviceList(devices);
                    if (device.getMethodGroups().isEmpty()) return;
                    identifiersByDevice
                            .computeIfAbsent(device, unused -> new ArrayList<>())
                            .add(identifier);
                });

        devicesWithId.clear();
        devicesById.clear();

        final Set<RPCDeviceList> devices = new HashSet<>();
        identifiersByDevice.forEach(
                (device, identifiers) -> {
                    final UUID id = selectIdentifierDeterministically(identifiers);
                    devicesWithId.add(new RPCDeviceWithIdentifier(id, device));
                    devicesById.put(id, device);
                    devices.add(device);
                    if (!mountedDevices.contains(device)) {
                        unmountedDevices.add(device);
                    }
                });

        final Set<RPCDeviceList> removedMountedDevices = new HashSet<>(mountedDevices);
        removedMountedDevices.removeAll(devices);
        mountedDevices.removeAll(removedMountedDevices);
        removedMountedDevices.forEach(RPCDeviceList::unmount);

        unmountedDevices.retainAll(devices);
    }

    public void subscribe(
            final IEventSink eventSink, final UUID deviceId, final Consumer<String> errorWriter) {
        final RPCDeviceList devices = devicesById.get(deviceId);
        if (devices != null) {
            for (final RPCDevice device : devices.getDevices()) {
                if (device instanceof final ObjectDevice od) {
                    final RPCEventSource source = od.asEventSource();
                    if (source != null) {
                        source.subscribe(eventSink, deviceId);
                        subscriptions.add(source);
                        return;
                    }
                }
                if (device instanceof final RPCEventSource source) {
                    source.subscribe(eventSink, deviceId);
                    subscriptions.add(source);
                    return;
                }
            }
            errorWriter.accept("device does not support subscriptions");
        } else {
            errorWriter.accept("unknown device");
        }
    }

    public void unsubscribe(
            final IEventSink eventSink, final UUID deviceId, final Consumer<String> errorWriter) {
        final RPCDeviceList devices = devicesById.get(deviceId);
        if (devices != null) {
            for (final RPCDevice device : devices.getDevices()) {
                if (device instanceof final RPCEventSource source) {
                    source.unsubscribe(eventSink);
                    subscriptions.remove(source);
                } else {
                    errorWriter.accept("device does not support subscriptions");
                }
            }
        } else {
            errorWriter.accept("unknown device");
        }
    }

    private UUID selectIdentifierDeterministically(final List<UUID> identifiers) {
        UUID lowest = identifiers.get(0);
        for (int i = 1; i < identifiers.size(); i++) {
            final UUID identifier = identifiers.get(i);
            if (identifier.compareTo(lowest) < 0) {
                lowest = identifier;
            }
        }
        return lowest;
    }
}