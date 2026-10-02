package li.cil.oc2.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.neoforged.neoforge.energy.IEnergyStorage;
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
        final IEnergyStorage first = wrap(storage);
        final IEnergyStorage second = wrap(storage);

        assertNotNull(first);
        assertNotNull(second);
        assertNotSame(first, second);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void wrappersOverDifferentStoragesAreNotEqual() {
        final IEnergyStorage first = wrap(new StubStorage(100));
        final IEnergyStorage second = wrap(new StubStorage(100));

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first, second);
        assertNotEquals(second, first);
    }

    @Test
    void wrapperDelegatesToUnderlyingStorage() {
        final StubStorage storage = new StubStorage(100);
        storage.stored = 40;
        final IEnergyStorage wrapper = wrap(storage);

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
    }

    private static IEnergyStorage wrap(final EnergyStorage storage) {
        return (IEnergyStorage) NeoForgeCapabilities.toNeoForge(EnergyStorage.class, storage, null);
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
