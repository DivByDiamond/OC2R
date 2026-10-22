package li.cil.oc2.common.vm.misc;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import li.cil.oc2.common.serialization.nbt.util.NBTSerialization;
import li.cil.sedna.Sedna;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionedRootImagesTest {
    @TempDir Path cache;

    @BeforeAll
    static void initializeSerializers() {
        Sedna.initialize();
    }

    @Test
    void unversionedWorldsUseLegacyWhileFreshMachinesUseUpdatedBase() throws Exception {
        final var images = images(2);
        assertEquals(1, images.resolve("").device().getInputStream(0).read());
        assertEquals(2, images.current().device().getInputStream(0).read());
        assertNotEquals(images.current().id(), images.resolve("").id());
    }

    @Test
    void savedPagesAreRestoredAgainstTheirOriginalBaseAfterAnImageUpgrade() throws Exception {
        final var initial = images(2);
        final var base = initial.current();
        final var original = new BuiltinRootFilesystemTest.Snapshot(new BuiltinRootFilesystem(base.device(), base.id()));
        original.rootFilesystem.getOutputStream(65540).write(91);
        final CompoundTag saved = NBTSerialization.serialize(original);
        final var nextRelease = images(3);
        final var pinned = nextRelease.resolve(base.id());
        final var restored = new BuiltinRootFilesystemTest.Snapshot(new BuiltinRootFilesystem(pinned.device(), pinned.id()));
        NBTSerialization.deserialize(saved, restored);
        assertEquals(base.id(), restored.rootFilesystem.getBaseImageId());
        assertEquals(2, restored.rootFilesystem.getInputStream(0).read(), "unchanged pages use the saved base");
        assertEquals(91, restored.rootFilesystem.getInputStream(65540).read());
        assertEquals(3, nextRelease.current().device().getInputStream(0).read());
    }

    @Test
    void legacySnapshotWithoutIdentityKeepsTheSelectedLegacyBaseWhenResaved() throws Exception {
        final var images = images(2);
        final var legacy = images.resolve("");
        final var original = new BuiltinRootFilesystemTest.Snapshot(new BuiltinRootFilesystem(legacy.device()));
        original.rootFilesystem.getOutputStream(65540).write(91);
        final CompoundTag saved = NBTSerialization.serialize(original);
        final CompoundTag root = (CompoundTag) saved.get("rootFilesystem");
        root.remove("baseImageId");
        final var restored = new BuiltinRootFilesystemTest.Snapshot(new BuiltinRootFilesystem(legacy.device(), legacy.id()));
        NBTSerialization.deserialize(saved, restored);
        assertEquals(legacy.id(), restored.rootFilesystem.getBaseImageId());
        assertEquals(1, restored.rootFilesystem.getInputStream(0).read());
        assertEquals(91, restored.rootFilesystem.getInputStream(65540).read());
    }

    @Test
    void missingCorruptOrUntrustedBaseIdentityNeverFallsBackToAnotherImage() throws Exception {
        final var initial = images(2);
        final String id = initial.current().id();
        final var upgraded = images(3);
        Files.write(cache.resolve(id + ".img"), new byte[] {99});
        assertThrows(VersionedRootImages.MissingImageException.class, () -> upgraded.resolve(id));
        Files.delete(cache.resolve(id + ".img"));
        assertThrows(VersionedRootImages.MissingImageException.class, () -> upgraded.resolve(id));
        assertThrows(VersionedRootImages.MissingImageException.class, () -> upgraded.resolve("../../other"));
    }

    @Test
    void unversionedCustomImagesRetainTheirExistingOverride() throws Exception {
        final var custom = new VersionedRootImages(bytes(4), bytes(1), cache, true);
        assertEquals(custom.current().id(), custom.resolve("").id());
    }

    @Test
    void unavailableCacheBlocksNewSparseRootsButKeepsExistingLegacyAndStandaloneDisksReadable() throws Exception {
        final Path blocked = cache.resolve("not-a-directory");
        Files.write(blocked, new byte[] {7});
        final var images = new VersionedRootImages(bytes(2), bytes(1), blocked, false);
        assertEquals(1, images.resolve("").device().getInputStream(0).read());
        assertEquals(2, images.standaloneDisk().device().getInputStream(0).read());
        assertThrows(java.io.IOException.class, images::current);
    }

    @Test
    void retainedLegacyResourceIsByteIdenticalToTheReleasedBase() throws Exception {
        try (var input = getClass().getResourceAsStream("/onyxos/onyxfs-20261016.img")) {
            assertNotNull(input);
            final String hash = java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
            assertEquals("bcbc1f870be9b14967eec802e6021c1c6e934ac1748d893430c7d62261fab31b", hash);
        }
    }

    private VersionedRootImages images(final int value) throws Exception {
        return new VersionedRootImages(bytes(value), bytes(1), cache, false);
    }

    private static byte[] bytes(final int value) {
        final byte[] bytes = new byte[2 * 65536];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }
}
