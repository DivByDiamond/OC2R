package li.cil.oc2.common.bus.controller;

import java.time.Duration;
import java.util.*;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.bus.topology.NetworkResolver;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.util.tick.TickUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class BusElementManager {
    private static final Logger LOGGER = LogManager.getLogger();
    /** Delay before re-scanning after an incomplete bus (a neighbor returned no neighbors yet). */
    private static final int INCOMPLETE_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));
    /** Scans slower than this mean a runaway maxBusElements config; warn so it stays visible. */
    private static final long SLOW_SCAN_WARN_MILLIS = 10;

    private final CommonDeviceBusController controller;
    private final DeviceBusElement root;
    private final int baseEnergyConsumption;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final BusElementMembership membership;
    private final Set<DeviceBusElement> collectedElements = new HashSet<>();
    private int overflowCount;
    /**
     * Current state of the bus state machine ({@link BusState}). Only ever changed on the
     * server thread by this class: {@link #scheduleBusScan} resets to {@code SCAN_PENDING};
     * {@link #scan} transitions to {@code INCOMPLETE} while a neighbor cannot report its
     * neighbors yet, and to {@code READY} on a successful scan. Sharing a bus with other
     * controllers and exceeding the element limit are not errors: devices are assigned to
     * one owner and elements over the limit are left out.
     */
    private BusState state = BusState.SCAN_PENDING;
    private int scanDelay;
    private int energyConsumption;

    BusElementManager(
            final CommonDeviceBusController controller,
            final DeviceBusElement root,
            final int baseEnergyConsumption) {
        this.controller = controller;
        this.root = root;
        this.baseEnergyConsumption = baseEnergyConsumption;
        this.membership = new BusElementMembership(controller, elements);
    }

    void dispose() {
        for (final DeviceBusElement element : elements) {
            element.removeController(controller);
            for (final DeviceBusController otherController : element.getControllers()) {
                otherController.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
            }
        }
        elements.clear();
    }

    BusState getState() {
        return state;
    }

    int getEnergyConsumption() {
        return energyConsumption;
    }

    Collection<DeviceBusElement> getElements() {
        return elements;
    }

    /**
     * Schedules a bus scan for the next update.
     *
     * <p>{@code BUS_ERROR} reports are ignored while this controller is not {@code READY}:
     * they typically come from other controllers that also see this bus as broken, and
     * acting on them would make the controllers re-scan each other in a loop. Only a
     * controller that believed the bus was fine reacts to an error report.
     */
    void scheduleBusScan(final DeviceBusController.ScanReason reason) {
        if (reason == DeviceBusController.ScanReason.BUS_ERROR
                && state != BusState.READY) {
            return;
        }
        scanDelay = 0;
        state = BusState.SCAN_PENDING;
    }

    void scan() {
        if (scanDelay < 0) {
            return;
        }
        final int delay = scanDelay--;
        if (delay > 0) {
            return;
        }
        final long startNanos = System.nanoTime();
        if (!collectBusElements(startNanos)) {
            return;
        }
        final Set<DeviceBusElement> addedElements = membership.updateElements(collectedElements);
        membership.handOverAddedElements(addedElements);
        controller.scanDevices();
        updateEnergyConsumption();
        state = BusState.READY;
        controller.onAfterBusScan();
        warnIfSlowScan(startNanos, collectedElements.size(), overflowCount);
    }

    int getOverflowCount() {
        return overflowCount;
    }

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(controller);
            // Wake the remaining controllers: we may have been the bridge between their buses, so
            // their devices stay ownerless until someone re-scans. Self is already removed.
            for (final DeviceBusController other : element.getControllers()) {
                other.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
            }
        }
        elements.clear();
        controller.scanDevices();
    }

    private boolean collectBusElements(final long startNanos) {
        final NetworkResolver.Result<DeviceBusElement> result =
                NetworkResolver.resolve(root, DeviceBusElement::getNeighbors, Config.maxBusElements);
        if (result.incomplete()) {
            scanDelay = INCOMPLETE_RETRY_INTERVAL;
            state = BusState.INCOMPLETE;
            clearElements();
            warnIfSlowScan(startNanos, result.nodes().size(), result.overflow().size());
            return false;
        }

        if (result.overflow().size() != overflowCount) {
            LOGGER.warn(
                    "Device bus of {} exceeds maxBusElements ({}); {} element(s) are left out.",
                    root,
                    Config.maxBusElements,
                    result.overflow().size());
        }
        overflowCount = result.overflow().size();

        collectedElements.clear();
        collectedElements.addAll(result.nodes());
        return true;
    }

    private void warnIfSlowScan(final long startNanos, final int nodeCount, final int overflow) {
        final long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000L;
        if (elapsedMillis >= SLOW_SCAN_WARN_MILLIS) {
            LOGGER.warn(
                    "Bus scan of {} took {} ms: {} node(s), {} over the maxBusElements limit of {}.",
                    root, elapsedMillis, nodeCount, overflow, Config.maxBusElements);
        }
    }

    private void updateEnergyConsumption() {
        double accumulator = baseEnergyConsumption;
        for (final DeviceBusElement element : elements) {
            if (controller.isOwnerOf(element)) {
                accumulator += Math.max(0, element.getEnergyConsumption());
            }
        }

        if (accumulator > Integer.MAX_VALUE) {
            energyConsumption = Integer.MAX_VALUE;
        } else {
            energyConsumption = (int) Math.ceil(accumulator);
        }
    }
}