package li.cil.oc2.common.bus.controller;

import java.time.Duration;
import java.util.*;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.util.TickUtils;

final class BusElementManager {
    private static final int MAX_BUS_ELEMENT_COUNT = 128;
    private static final int INCOMPLETE_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));
    private static final int BAD_CONFIGURATION_RETRY_INTERVAL =
            TickUtils.toTicks(Duration.ofSeconds(5));

    private final CommonDeviceBusController controller;
    private final DeviceBusElement root;
    private final int baseEnergyConsumption;

    private final Set<DeviceBusElement> elements = new HashSet<>();
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
        if (scanDelay-- > 0) {
            return;
        }
        collectBusElements()
                .ifPresent(
                        optionals -> {
                            final HashSet<DeviceBusElement> addedElements =
                                    updateElements(optionals.keySet());
                            if (checkOtherBusControllers()) {
                                return;
                            }
                            addedElements.remove(root);
                            controller.scanDevices();
                            updateEnergyConsumption();
                            state = BusState.READY;
                            controller.onAfterBusScan();
                        });
    }

    private void clearElements() {
        for (final DeviceBusElement element : elements) {
            element.removeController(controller);
        }
        elements.clear();
        controller.scanDevices();
    }

    private Optional<HashMap<DeviceBusElement, DeviceBusElement>> collectBusElements() {
        final HashSet<DeviceBusElement> closed = new HashSet<>();
        final Stack<DeviceBusElement> open = new Stack<>();
        final HashMap<DeviceBusElement, DeviceBusElement> optionals = new HashMap<>();

        closed.add(root);
        open.add(root);
        optionals.put(root, null);

        while (!open.isEmpty()) {
            final DeviceBusElement element = open.pop();

            final Optional<Collection<DeviceBusElement>> elementNeighbors = element.getNeighbors();
            if (elementNeighbors.isEmpty()) {
                scanDelay = INCOMPLETE_RETRY_INTERVAL;
                state = BusState.INCOMPLETE;
                clearElements();
                return Optional.empty();
            }

            for (final DeviceBusElement neighborElement : elementNeighbors.get()) {
                if (neighborElement != null) {
                    if (closed.add(neighborElement)) {
                        open.add(neighborElement);
                        optionals.put(neighborElement, neighborElement);
                    }
                }
            }

            if (closed.size() > MAX_BUS_ELEMENT_COUNT) {
                scanDelay = BAD_CONFIGURATION_RETRY_INTERVAL;
                state = BusState.TOO_COMPLEX;
                clearElements();
                return Optional.empty();
            }
        }

        return Optional.of(optionals);
    }

    private HashSet<DeviceBusElement> updateElements(final Set<DeviceBusElement> newElements) {
        final HashSet<DeviceBusElement> removedElements = new HashSet<>(elements);
        removedElements.removeAll(newElements);

        elements.removeAll(removedElements);

        for (final DeviceBusElement removedElement : removedElements) {
            removedElement.removeController(controller);
            for (final DeviceBusController otherController : removedElement.getControllers()) {
                otherController.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
            }
        }

        final HashSet<DeviceBusElement> addedElements = new HashSet<>(newElements);
        addedElements.removeAll(elements);

        elements.addAll(addedElements);

        for (final DeviceBusElement element : addedElements) {
            element.addController(controller);
        }
        return addedElements;
    }

    private boolean checkOtherBusControllers() {
        final HashSet<DeviceBusController> controllers = new HashSet<>();
        for (final DeviceBusElement element : elements) {
            controllers.addAll(element.getControllers());
        }

        controllers.remove(controller);

        if (controllers.isEmpty()) {
            return false;
        }

        for (final DeviceBusController otherController : controllers) {
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