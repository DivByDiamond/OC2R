package li.cil.oc2.common.bus.controller;

import java.time.Duration;
import java.util.*;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.tick.TickUtils;

final class BusElementManager {
    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    /** Delay before re-scanning after an incomplete bus (a neighbor returned no neighbors yet). */
    private static final int INCOMPLETE_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));
    /**
     * Delay before re-scanning after a configuration the player must fix (bus too complex,
     * or multiple controllers fighting over the same bus).
     */
    private static final int BAD_CONFIGURATION_RETRY_INTERVAL =
            TickUtils.toTicks(Duration.ofSeconds(5));

    private final CommonDeviceBusController controller;
    private final DeviceBusElement root;
    private final int baseEnergyConsumption;

    private final Set<DeviceBusElement> elements = new HashSet<>();
    // Scratch collections reused across scans; scans run on the server thread only.
    private final Set<DeviceBusElement> bfsClosed = new HashSet<>();
    private final Deque<DeviceBusElement> bfsOpen = new ArrayDeque<>();
    private final Set<DeviceBusElement> collectedElements = new HashSet<>();
    private final List<DeviceBusElement> removedElements = new ArrayList<>();
    private final Set<DeviceBusController> otherControllers = new HashSet<>();
    /**
     * Current state of the bus state machine ({@link BusState}). Only ever changed on the
     * server thread by this class: {@link #scheduleBusScan} resets to {@code SCAN_PENDING};
     * {@link #scan} transitions to {@code INCOMPLETE}/{@code TOO_COMPLEX} while collecting
     * elements, to {@code MULTIPLE_CONTROLLERS} when another controller is detected, and
     * to {@code READY} on a successful scan.
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
                && state.ordinal() < BusState.READY.ordinal()) {
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
        if (checkOtherBusControllers()) {
            return;
        }
        addedElements.remove(root);
        controller.scanDevices();
        updateEnergyConsumption();
        state = BusState.READY;
        controller.onAfterBusScan();
    }

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(controller);
        }
        elements.clear();
        controller.scanDevices();
    }

    private boolean collectBusElements() {
        bfsClosed.clear();
        bfsOpen.clear();
        collectedElements.clear();

        bfsClosed.add(root);
        bfsOpen.add(root);
        collectedElements.add(root);

        while (!bfsOpen.isEmpty()) {
            final DeviceBusElement element = bfsOpen.pop();

            final Optional<Collection<DeviceBusElement>> elementNeighbors = element.getNeighbors();
            if (elementNeighbors.isEmpty()) {
                scanDelay = INCOMPLETE_RETRY_INTERVAL;
                state = BusState.INCOMPLETE;
                clearElements();
                return false;
            }

            for (final DeviceBusElement neighborElement : elementNeighbors.get()) {
                if (neighborElement != null && bfsClosed.add(neighborElement)) {
                    bfsOpen.add(neighborElement);
                    collectedElements.add(neighborElement);
                }
            }

            if (bfsClosed.size() > MAX_BUS_ELEMENT_COUNT) {
                scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
                state = BusState.TOO_COMPLEX;
                clearElements();
                return false;
            }
        }

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

    private boolean checkOtherBusControllers() {
        otherControllers.clear();
        for (final DeviceBusElement element : elements) {
            otherControllers.addAll(element.getControllers());
        }

        otherControllers.remove(controller);

        if (otherControllers.isEmpty()) {
            return false;
        }

        for (final DeviceBusController otherController : otherControllers) {
            otherController.scheduleBusScan(DeviceBusController.ScanReason.BUS_ERROR);
        }

        state = BusState.MULTIPLE_CONTROLLERS;
        scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
        return true;
    }

    private void updateEnergyConsumption() {
        double accumulator = baseEnergyConsumption;
        for (final DeviceBusElement element : elements) {
            accumulator += Math.max(0, element.getEnergyConsumption());
        }

        if (accumulator > Integer.MAX_VALUE) {
            energyConsumption = Integer.MAX_VALUE;
        } else {
            energyConsumption = (int) Math.ceil(accumulator);
        }
    }
}