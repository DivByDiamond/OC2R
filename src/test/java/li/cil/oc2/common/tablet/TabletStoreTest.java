package li.cil.oc2.common.tablet;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TabletStoreTest {
    @TempDir Path directory;

    private static CompoundTag tagWith(final int value) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt("value", value);
        tag.putByteArray("ram", new byte[] {1, 2, 3});
        return tag;
    }

    @Test
    void anUnsavedTabletHasNoState() throws IOException {
        final TabletStore store = new TabletStore(directory.resolve("tablets"));
        assertTrue(store.load(UUID.randomUUID()).isEmpty());
        assertFalse(store.exists(UUID.randomUUID()));
    }

    @Test
    void savedStateRoundTripsAndCreatesTheDirectory() throws IOException {
        final TabletStore store = new TabletStore(directory.resolve("tablets"));
        final UUID id = UUID.randomUUID();
        store.save(id, tagWith(7));
        assertTrue(store.exists(id));
        final CompoundTag loaded = store.load(id).orElseThrow();
        assertEquals(tagWith(7), loaded);
    }

    @Test
    void savingAgainReplacesTheStateAndLeavesNoTemporaryFile() throws IOException {
        final Path folder = directory.resolve("tablets");
        final TabletStore store = new TabletStore(folder);
        final UUID id = UUID.randomUUID();
        store.save(id, tagWith(1));
        store.save(id, tagWith(2));
        assertEquals(tagWith(2), store.load(id).orElseThrow());
        try (var files = Files.list(folder)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void tabletsAreKeptApart() throws IOException {
        final TabletStore store = new TabletStore(directory);
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        store.save(first, tagWith(1));
        store.save(second, tagWith(2));
        assertEquals(tagWith(1), store.load(first).orElseThrow());
        assertEquals(tagWith(2), store.load(second).orElseThrow());
    }

    @Test
    void deleteRemovesOnlyThatTablet() throws IOException {
        final TabletStore store = new TabletStore(directory);
        final UUID first = UUID.randomUUID();
        final UUID second = UUID.randomUUID();
        store.save(first, tagWith(1));
        store.save(second, tagWith(2));
        store.delete(first);
        assertFalse(store.exists(first));
        assertTrue(store.exists(second));
        store.delete(first);
    }

    @Test
    void aCorruptFileIsReportedAndNotTreatedAsEmpty() throws IOException {
        final TabletStore store = new TabletStore(directory);
        final UUID id = UUID.randomUUID();
        Files.writeString(directory.resolve(id + ".dat"), "not nbt");
        assertThrows(IOException.class, () -> store.load(id));
    }
}
