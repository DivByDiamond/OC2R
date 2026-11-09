package li.cil.oc2.common.tablet.radio;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.common.bus.element.AbstractDeviceBusElement;

/** Root of the tablet's bus: the radio device of the tablet itself beside its processor and flash. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "The root connects the live inventory bus element.")
public final class TabletBusElement extends AbstractDeviceBusElement {
    private final DeviceBusElement items;
    private final Device device;
    private final UUID deviceId;
    private final Device display;
    private final UUID displayId;

    public TabletBusElement(final DeviceBusElement items, final TabletRadio radio,
            final UUID deviceId, final Device display) {
        super();
        this.items = items;
        // Existing ochat discovers the radio RPC through this compatibility type.
        this.device = new ObjectDevice(new TabletDevice(radio), "tablet", "gps_tower");
        this.deviceId = deviceId;
        this.display = display;
        this.displayId = UUID.nameUUIDFromBytes(
                (deviceId + ":display").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Override
    public Optional<Collection<DeviceBusElement>> getNeighbors() {
        return Optional.of(List.of(items));
    }

    @Override
    public Collection<Device> getLocalDevices() {
        return List.of(device, display);
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device other) {
        if (other.equals(display)) return Optional.of(displayId);
        return other.equals(device) ? Optional.of(deviceId) : super.getDeviceIdentifier(other);
    }
}
