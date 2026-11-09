package li.cil.oc2.common.blockentity.radio.dock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import li.cil.oc2.common.blockentity.radio.RadioNetworkData;
import net.minecraft.server.level.ServerLevel;

/**
 * The loaded dock stations of one level that bridge a wired Ethernet network into the radio. Docks
 * are not saved here: each dock registers itself when it loads or sends and is dropped as soon as it
 * unloads or stops being valid, so no chunk is ever loaded for a dock.
 */
@SuppressWarnings("PMD.UseConcurrentHashMap") // Server thread only.
public final class DockBridges {
    static final int MAX_BRIDGES = 256;

    private final Map<UUID, DockBridge> bridges = new LinkedHashMap<>();

    /** Side-effect-free admission check before validating an enrollment invitation. */
    public boolean canRegister(final DockBridge bridge) {
        final DockBridge previous = bridges.get(bridge.id());
        if (previous == bridge) return true; // NOPMD - same live dock
        if (previous != null && previous.valid() && !previous.position().equals(bridge.position())) return false;
        return previous != null || bridges.size() < MAX_BRIDGES;
    }

    /** Copied block data cannot move an identity to another dock while the first one is still loaded. */
    public boolean register(final DockBridge bridge) {
        if (!canRegister(bridge)) return false;
        bridges.put(bridge.id(), bridge);
        return true;
    }

    public void remove(final DockBridge bridge) {
        bridges.remove(bridge.id(), bridge);
    }

    /** Enrolled docks that reach the radio through the given tower, other than the one the frame came from. */
    public List<DockBridge> attachedTo(
            final ServerLevel level, final RadioNetworkData data, final UUID tower, final UUID except) {
        bridges.values().removeIf(bridge -> !bridge.valid());
        final List<DockBridge> result = new ArrayList<>();
        for (final DockBridge bridge : bridges.values()) {
            if (bridge.id().equals(except) || !bridge.enrolled()) continue;
            final var attachment = data.bridgeAccess(level, bridge.id(), bridge.credential(), bridge.x(), bridge.z());
            if (attachment.isPresent() && attachment.get().tower().device().equals(tower)) result.add(bridge);
        }
        return result;
    }

    public boolean anyAttachedTo(final ServerLevel level, final RadioNetworkData data, final UUID tower) {
        return !attachedTo(level, data, tower, null).isEmpty();
    }
}
