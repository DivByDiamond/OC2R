package li.cil.oc2.common.bus.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.bus.topology.OwnerResolver;
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
    private final DeviceTable deviceTable = new DeviceTable(this);

    // Root is kept for the controller's lifetime and never handed out, so nothing new escapes.
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "root is only read, never exposed")
    private final DeviceBusElement root;
    private final LongSupplier ownershipKey;

    public CommonDeviceBusController(final DeviceBusElement root, final int baseEnergyConsumption) {
        this(root, baseEnergyConsumption, () -> Long.MAX_VALUE);
    }

    /**
     * Creates a controller whose ownership key for shared bus elements is supplied by the caller.
     *
     * @param ownershipKey supplies {@link #getOwnershipKey()}; it must yield a value that is stable
     *                     for the lifetime of the controller and across restarts (for example one
     *                     derived from a persisted id, read at call time), because shared elements
     *                     are handed to the controller with the smallest key on every scan
     */
    public CommonDeviceBusController(
            final DeviceBusElement root,
            final int baseEnergyConsumption,
            final LongSupplier ownershipKey) {
        this.root = root;
        this.ownershipKey = ownershipKey;
        this.manager = new BusElementManager(this, root, baseEnergyConsumption);
    }

    @Override
    public long getOwnershipKey() {
        return ownershipKey.getAsLong();
    }

    /**
     * Whether this controller owns the devices of {@code element}. An element that is the root of one
     * of its controllers always belongs to that controller (a computer's own devices are never
     * handed to a neighbor); shared cable elements go to the controller with the lowest ownership key.
     */
    boolean isOwnerOf(final DeviceBusElement element) {
        return ownerOf(element).equals(this);
    }

    DeviceBusController ownerOf(final DeviceBusElement element) {
        final Collection<DeviceBusController> controllers = element.getControllers();
        for (final DeviceBusController candidate : controllers) {
            if (candidate instanceof CommonDeviceBusController common && common.root.equals(element)) {
                return common;
            }
        }
        // identityHashCode is only consulted when two controllers report the exact same key. It is
        // per-instance and stable for the lifetime of the JVM run, i.e. ownership stops flipping
        // between scans; anything "better" would have to be persisted, and ownership is
        // deliberately never stored (see OwnerResolver) so it can re-derive on unload.
        return OwnerResolver.owner(
                        controllers,
                        DeviceBusController::getOwnershipKey,
                        Comparator.comparingInt(System::identityHashCode))
                .orElse(this);
    }

    /**
     * Devices this controller can reach but that are owned by another controller, with that owner.
     * Lets tooling report "occupied by ..." instead of the device silently missing.
     *
     * @return an immutable snapshot, taken because the live map is cleared during scans
     */
    public Map<Device, DeviceBusController> getOccupiedDevices() {
        return Map.copyOf(deviceTable.getOccupiedDevices());
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

    /**
     * Elements the last scan had to leave out because the bus exceeds the configured element
     * limit. The bus keeps running with the rest; this is only surfaced to the player.
     */
    public int getBusOverflow() {
        return manager.getOverflowCount();
    }

    @Override
    public void scheduleBusScan(final ScanReason reason) {
        manager.scheduleBusScan(reason);
    }

    @Override
    public void scanDevices() {
        deviceTable.scan();
    }

    @Override
    public Set<Device> getDevices() {
        return deviceTable.getDevices();
    }

    @Override
    public Set<UUID> getDeviceIdentifiers(final Device device) {
        return deviceTable.getDeviceIdentifiers(device);
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