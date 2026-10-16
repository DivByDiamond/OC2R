package li.cil.oc2.common.vm.misc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.memory.MemoryMap;
import li.cil.sedna.device.block.ByteBufferBlockDevice;
import li.cil.sedna.device.virtio.VirtIOBlockDevice;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuiltinRootFilesystemTest {
    @BeforeAll
    static void initializeSerializers() {
        Sedna.initialize();
    }

    @Test
    void virtioAdvertisesWritableDiskAndGuestWritesDoNotLeakToAnotherVm() throws Exception {
        final byte[] image = new byte[128 * 1024];
        image[17] = 42;
        final BuiltinRootFilesystem first = disk(image);
        final BuiltinRootFilesystem second = disk(image);
        final VirtIOBlockDevice device = new VirtIOBlockDevice(mock(MemoryMap.class), first);
        assertEquals(0, device.load(0x10, Sizes.SIZE_32_LOG2) & (1 << 5), "VIRTIO_BLK_F_RO");
        first.getOutputStream(17).write(123);
        assertEquals(123, first.getInputStream(17).read());
        assertEquals(42, second.getInputStream(17).read());
        assertEquals(42, image[17]);
    }

    @Test
    void changedBlocksSurviveVmSnapshotIntoFreshBackingImage() throws Exception {
        final byte[] image = new byte[192 * 1024];
        Arrays.fill(image, (byte) 7);
        final Snapshot original = new Snapshot(disk(image));
        original.rootFilesystem.getOutputStream(65530).write(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9});
        final CompoundTag snapshot = NBTSerialization.serialize(original);
        final Snapshot restored = new Snapshot(disk(image));
        final BuiltinRootFilesystem attachedBacking = restored.rootFilesystem;
        NBTSerialization.deserialize(snapshot, restored);
        assertSame(attachedBacking, restored.rootFilesystem, "VirtIO backing must stay attached");
        assertArrayEquals(original.rootFilesystem.getInputStream().readAllBytes(),
                restored.rootFilesystem.getInputStream().readAllBytes());
        final CompoundTag root = (CompoundTag) snapshot.get("rootFilesystem");
        final CompoundTag overlay = (CompoundTag) root.get("overlay");
        final CompoundTag blocks = (CompoundTag) overlay.get("blocks");
        final net.minecraft.nbt.ListTag pages = (net.minecraft.nbt.ListTag) blocks.get("values");
        assertEquals(2, pages.size(), "Only the two changed pages should be persisted");
        assertNull(overlay.get("lower"), "The immutable packaged image is not part of the VM snapshot");
    }

    @Test
    void bulkReadFromUnchangedPageSeesWritesInFollowingShadowPage() throws Exception {
        final byte[] image = new byte[192 * 1024];
        final BuiltinRootFilesystem disk = disk(image);
        disk.getOutputStream(65536 + 13).write(99);
        final byte[] result = new byte[image.length];
        try (final var input = disk.getInputStream()) {
            int position = 0;
            while (position < result.length) {
                final int count = input.read(result, position, result.length - position);
                assertTrue(count > 0, "Bulk read must make progress until capacity");
                position += count;
            }
            assertEquals(-1, input.read());
        }
        assertEquals(99, result[65536 + 13]);
        assertEquals(0, result[65535]);
        assertEquals(0, result[131072]);
    }

    @Test
    void firmwareSwitchRetainsBothRootsAndDoesNotSignalChangeOnRepeatedMount() throws Exception {
        final BuiltinDevices devices = mock(BuiltinDevices.class, CALLS_REAL_METHODS);
        devices.rfs = mock(VirtIOBlockDevice.class);
        devices.rootFilesystem = disk(new byte[128 * 1024]);
        devices.onyxRootFilesystem = disk(new byte[128 * 1024]);
        devices.rootFilesystem.getOutputStream(12).write(41);
        devices.onyxRootFilesystem.getOutputStream(12).write(82);
        devices.selectRootFilesystem(false);
        devices.selectRootFilesystem(true);
        devices.selectRootFilesystem(true);
        devices.selectRootFilesystem(false);
        verify(devices.rfs, times(2)).setBlock(devices.rootFilesystem);
        verify(devices.rfs, times(1)).setBlock(devices.onyxRootFilesystem);
        assertEquals(41, devices.rootFilesystem.getInputStream(12).read());
        assertEquals(82, devices.onyxRootFilesystem.getInputStream(12).read());
    }

    @Test
    void sharedPackagedImageKeepsIndependentPersistentOsOverlays() throws Exception {
        final byte[] bytes = new byte[128 * 1024];
        bytes[23] = 17;
        final var base = ByteBufferBlockDevice.createFromStream(new ByteArrayInputStream(bytes), true);
        final Snapshot onyx = new Snapshot(new BuiltinRootFilesystem(base));
        final Snapshot otherComputer = new Snapshot(new BuiltinRootFilesystem(base));
        onyx.rootFilesystem.getOutputStream(23).write(91);
        final CompoundTag saved = NBTSerialization.serialize(onyx);
        final Snapshot restored = new Snapshot(new BuiltinRootFilesystem(base));
        NBTSerialization.deserialize(saved, restored);
        assertEquals(91, restored.rootFilesystem.getInputStream(23).read());
        assertEquals(17, otherComputer.rootFilesystem.getInputStream(23).read());
        assertEquals(17, base.getInputStream(23).read());
    }

    @Test
    void legacySnapshotWithoutOverlayLeavesFreshDiskAttached() throws Exception {
        final Snapshot restored = new Snapshot(disk(new byte[512]));
        final BuiltinRootFilesystem attachedBacking = restored.rootFilesystem;
        NBTSerialization.deserialize(new CompoundTag(), restored);
        assertSame(attachedBacking, restored.rootFilesystem);
        assertFalse(restored.rootFilesystem.isReadonly());
    }

    private static BuiltinRootFilesystem disk(final byte[] bytes) throws Exception {
        return new BuiltinRootFilesystem(new ByteArrayInputStream(bytes));
    }

    public static final class Snapshot {
        @Serialized public BuiltinRootFilesystem rootFilesystem;

        Snapshot(final BuiltinRootFilesystem rootFilesystem) {
            this.rootFilesystem = rootFilesystem;
        }
    }
}
