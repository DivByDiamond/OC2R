package li.cil.oc2.common.bus.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.DeviceBusElement;

/**
 * Keeps one controller's element set in sync with the elements found by the last scan and hands
 * newly claimed elements over to the controllers that already had them. Runs on the server
 * thread during a scan; no locking involved.
 */
final class BusElementMembership {
    private final CommonDeviceBusController controller;
    private final Set<DeviceBusElement> elements;

    /**
     * @param controller the controller whose membership is maintained
     * @param elements   that controller's live element set, mutated in place by this class;
     *                   it stays owned by {@link BusElementManager}
     */
    BusElementMembership(
            final CommonDeviceBusController controller, final Set<DeviceBusElement> elements) {
        this.controller = controller;
        this.elements = elements;
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
    Set<DeviceBusElement> updateElements(final Set<DeviceBusElement> newElements) {
        final List<DeviceBusElement> removedElements = new ArrayList<>();
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
     * Hands newly claimed elements over to the controllers that already had them.
     *
     * <p>They rebuild their device map <em>synchronously</em> first, so the previous owner drops the
     * element's devices before this controller mounts them: ownership is re-derived from {@link
     * DeviceBusElement#getControllers()} inside {@code scanDevices()}, which already includes this
     * controller thanks to {@link #updateElements}. Afterwards they are asked to run their own
     * topology scan on their next tick; that scan only notifies back when its element set changes,
     * so the round trip settles after one round. Runs on the server thread, no locking involved.
     */
    void handOverAddedElements(final Set<DeviceBusElement> addedElements) {
        final Set<DeviceBusController> others = new HashSet<>();
        for (final DeviceBusElement element : addedElements) {
            for (final DeviceBusController other : element.getControllers()) {
                if (!other.equals(controller)) {
                    others.add(other);
                }
            }
        }
        for (final DeviceBusController other : others) {
            other.scanDevices();
            other.scheduleBusScan(DeviceBusController.ScanReason.BUS_CHANGE);
        }
    }
}
