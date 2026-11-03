package li.cil.oc2.common.blockentity.gps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class GpsNetworkTest {
    private static final UUID NETWORK = UUID.randomUUID();

    @Test
    void independentNetworksDoNotBorrowNearbyReferencesOrRelays() {
        final GpsNetwork graph = new GpsNetwork();
        final GpsNetworkNode base = node(0, NETWORK, true);
        final GpsNetworkNode station = node(19, UUID.randomUUID(), false);
        graph.put(base);
        graph.put(station);
        assertEquals("GPS reference is not configured", assertThrows(IllegalStateException.class,
                () -> graph.offset(station.id(), 20)).getMessage());
    }

    @Test
    void relayChainWorksFromPersistedNodesAndBreakingItRemovesSignal() {
        final GpsNetwork graph = new GpsNetwork();
        final GpsNetworkNode base = node(0, NETWORK, true);
        final GpsNetworkNode relay = node(20, NETWORK, false);
        final GpsNetworkNode station = node(40, NETWORK, false);
        graph.put(base);
        graph.put(relay);
        graph.put(station);
        assertEquals(new BlockPos(40, 0, 0), graph.offset(station.id(), 20));
        graph.remove(relay.id());
        assertEquals("GPS signal is unavailable", assertThrows(IllegalStateException.class,
                () -> graph.offset(station.id(), 20)).getMessage());
        graph.put(node(20, UUID.randomUUID(), false));
        assertThrows(IllegalStateException.class, () -> graph.offset(station.id(), 20));
    }

    @Test
    void rangeIsCircularInXZAndIgnoresHeightWithoutChangingCoordinates() {
        final GpsNetwork graph = new GpsNetwork();
        graph.put(node(0, NETWORK, true));
        final GpsNetworkNode station = node(21, NETWORK, false);
        graph.put(station);
        assertThrows(IllegalStateException.class, () -> graph.offset(station.id(), 20));
        assertEquals(new BlockPos(21, 0, 0), graph.offset(station.id(), 21));
        graph.put(new GpsNetworkNode(station.id(), new BlockPos(20, -1000, -1234), NETWORK, false));
        assertEquals(new BlockPos(20, -1070, 0), graph.offset(station.id(), 20));
        graph.put(new GpsNetworkNode(station.id(), new BlockPos(14, 1000, -1220), NETWORK, false));
        assertEquals(new BlockPos(14, 930, 14), graph.offset(station.id(), 20));
        graph.put(new GpsNetworkNode(station.id(), new BlockPos(15, 1000, -1219), NETWORK, false));
        assertThrows(IllegalStateException.class, () -> graph.offset(station.id(), 20));
    }

    @Test
    void fortyFiveBlockHopsIncludeTheBoundaryAndSupportRelayChains() {
        final GpsNetwork graph = new GpsNetwork();
        graph.put(node(0, NETWORK, true));
        final GpsNetworkNode relay = node(45, NETWORK, false);
        final GpsNetworkNode station = node(90, NETWORK, false);
        graph.put(relay);
        graph.put(station);
        assertEquals(new BlockPos(90, 0, 0), graph.offset(station.id(), 45));
        graph.remove(relay.id());
        assertThrows(IllegalStateException.class, () -> graph.offset(station.id(), 45));
        final GpsNetworkNode outside = node(46, NETWORK, false);
        graph.put(outside);
        assertThrows(IllegalStateException.class, () -> graph.offset(outside.id(), 45));
        graph.put(new GpsNetworkNode(outside.id(), new BlockPos(45, 71, -1234), NETWORK, false));
        assertEquals(new BlockPos(45, 1, 0), graph.offset(outside.id(), 45));
    }

    @Test
    void conflictsNeverChooseAnArbitraryReference() {
        final GpsNetwork graph = new GpsNetwork();
        final GpsNetworkNode base = node(0, NETWORK, true);
        graph.put(base);
        graph.put(node(1, NETWORK, true));
        assertEquals("GPS network has multiple references", assertThrows(IllegalStateException.class,
                () -> graph.offset(base.id(), 20)).getMessage());
    }

    @Test
    void replacingAStationAtTheSamePositionDoesNotLeaveAGhostRelay() {
        final GpsNetwork graph = new GpsNetwork();
        final GpsNetworkNode base = node(0, NETWORK, true);
        graph.put(base);
        graph.put(node(0, UUID.randomUUID(), false));
        assertFalse(graph.hasReference(NETWORK));
        assertEquals(1, graph.nodes().size());
    }

    private static GpsNetworkNode node(final int x, final UUID network, final boolean reference) {
        return new GpsNetworkNode(UUID.randomUUID(), new BlockPos(x, 70, -1234), network, reference);
    }
}
