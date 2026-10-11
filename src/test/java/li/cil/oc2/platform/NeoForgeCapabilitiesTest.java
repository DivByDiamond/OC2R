package li.cil.oc2.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
//? if <26.1 {
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
//?}
//? if >=26.1 {
/*import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
*///?} else {
import net.neoforged.neoforge.energy.IEnergyStorage;
//?}
import org.junit.jupiter.api.Test;

class NeoForgeCapabilitiesTest {
    @Test
    void unwrapNullStorageReturnsNull() {
        assertNull(NeoForgeCapabilities.toNeoForge(EnergyStorage.class, null, null));
    }

    // Device-bus identity (ObjectDevice -> EnergyStorageDevice/IdentityProxy) is defined via
    // equals/hashCode, never via instance identity: the same underlying storage must compare
    // equal across separate queries even though each query builds a fresh wrapper. assertNotSame
    // keeps a reintroduced static wrapper cache from slipping back in unnoticed -- such a cache
    // was an unbounded memory leak (one entry per query, entries outliving their owners).
    @Test
    void wrappersOverSameStorageAreEqual() {
        final StubStorage storage = new StubStorage(100);
        final Object first = wrap(storage);
        final Object second = wrap(storage);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void wrappersOverDifferentStoragesAreNotEqual() {
        final Object first = wrap(new StubStorage(100));
        final Object second = wrap(new StubStorage(100));

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
        assertNotEquals(second, first);
    }

    @Test
    void wrapperDelegatesToUnderlyingStorage() {
        final StubStorage storage = new StubStorage(100);
        storage.stored = 40;
        //? if >=26.1 {
        /*// The transfer API has no canExtract/canReceive and no simulate flag; movement goes
        // through explicit transactions instead (see EnergyStorageView for the inverse mapping).
        final EnergyHandler wrapper = (EnergyHandler) wrap(storage);

        assertNotNull(wrapper);
        assertEquals(40, wrapper.getAmountAsInt());
        assertEquals(100, wrapper.getCapacityAsInt());
        try (Transaction insert = Transaction.openRoot()) {
            assertEquals(20, wrapper.insert(20, insert));
            insert.commit();
        }
        assertEquals(60, wrapper.getAmountAsInt());
        try (Transaction simulate = Transaction.openRoot()) {
            assertEquals(40, wrapper.insert(50, simulate));
        }
        assertEquals(60, wrapper.getAmountAsInt());
        try (Transaction extract = Transaction.openRoot()) {
            assertEquals(0, wrapper.extract(10, extract));
            extract.commit();
        }
        assertEquals(60, wrapper.getAmountAsInt());
        *///?} else {
        final IEnergyStorage wrapper = (IEnergyStorage) wrap(storage);

        assertNotNull(wrapper);
        assertEquals(40, wrapper.getEnergyStored());
        assertEquals(100, wrapper.getMaxEnergyStored());
        assertTrue(wrapper.canReceive());
        assertFalse(wrapper.canExtract());
        assertEquals(20, wrapper.receiveEnergy(20, false));
        assertEquals(60, wrapper.getEnergyStored());
        assertEquals(40, wrapper.receiveEnergy(50, true));
        assertEquals(60, wrapper.getEnergyStored());
        assertEquals(0, wrapper.extractEnergy(10, false));
        assertEquals(60, wrapper.getEnergyStored());
        //?}
    }

    private static Object wrap(final EnergyStorage storage) {
        return NeoForgeCapabilities.toNeoForge(EnergyStorage.class, storage, null);
    }

    // Named Stub* rather than Test*: PMD's TestClassWithoutTestCases would flag a Test* inner class.
    private static final class StubStorage implements EnergyStorage {
        private final int capacity;
        private int stored;

        private StubStorage(final int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            final int received = Math.min(maxReceive, capacity - stored);
            if (!simulate) {
                stored += received;
            }
            return received;
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
