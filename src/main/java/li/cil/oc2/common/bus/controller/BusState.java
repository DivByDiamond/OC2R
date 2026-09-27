package li.cil.oc2.common.bus.controller;

public enum BusState {
    SCAN_PENDING,
    INCOMPLETE,
    /**
     * No longer produced: a bus over the configured element limit now leaves the extra elements out
     * instead of stopping. Kept so saved and synced values still decode.
     */
    TOO_COMPLEX,
    /**
     * No longer produced: controllers may share a bus and devices are assigned to one owner. Kept so
     * saved and synced values still decode.
     */
    MULTIPLE_CONTROLLERS,
    READY,
}
