package li.cil.oc2.common.vm.terminal.vttest;

import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Properties;

/** Captured upstream streams are immutable inputs; goldens must be authored by the independent oracle. */
final class VttestProvenance {
    private VttestProvenance() {
    }

    static boolean verify(final VttestFixtures.Fixture fixture, final byte[] stream) throws IOException {
        final Properties metadata = new Properties();
        try (var reader = Files.newBufferedReader(fixture.dir().resolve("page.properties"))) {
            metadata.load(reader);
        }
        if (!metadata.containsKey("source.revision")) return false;
        final String revision = metadata.getProperty("source.revision");
        if (!revision.matches("[0-9a-f]{40}") || !metadata.containsKey("source.page")
                || !metadata.containsKey("oracle")) {
            throw new IOException("incomplete capture provenance: " + fixture.id());
        }
        try {
            final String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(stream));
            if (!actual.equals(metadata.getProperty("stream.sha256"))) {
                throw new IOException("capture hash mismatch: " + fixture.id());
            }
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
        return true;
    }
}
