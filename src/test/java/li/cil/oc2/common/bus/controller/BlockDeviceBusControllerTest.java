package li.cil.oc2.common.bus.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import li.cil.oc2.api.bus.DeviceBusElement;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockDeviceBusControllerTest {
    private DeviceBusElement root;
    private AtomicReference<BlockPos> position;
    private BlockDeviceBusController controller;

    @BeforeEach
    void setUp() {
        root = mock(DeviceBusElement.class);
        when(root.getNeighbors()).thenReturn(Optional.of(List.of()));
        position = new AtomicReference<>(new BlockPos(17, 64, -33));
        // A detached host is a real lifecycle state. No loader-owned block entity or world
        // is needed to exercise ownership, bus discovery, notification and disposal.
        controller = new BlockDeviceBusController(root, 100, position::get, () -> null);
    }

    @Test
    void ownershipKeyUsesCurrentHostBlockPosition() {
        assertEquals(new BlockPos(17, 64, -33).asLong(), controller.getOwnershipKey());

        position.set(new BlockPos(-48, -16, 97));

        assertEquals(new BlockPos(-48, -16, 97).asLong(), controller.getOwnershipKey());
    }

    @Test
    void detachedHostStillRegistersElementsAndNotifiesAfterScan() {
        final AtomicInteger notifications = new AtomicInteger();
        controller.afterBusScanListeners.add(notifications::incrementAndGet);

        controller.scan();

        assertEquals(BusState.READY, controller.getState());
        assertEquals(Set.of(root), Set.copyOf(controller.getElements()));
        assertEquals(100, controller.getEnergyConsumption());
        assertEquals(1, notifications.get());
        verify(root).addController(controller);
        // The dirty-chunk operation is harmless for a detached host without tracked chunks;
        // it must preserve the successfully discovered bus rather than reset it.
        controller.setDeviceContainersChanged();
        assertEquals(BusState.READY, controller.getState());
        assertEquals(Set.of(root), Set.copyOf(controller.getElements()));
    }

    @Test
    void disposeDetachesScannedElementsExactlyOnce() {
        final DeviceBusElement neighbor = mock(DeviceBusElement.class);
        when(root.getNeighbors()).thenReturn(Optional.of(List.of(neighbor)));
        when(neighbor.getNeighbors()).thenReturn(Optional.of(List.of(root)));
        controller.scan();
        assertEquals(Set.of(root, neighbor), Set.copyOf(controller.getElements()));
        verify(root).addController(controller);
        verify(neighbor).addController(controller);

        controller.dispose();
        controller.dispose();

        assertTrue(controller.getElements().isEmpty());
        verify(root, times(1)).removeController(controller);
        verify(neighbor, times(1)).removeController(controller);
    }
}
