package li.cil.oc2.common.tablet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** One compressed NBT file per tablet, keyed by the id stored in the item. */
public final class TabletStore {
    private final Path directory;

    public TabletStore(final Path directory) {
        this.directory = directory;
    }

    private Path fileOf(final UUID id) {
        return directory.resolve(id + ".dat");
    }

    /** Reads the saved state of a tablet; empty if it was never saved. */
    public Optional<CompoundTag> load(final UUID id) throws IOException {
        final Path file = fileOf(id);
        if (!Files.isRegularFile(file)) return Optional.empty();
        return Optional.of(NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()));
    }

    /** Replaces the saved state; the old file stays intact if writing fails half way. */
    public void save(final UUID id, final CompoundTag tag) throws IOException {
        Files.createDirectories(directory);
        final Path target = fileOf(id);
        final Path temp = directory.resolve(id + ".dat.tmp");
        NbtIo.writeCompressed(tag, temp);
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /** Removes the saved state, if any. */
    public void delete(final UUID id) throws IOException {
        Files.deleteIfExists(fileOf(id));
    }

    public boolean exists(final UUID id) {
        return Files.isRegularFile(fileOf(id));
    }
}
