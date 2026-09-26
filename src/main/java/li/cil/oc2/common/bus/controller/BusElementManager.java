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

    private final CommonDeviceBusController controller;
    private final DeviceBusElement root;
    private final int baseEnergyConsumption;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    private final Set<DeviceBusElement> collectedElements = new HashSet<>();
    private final List<DeviceBusElement> removedElements = new ArrayList<>();
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
        if (!collectBusElements()) {
            return;
        }
        final Set<DeviceBusElement> addedElements = updateElements(collectedElements);
        notifyControllersSharing(addedElements);
        controller.scanDevices();
        updateEnergyConsumption();
        state = BusState.READY;
        controller.onAfterBusScan();
    }

    int getOverflowCount() {
        return overflowCount;
    }

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(controller);
        }
        elements.clear();
        controller.scanDevices();
    }

    private boolean collectBusElements() {
        final NetworkResolver.Result<DeviceBusElement> result =
                NetworkResolver.resolve(root, DeviceBusElement::getNeighbors, Config.maxBusElements);
        if (result.incomplete()) {
            scanDelay = INCOMPLETE_RETRY_INTERVAL;
            state = BusState.INCOMPLETE;
            clearElements();
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

    /**
     * Diffs the currently attached elements against the elements found by the last scan.
     *
     * <p>Removed elements are detached from this controller and, importantly, all their
     * <em>remaining</em> controllers are asked to re-scan ({@code BUS_CHANGE}): the element
     * may have been the bridge that connected those controllers' buses, so their topology
     * may have changed too. Returns the set of newly added elements (still including the
     * root; callers remove it if they do not want it scanned for devices).
     */
    private Set<DeviceBusElement> updateElements(final Set<DeviceBusElement> newElements) {
        removedElements.clear();
        for (final DeviceBusElement element : elements) {
            if (!newElements.contains(element)) {
                removedElements.add(element);
            }
        }
        elements.removeAll(removedElements);

        for (final DeviceBusElement removedElement : removedElements) {
            removedElement.removeController(controller);
            for (final DeviceBusController otherController : removedElement.getControllers()) {
                otherController.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
            }
        }

        final Set<DeviceBusElement> addedElements = new HashSet<>();
        for (final DeviceBusElement element : newElements) {
            if (elements.add(element)) {
                addedElements.add(element);
            }
        }

        for (final DeviceBusElement element : addedElements) {
            element.addController(controller);
        }
        return addedElements;
    }

    /**
     * Asks other controllers that share newly attached elements to re-scan so they re-derive device
     * ownership. They only notify back when their own element set changes, so this settles after one round.
     */
    private void notifyControllersSharing(final Set<DeviceBusElement> addedElements) {
        for (final DeviceBusElement element : addedElements) {
            for (final DeviceBusController other : element.getControllers()) {
                if (!other.equals(controller)) {
                    other.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
                }
            }
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