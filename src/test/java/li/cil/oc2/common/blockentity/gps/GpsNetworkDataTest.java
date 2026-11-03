package li.cil.oc2.common.blockentity.gps;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

class GpsNetworkDataTest {
    @Test
    void persistedNetworkRetainsIdsReferencesAndRoutesWithoutLoadedChunks() {
        final UUID network = UUID.randomUUID();
        final UUID station = UUID.randomUUID();
        final GpsNetworkData data = new GpsNetworkData();
        data.register(new GpsNetworkNode(UUID.randomUUID(), new BlockPos(-200, 64, 100), network, true));
        data.register(new GpsNetworkNode(UUID.randomUUID(), new BlockPos(-180, 64, 100), network, false));
        data.register(new GpsNetworkNode(station, new BlockPos(-160, 64, 100), network, false));
        final var encoded = GpsNetworkData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        final GpsNetworkData restored = GpsNetworkData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(new BlockPos(40, 0, 0), restored.network().offset(station, 20));
        assertEquals(3, restored.network().nodes().size());
    }

    @Test
    void unloadedStationsRemainWithoutBlockReadsAndLoadedMissingStationsAreRemoved() {
        final GpsNetworkData data = new GpsNetworkData();
        final GpsNetworkNode node = new GpsNetworkNode(UUID.randomUUID(), new BlockPos(100, 70, -9), UUID.randomUUID(), true);
        data.register(node);
        final var loaded = new java.util.HashSet<BlockPos>();
        final var blockReads = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.function.Predicate<GpsNetworkNode> missingBlock = station -> {
            blockReads.incrementAndGet();
            return false;
        };
        data.reconcile(loaded::contains, missingBlock);
        assertEquals(1, data.network().nodes().size());
        assertEquals(0, blockReads.get());
        loaded.add(node.position());
        data.reconcile(loaded::contains, missingBlock);
        assertTrue(data.network().nodes().isEmpty());
        assertEquals(1, blockReads.get());
    }

    @Test
    void registryDoesNotSaveUnchangedMembershipAgain() {
        final GpsNetworkData data = new GpsNetworkData();
        final GpsNetworkNode node = new GpsNetworkNode(UUID.randomUUID(), BlockPos.ZERO, UUID.randomUUID(), true);
        data.register(node);
        assertTrue(data.isDirty());
        data.setDirty(false);
        data.register(node);
        assertFalse(data.isDirty());
        data.removeAt(BlockPos.ZERO);
        assertTrue(data.isDirty());
        assertTrue(data.network().nodes().isEmpty());
    }
}
