package li.cil.oc2.common.machine;

import static org.junit.jupiter.api.Assertions.*;

import li.cil.oc2.platform.AbstractEnergyStorage;
import org.junit.jupiter.api.Test;

class MachineEnergyTest {
    @Test
    void transfersUseActualAcceptedAmountAndConserveEnergy() {
        final MachineResources buffer = new MachineResources(() -> {});
        final AbstractEnergyStorage source = new AbstractEnergyStorage(10000, 1000, 1000, 5000);
        assertEquals(1000, buffer.moveEnergy(source, 1000, true));
        assertEquals(5000, source.getEnergyStored() + buffer.energy());
        final AbstractEnergyStorage sink = new AbstractEnergyStorage(150);
        assertEquals(150, buffer.moveEnergy(sink, 1000, false));
        assertEquals(5000, source.getEnergyStored() + buffer.energy() + sink.getEnergyStored());
        assertEquals(0, buffer.moveEnergy(sink, 1000, false));
        assertEquals(850, buffer.energy());
    }

    @Test
    void aFullBufferAndInvalidAmountsDoNotExtractEnergy() {
        final MachineResources buffer = new MachineResources(() -> {});
        final AbstractEnergyStorage source = new AbstractEnergyStorage(20000, 20000, 20000, 20000);
        for (int i = 0; i < 10; i++) assertEquals(1000, buffer.moveEnergy(source, 1000, true));
        assertEquals(0, buffer.moveEnergy(source, 1000, true));
        assertThrows(IllegalArgumentException.class, () -> buffer.moveEnergy(source, -1, true));
        assertThrows(IllegalArgumentException.class, () -> buffer.moveEnergy(source, 1001, true));
        assertEquals(10000, source.getEnergyStored());
    }

    @Test
    void recursiveTransfersAreRejectedBeforeTouchingTheBuffer() {
        final MachineResources buffer = new MachineResources(() -> {});
        final AbstractEnergyStorage source = new AbstractEnergyStorage(1000, 1000, 1000, 1000) {
            @Override
            public int extractEnergy(final int amount, final boolean simulate) {
                assertThrows(IllegalStateException.class, () -> buffer.moveEnergy(this, 1, true));
                return super.extractEnergy(amount, simulate);
            }
        };
        assertEquals(1000, buffer.moveEnergy(source, 1000, true));
        assertEquals(0, source.getEnergyStored());
    }
}
