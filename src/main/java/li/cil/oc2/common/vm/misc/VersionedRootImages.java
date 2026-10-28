package li.cil.oc2.common.vm.misc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import li.cil.sedna.api.device.BlockDevice;
import li.cil.sedna.device.block.ByteBufferBlockDevice;

/** Content-addressed immutable images backing saved sparse root disks. */
public final class VersionedRootImages {
    public record Image(BlockDevice device, String id) {}

    private final Image latestImage;
    private final Image legacy;
    private final Map<String, Image> retained = new ConcurrentHashMap<>();
    private final Path cache;
    private final boolean legacyUsesCurrent;
    private final IOException pinFailure;

    public VersionedRootImages(final byte[] currentBytes, final byte[] legacyBytes,
            final Path cache, final boolean legacyUsesCurrent, final byte[]... retainedBytes) throws IOException {
        latestImage = image(currentBytes);
        legacy = image(legacyBytes);
        for (final byte[] bytes : retainedBytes) {
            final Image previous = image(bytes);
            retained.put(previous.id(), previous);
        }
        this.cache = cache;
        this.legacyUsesCurrent = legacyUsesCurrent;
        // Pin each new base once. Future mod/override updates must not reinterpret
        // saved sparse pages using a different filesystem layout.
        IOException failure = null;
        try {
            Files.createDirectories(cache);
            final Path pinned = cache.resolve(latestImage.id() + ".img");
            if (!Files.exists(pinned)) {
                final Path temporary = Files.createTempFile(cache, "root-", ".tmp");
                try {
                    Files.write(temporary, currentBytes, StandardOpenOption.TRUNCATE_EXISTING);
                    try {
                        Files.move(temporary, pinned, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                    } catch (final java.nio.file.AtomicMoveNotSupportedException ignored) {
                        Files.move(temporary, pinned);
                    }
                } finally {
                    Files.deleteIfExists(temporary);
                }
            }
            if (!latestImage.id().equals(fingerprint(Files.readAllBytes(pinned)))) {
                failure = new IOException("Pinned OnyxOS root image does not match " + latestImage.id());
            }
        } catch (final IOException e) {
            failure = e;
        }
        pinFailure = failure;
    }

    public Image current() throws IOException {
        if (pinFailure != null) {
            throw new IOException("Cannot preserve the new OnyxOS root base in " + cache, pinFailure);
        }
        return latestImage;
    }

    /** Standalone hard drives copy their complete image and do not need a sparse base pin. */
    public Image standaloneDisk() {
        return latestImage;
    }

    public Image resolve(final String id) throws IOException {
        if (id == null || id.isEmpty()) return legacyUsesCurrent ? current() : legacy;
        if (id.equals(latestImage.id())) return latestImage;
        if (id.equals(legacy.id())) return legacy;
        if (retained.containsKey(id)) return retained.get(id);
        if (!id.matches("[0-9a-f]{64}")) throw new MissingImageException(id);
        final Path pinned = cache.resolve(id + ".img");
        if (!Files.isRegularFile(pinned)) throw new MissingImageException(id);
        final Image image = image(Files.readAllBytes(pinned));
        if (!id.equals(image.id())) throw new MissingImageException(id);
        return image;
    }

    private static Image image(final byte[] bytes) throws IOException {
        return new Image(ByteBufferBlockDevice.createFromStream(new ByteArrayInputStream(bytes), true),
                fingerprint(bytes));
    }

    private static String fingerprint(final byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public static final class MissingImageException extends IOException {
        public MissingImageException(final String id) {
            super("OnyxOS root base " + id + " is missing or changed; restore its root-images cache file");
        }
    }
}
