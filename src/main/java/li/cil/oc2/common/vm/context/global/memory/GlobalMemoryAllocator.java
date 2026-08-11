package li.cil.oc2.common.vm.context.global.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import li.cil.oc2.api.bus.device.vm.context.MemoryAllocator;
import li.cil.oc2.common.vm.memory.Allocator;

public final class GlobalMemoryAllocator implements MemoryAllocator {
    private final List<UUID> claimedMemory = new ArrayList<>();

    public void invalidate() {
        for (final UUID handle : claimedMemory) {
            Allocator.freeMemory(handle);
        }

        claimedMemory.clear();
    }

    @Override
    public boolean claimMemory(final int size) {
        final UUID handle = Allocator.createHandle();
        if (!Allocator.claimMemory(handle, size)) {
            return false;
        }

        claimedMemory.add(handle);
        return true;
    }
}