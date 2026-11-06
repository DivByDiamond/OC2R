package li.cil.oc2.common.tablet;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import li.cil.oc2.common.bus.controller.BusState;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

class TabletClientViewTest {
    @Test
    void aViewExistsOnlyWhileAScreenHoldsIt() {
        final UUID id = UUID.randomUUID();
        assertTrue(TabletClientView.find(id).isEmpty());
        final TabletClientView view = TabletClientView.acquire(id);
        assertSame(view, TabletClientView.find(id).orElseThrow());
        TabletClientView.release(id);
        assertTrue(TabletClientView.find(id).isEmpty());
    }

    @Test
    void switchingScreensKeepsTheViewWhenTheNewScreenOpensBeforeTheOldOneCloses() {
        final UUID id = UUID.randomUUID();
        final TabletClientView first = TabletClientView.acquire(id);
        final TabletClientView second = TabletClientView.acquire(id);
        assertSame(first, second);
        TabletClientView.release(id);
        assertSame(first, TabletClientView.find(id).orElseThrow());
        TabletClientView.release(id);
        assertTrue(TabletClientView.find(id).isEmpty());
    }

    @Test
    void viewsOfDifferentTabletsAreIndependent() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        final TabletClientView first = TabletClientView.acquire(a);
        final TabletClientView second = TabletClientView.acquire(b);
        assertNotSame(first, second);
        assertNotSame(first.terminal(), second.terminal());
        TabletClientView.release(a);
        TabletClientView.release(b);
    }

    @Test
    void reportedStateDrivesTheErrorText() {
        final UUID id = UUID.randomUUID();
        final TabletClientView view = TabletClientView.acquire(id);
        assertNotNull(view.getError(), "a bus that is not ready is reported");
        assertFalse(view.isRunning());

        view.setBusStateClient(BusState.READY);
        view.setBootErrorClient(Component.literal(""));
        view.setRunStateClient(VMRunState.RUNNING);
        assertTrue(view.isRunning());
        assertNull(view.getError());

        view.setRunStateClient(VMRunState.STOPPED);
        view.setBootErrorClient(Component.literal("no firmware"));
        assertEquals("no firmware", view.getError().getString());
        TabletClientView.release(id);
    }
}
