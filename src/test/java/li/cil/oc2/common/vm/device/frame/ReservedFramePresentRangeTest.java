package li.cil.oc2.common.vm.device.frame;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalLong;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.vm.block.KeyboardDevice;
import li.cil.oc2.common.vm.context.global.memory.GlobalMemoryRangeAllocator;
import li.cil.oc2.common.vm.context.managed.memory.ManagedMemoryRangeAllocator;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import li.cil.oc2.common.vm.handler.AbstractVMItemStackHandlers;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.memory.MemoryRange;
import li.cil.sedna.device.memory.ByteBufferMemory;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

class ReservedFramePresentRangeTest {
    @Test
    void realExternalDeviceBaseProviderPreservesFutureAdjacentSavedFramebufferRange() {
        final AbstractVMItemStackHandlers handlers = new AbstractVMItemStackHandlers(() -> null) {
            @Override
            protected ItemDeviceQuery makeQuery(final ItemStack stack) {
                throw new UnsupportedOperationException();
            }
            @Override
            protected boolean isClientSide() { return false; }
        };
        final OptionalLong externalBase = handlers.getDeviceAddressBase(new KeyboardDevice<>(new Object()));
        assertEquals(OptionalLong.of(0x30000000L), externalBase);
        verifyReservation(externalBase, 0x30000004L);
    }

    @Test
    void absentBaseProviderAlsoPreservesFutureSavedRange() {
        verifyReservation(OptionalLong.empty(), 0x10000000L);
    }

    @Test
    void zeroStartPreservesSednaPhysicalMemoryAndMmioAllocationClasses() throws Exception {
        Sedna.initialize();
        final R5Board board = new R5Board();
        final GlobalMemoryRangeAllocator global = new GlobalMemoryRangeAllocator(board, new ArrayList<>());
        final ManagedMemoryRangeAllocator managed = new ManagedMemoryRangeAllocator(global, global, OptionalLong::empty);
        try (ByteBufferMemory ram = new ByteBufferMemory(4096)) {
            final long physical = managed.claimMemoryRange(ram).orElseThrow();
            assertEquals(0x80000000L, physical);
            final long mmio = managed.claimMemoryRange(control()).orElseThrow();
            assertTrue(mmio >= 0x10000000L && mmio < 0x80000000L);
            managed.invalidate();
        }
    }

    private static void verifyReservation(final OptionalLong base, final long oldAddress) {
        Sedna.initialize();
        final R5Board board = new R5Board();
        final ArrayList<MemoryRange> reserved = new ArrayList<>();
        reserved.add(MemoryRange.at(oldAddress, 4096));
        final GlobalMemoryRangeAllocator global = new GlobalMemoryRangeAllocator(board, reserved);
        final ManagedMemoryRangeAllocator managed = new ManagedMemoryRangeAllocator(global, global, () -> base);
        final SimpleFramebufferDevice first = pixels();
        assertTrue(managed.claimMemoryRange(0x30000000L, first));
        final FramePresentDevice companion = new FramePresentDevice(first);
        final long newAddress = managed.claimMemoryRange(companion).orElseThrow();
        assertFalse(MemoryRange.at(newAddress, companion.getLength()).intersects(reserved.getFirst()));
        final SimpleFramebufferDevice restored = pixels();
        assertTrue(managed.claimMemoryRange(oldAddress, restored));
        assertEquals(oldAddress, board.getMemoryMap().getMemoryRange(restored).orElseThrow().address());
        managed.invalidate();
    }

    private static FramePresentDevice control() { return new FramePresentDevice(pixels()); }
    private static SimpleFramebufferDevice pixels() {
        return new SimpleFramebufferDevice(2, 1, ByteBuffer.allocate(4));
    }
}
